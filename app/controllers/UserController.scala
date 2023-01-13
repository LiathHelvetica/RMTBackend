package controllers

import auth.JwtUtils.getIdFromToken
import dto.user.CreateUserDTO
import dto.user.UserDTO
import middleware.AuthAction
import models.TestModel.testTable
import models.User.users
import org.apache.commons.codec.binary.Base64.decodeBase64
import org.apache.commons.codec.binary.Base64.encodeBase64String
import pdi.jwt.JwtSession.RichResult
import pdi.jwt.JwtSession.RichRequestHeader
import play.api.Configuration
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import slick.jdbc.JdbcProfile
import util.Hasher.hash
import util.IdGenerator.generateId
import util.SaltGenerator.generateSalt
import util.control.HttpException
import util.control.ResponseBody.plainResponse
import util.conversion.FutureConvertible.TryFutureConvertible
import util.conversion.FutureTransformers.HttpRecoverableFuture
import util.conversion.TryConvertible.OptionTryConvertible

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

@Singleton
class UserController @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents,
  authAction: AuthAction
)( implicit
  ec: ExecutionContext,
  conf: Configuration
) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  implicit val clock: Clock = Clock.systemUTC
  implicit val logger: Logger = Logger(this.getClass)

  def test: Action[AnyContent] = Action.async { implicit request =>
    val c = DBIO.seq(
    sqlu"insert into test values ('aaa', 12, true)",
      sqlu"insert into adddasad values (12)",
      sqlu"insert into test values ('qqq', 12, true)",
    )

    db.run(c).map(s => Ok)
  }

  def createUser: Action[JsValue] = Action(parse.json).async { implicit request =>
    CreateUserDTO(request.body)
      .flatMap(dto => dto.validate)
      .map(dto => {
        val salt = generateSalt
        dto.copy(
          id = Some(generateId),
          password = Some(encodeBase64String(hash(dto.password.get, salt))),
          salt = Some(encodeBase64String(salt))
        )
      })
      .toFuture
      .flatMap(dto => {
        val entity = dto.toEntity
        db.run(users += entity.get).map(_ => dto.toJsObject)
          .recoverWith {
            case cause => Future.failed(HttpException("Error on database insertion", INTERNAL_SERVER_ERROR, cause))
          }
      })
      .map(tryJs => Created(tryJs.get))
      .recoverHttp
  }

  def login: Action[JsValue] = Action(parse.json).async { implicit request =>
    (request.body \ conf.get[String]("rmt.key.map.email"))
      .asOpt[String]
      .toTry(HttpException(s"Field ${conf.get[String]("rmt.key.map.email")} was not present in body", BAD_REQUEST))
      .toFuture
      .flatMap(s => db.run(
          (for {
           u <- users if u.email === s
         } yield (u.id, u.password, u.salt)).result
        ).recoverWith {
          case cause => Future.failed(HttpException("Error on user data recovery", INTERNAL_SERVER_ERROR, cause))
        }.map(userDataSeq => (userDataSeq, s))
      )
      .flatMap(userData =>
        userData._1
        .headOption
        .toTry(HttpException(s"No data for user ${userData._2}", BAD_REQUEST))
        .toFuture
      )
      .map(userData => {
        val (id, dbPassword, salt) = userData
        (request.body \ conf.get[String]("rmt.key.map.password"))
          .asOpt[String]
          .toTry(HttpException(s"Field ${conf.get[String]("rmt.key.map.password")} was not present in body", BAD_REQUEST))
          .map(password => encodeBase64String(hash(password, decodeBase64(salt))) == dbPassword)
          .flatMap(result => if (result) {
            Success(
              Ok(plainResponse("Authenticated"))
                .withNewJwtSession
                .addingToJwtSession(conf.get[String]("rmt.key.map.jwt.sub") -> id)
            )
          } else {
            Failure(HttpException("Authentication error", UNAUTHORIZED))
          })
          .get
      })
      .recoverHttp
  }

  def getUserInfo: Action[AnyContent] = authAction.async(parse.anyContent) { implicit request =>
    getIdFromToken(request.jwtSession)
      .toFuture
      .flatMap(id => db.run(
        (for {
          u <- users if u.id === id
        } yield (u.id, u.userName, u.email)).result
      ).recoverWith {
        case cause => Future.failed(HttpException("Error on user data recovery", INTERNAL_SERVER_ERROR, cause))
      }.map(userDataSeq => (userDataSeq, id))
      )
      .flatMap(userData => userData._1
        .headOption
        .map(data => UserDTO(data._1, data._2, data._3))
        .toTry(HttpException(s"No data for user ${userData._2}", BAD_REQUEST))
        .toFuture
      )
      .map(_.toJsObject match {
        case Success(jsObj) => Ok(jsObj)
        case Failure(t) => HttpException("Failed to transform result to user object", INTERNAL_SERVER_ERROR, t).toHttpResult
      })
      .recoverHttp
  }
}
