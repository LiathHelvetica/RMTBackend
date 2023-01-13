package controllers

import auth.JwtUtils.getIdFromToken
import dto.solution.SolutionDTO
import middleware.AuthAction
import models.Answer.answers
import models.Solution.solutions
import org.joda.time.DateTime
import pdi.jwt.JwtSession.RichRequestHeader
import play.api.Configuration
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.mvc.AbstractController
import play.api.mvc.Action
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
import scala.util.Try

@Singleton
class SolutionsController @Inject() (
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

  def createSolution(riddleId: String): Action[JsValue] = authAction.async(parse.json) { implicit request =>
    getIdFromToken(request.jwtSession)
      .map(userId => (userId, generateId))
      .flatMap(pair => {
        val (userId, solutionId) = pair
        SolutionDTO(request.body, solutionId, userId, riddleId)
          .recoverWith {
            case cause => Failure(HttpException("Couldn't convert body to DTO object", BAD_REQUEST, cause))
          }
      })
      .toFuture
      .flatMap(solutionDTO => {
        val query = for {
          a <- answers.filter(a => a.riddleId === riddleId).filter(a => a.isCorrect)
        } yield a.answer
        db.run(query.result).map(answers => (answers, solutionDTO))
          .recoverWith {
            case cause => Future.failed(HttpException(s"Error fetching answers for riddle $riddleId from DB", INTERNAL_SERVER_ERROR, cause))
          }
      })
      .flatMap(tuple => {
        val (answers, solutionDTO) = tuple
        Try {
          (answers, solutionDTO, solutionDTO.answer.get)
        }.toFuture
      })
      .map(tuple => {
        val (answers, solutionDTO, answer) = tuple
        solutionDTO.copy(isCorrect = Some(answers.contains(answer)), answerTime = Some(DateTime.now))
      })
      .flatMap(solutionDTO => {
        val entity = solutionDTO.toEntity
        db.run(solutions += entity.get).map(_ => solutionDTO.toJsObject)
          .recoverWith {
            case cause => Future.failed(HttpException("Error on database insertion", INTERNAL_SERVER_ERROR, cause))
          }
      })
      .map(v => Ok(v.get))
      .recoverHttp
  }

}
