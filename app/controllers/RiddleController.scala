package controllers

import auth.JwtUtils.getIdFromToken
import dto.riddle.RiddleDTO
import middleware.AuthAction
import models.Answer.answers
import models.Riddle.riddles
import models.User.users
import org.joda.time.DateTime
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
import util.IdGenerator.generateId
import util.control.HttpException
import util.conversion.FutureConvertible.TryFutureConvertible
import util.conversion.FutureTransformers.HttpRecoverableFuture

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import com.github.tototoshi.slick.PostgresJodaSupport._
import dto.helper.MyRiddleHelper
import dto.helper.UpdateRiddleHelper
import dto.riddle.ForeignRiddleDTO
import dto.riddle.MyRiddleDTO
import dto.riddle.UpdateRiddleDTO
import cats._
import cats.data._
import cats.syntax.all._
import dto.generic.ListDTO
import dto.riddle.RiddleType
import play.api.mvc.Result

import scala.util.Success

@Singleton
class RiddleController @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents,
  authAction: AuthAction
) ( implicit
  ec: ExecutionContext,
  conf: Configuration
) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  implicit val clock: Clock = Clock.systemUTC
  implicit val logger: Logger = Logger(this.getClass)

  val myRiddleHelper: MyRiddleHelper = new MyRiddleHelper(dbConfigProvider)
  val updateRiddleHelper: UpdateRiddleHelper = new UpdateRiddleHelper(dbConfigProvider)

  def createRiddle: Action[JsValue] = authAction.async(parse.json) { implicit request =>
    getIdFromToken(request.jwtSession)
      .map(uId => (uId, generateId))
      .flatMap(data => RiddleDTO(request.body, data._1, data._2)
        .recoverWith {
          case cause => Failure(HttpException("Couldn't convert body to DTO object", BAD_REQUEST, cause))
        }
      )
      .map(riddleDTO => {
        val answers = riddleDTO.answers.map(answerDTO => answerDTO.copy(id = Some(generateId)))
        val now = DateTime.now
        riddleDTO.copy(
          creationDate = Some(now),
          lastUpdateDate = Some(now),
          isAccepted = Some(conf.get[Boolean]("rmt.administration.accept.default")),
          answers = answers
        )
      })
      .toFuture
      .flatMap(riddleDTO => {
        val answerEntities = riddleDTO.answers.map(a => a.toEntity.get)
        val commands = DBIO.seq(
          riddles += riddleDTO.toEntity.get,
          answers ++= answerEntities
        )
        db.run(commands)
          .map(_ => riddleDTO)
          .recoverWith {
            case cause => Future.failed(HttpException("Riddle creation failed on database insertion", INTERNAL_SERVER_ERROR, cause))
          }
      })
      .map(dto => Created(dto.toJsObject.get))
      .recoverHttp
  }

  def deleteRiddle(riddleId: String): Action[AnyContent] = authAction.async(parse.anyContent) { implicit request =>
    getIdFromToken(request.jwtSession)
      .toFuture
      .flatMap(userId => {
        val answerQuery = answers.filter(a => a.userId === userId && a.riddleId === riddleId).delete
        val riddleQuery = riddles.filter(r => r.id === riddleId && r.userId === userId).delete
        val query = DBIO.seq(answerQuery, riddleQuery)
        db.run(query)
          .recoverWith {
            case cause => Future.failed(HttpException("Error on riddle deletion from DB", INTERNAL_SERVER_ERROR, cause))
          }
      })
      .map(_ => Ok)
      .recoverHttp
  }

  def getRiddle(riddleId: String): Action[AnyContent] = Action(parse.anyContent).async { implicit request =>
    val query = for {
      (r, u) <- riddles join users on ((r, u) => r.userId === u.id) if r.id === riddleId
    } yield (r.id, r.title, r.contents, r.lastUpdateDate, r.isAccepted, r.riddleType, u.id, u.userName)
    db.run(query.result)
      .recoverWith {
        case cause => Future.failed(HttpException(s"Error fetching riddle with id $riddleId from DB", INTERNAL_SERVER_ERROR, cause))
      }
      .map(entities => entities.headOption match {
        case Some(entity) => Ok(ForeignRiddleDTO(entity).toJsObject.get)
        case None => HttpException(s"Couldn't find riddle with id $riddleId", NOT_FOUND).toHttpResult
      })
      .recoverHttp
  }

  def getMyRiddle(riddleId: String): Action[AnyContent] = authAction.async(parse.anyContent) { implicit request =>
    getIdFromToken(request.jwtSession)
      .toFuture
      .flatMap(userId => myRiddleHelper.getMyRiddle(userId, riddleId))
      .flatMap(data => {
        MyRiddleDTO(riddleId, data._1, data._2).toFuture
      })
      .map(dto => Ok(dto.toJsObject.get))
      .recoverHttp
  }

  def updateRiddle(riddleId: String): Action[JsValue] = authAction.async(parse.json) { implicit request =>
    getIdFromToken(request.jwtSession)
      .toFuture
      .flatMap(userId => myRiddleHelper.getMyRiddle(userId, riddleId))
      .flatMap(outcome => {
        val (userId, riddles) = outcome
        MyRiddleDTO(riddleId, userId, riddles)
          .toFuture
          .map(myRiddle => (userId, myRiddle, UpdateRiddleDTO(request.body)))
      })
      .flatMap(data => {
        data._3.readyForUpdate(data._2).toFuture
          .recoverWith {
            case cause => Future.failed(HttpException("Error transforming update riddle data", INTERNAL_SERVER_ERROR, cause))
          }
          .map(out => (data._1, out))
      })
      .flatMap(data => updateRiddleHelper.updateRiddle(data._2, data._1, riddleId))
      .map(_ => Ok)
      .recoverHttp
  }

  def getRiddles(
    n: Int,
    page: Int, // from 0
    titleContains: Option[String],
    contentsContains: Option[String],
    isAccepted: Option[Boolean],
    userNameContains: Option[String]
  ): Action[AnyContent] = Action(parse.anyContent).async { implicit request =>
    queryForRiddles(
      n,
      page,
      None,
      titleContains,
      contentsContains,
      isAccepted,
      userNameContains
    )
  }

  private def queryForRiddles(
    n: Int,
    page: Int, // from 0
    userId: Option[String],
    titleContains: Option[String],
    contentsContains: Option[String],
    isAccepted: Option[Boolean],
    userNameContains: Option[String]
  ): Future[Result] = {
    val query = for {
      (r, u) <- riddles.join(users).on((r, u) => r.userId === u.id)
        .filterOpt(userId)((tables, s) => tables._2.id === s)
        .filterOpt(titleContains)((tables, s) => tables._1.title.like(s"%$s%"))
        .filterOpt(contentsContains)((tables, s) => tables._1.contents.like(s"%$s%"))
        .filterOpt(isAccepted)((tables, b) => tables._1.isAccepted === b)
        .filterOpt(userNameContains)((tables, s) => tables._2.userName.like(s"%$s%"))
        .drop(page * n)
        .take(n)
    } yield (r.id, r.title, r.contents, r.lastUpdateDate, r.isAccepted, r.riddleType, u.id, u.userName)
    db.run(query.result)
      .recoverWith {
        case cause => Future.failed(HttpException(s"Error fetching riddles from DB", INTERNAL_SERVER_ERROR, cause))
      }
      .map(entities => {
        entities.map(e => ForeignRiddleDTO(e).toJsObject).sequence.flatMap(objs => ListDTO(objs).toJsObject) match {
          case Success(arr) => Ok(arr)
          case Failure(t) => HttpException(s"Error transforming riddles to JSON array", INTERNAL_SERVER_ERROR, t).toHttpResult
        }
      })
      .recoverHttp
  }

  def getMyRiddles(
    n: Int,
    page: Int, // from 0
    titleContains: Option[String],
    contentsContains: Option[String],
    isAccepted: Option[Boolean],
  ): Action[AnyContent] = authAction.async(parse.anyContent) { implicit request =>
    getIdFromToken(request.jwtSession)
      .toFuture
      .flatMap(userId => queryForRiddles(
        n,
        page,
        Some(userId),
        titleContains,
        contentsContains,
        isAccepted,
        None
      ))
  }

}
