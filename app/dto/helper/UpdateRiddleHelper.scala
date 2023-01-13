package dto.helper

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import dto.meta.ActionType.CreateAction
import dto.meta.ActionType.DeleteAction
import dto.meta.ActionType.UpdateAction
import dto.riddle.UpdateRiddleDTO
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try
import models.Answer.answers
import models.Riddle.riddles
import play.api.http.Status.INTERNAL_SERVER_ERROR
import util.IdGenerator.generateId
import util.control.HttpException
import util.conversion.FutureConvertible.TryFutureConvertible

import cats.implicits._
import com.github.tototoshi.slick.PostgresJodaSupport._

class UpdateRiddleHelper(val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  def updateRiddle(riddle: UpdateRiddleDTO, userId: String, riddleId: String)(implicit ec: ExecutionContext, conf: Configuration): Future[Unit] = {
    Try {
      val riddleEntity = riddle.toEntity.get
      val answers = riddle.answers.groupBy(a => a.action.get)

      val createAnswerEntities = answers.getOrElse(CreateAction, Set.empty).map(a => {
        a.toCreateEntity(generateId, userId, riddleId)
      }).toSeq.sequence.get
      val deleteAnswerEntities = answers.getOrElse(DeleteAction, Set.empty).map(a => a.toDeleteEntity).toSeq.sequence.get
      val updateAnswerEntities = answers.getOrElse(UpdateAction, Set.empty).map(a => a.toUpdateEntity).toSeq.sequence.get

      (riddleEntity, createAnswerEntities, deleteAnswerEntities, updateAnswerEntities)
    }
      .toFuture
      .flatMap(data => {
        val (riddleEntity, createAnswerEntities, deleteAnswerEntities, updateAnswerEntities) = data

        val updateRiddleQuery = (for {
          r <- riddles if r.id === riddleId
        } yield (r.title, r.contents, r.lastUpdateDate, r.riddleType))
          .update(riddleEntity)

        val createAnswerQueries = answers ++= createAnswerEntities

        val deleteAnswerQueries = answers.filter(a => a.id.inSet(deleteAnswerEntities)).delete

        val updateAnswerQueries = DBIO.sequence(
          updateAnswerEntities.map(data => {
            (for {
              a <- answers if a.userId === userId && a.riddleId === riddleId && a.id === data._1 // TODO sanity checks
            } yield (a.answer, a.isCorrect))
              .update(data._2, data._3)
          })
        )

        db.run(DBIO.seq(updateRiddleQuery, createAnswerQueries, deleteAnswerQueries, updateAnswerQueries))
      })
      .recoverWith {
        case cause => Future.failed(HttpException(s"Error executing update command for riddle $riddleId of user $userId", INTERNAL_SERVER_ERROR, cause))
      }
  }

}
