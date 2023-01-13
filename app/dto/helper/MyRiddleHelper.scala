package dto.helper

import dto.riddle.RiddleType
import models.Answer.answers
import models.Riddle.riddles
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.http.Status.INTERNAL_SERVER_ERROR
import slick.jdbc.JdbcProfile
import util.control.HttpException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.github.tototoshi.slick.PostgresJodaSupport._

class MyRiddleHelper(val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  def getMyRiddle(userId: String, riddleId: String)(implicit ec: ExecutionContext):
    Future[(String, Seq[(String, String, Option[String], DateTime, DateTime, Boolean, RiddleType.RiddleType, String, String, Boolean)])] = {

    val query = for {
      (a, r) <- answers join riddles on ((a, r) => a.riddleId === r.id) if a.riddleId === riddleId && a.userId === userId
    } yield (r.id, r.title, r.contents, r.creationDate, r.lastUpdateDate, r.isAccepted, r.riddleType, a.id, a.answer, a.isCorrect)
    db.run(query.result)
      .recoverWith {
        case cause => Future.failed(HttpException(s"Error fetching riddle with id $riddleId of user $userId from DB", INTERNAL_SERVER_ERROR, cause))
      }
      .map(data => (userId, data))
  }
}
