package dto.solution

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import util.conversion.ParsableDateTime.FromConfigParsableDateTime

import scala.util.Try

case class SolutionDTO(
  id: Option[String],
  userId: Option[String],
  riddleId: Option[String],
  answer: Option[String],
  isCorrect: Option[Boolean],
  answerTime: Option[DateTime]
) {

  def toEntity: Try[(String, String, String, String, Boolean, DateTime)] = {
    Try {
      (id.get, userId.get, riddleId.get, answer.get, isCorrect.get, answerTime.get)
    }
  }

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.userId") -> JsString(userId.get),
        conf.get[String]("rmt.key.map.riddleId") -> JsString(riddleId.get),
        conf.get[String]("rmt.key.map.answer") -> JsString(answer.get),
        conf.get[String]("rmt.key.map.isCorrect") -> JsBoolean(isCorrect.get),
        conf.get[String]("rmt.key.map.answerTime") -> JsString(answerTime.get.parse)
      ))
    }
  }
}

object SolutionDTO {

  def apply(body: JsValue, solutionId: Option[String], userId: Option[String], riddleId: Some[String])(implicit conf: Configuration): Try[SolutionDTO] = {
    Try {
      new SolutionDTO(
        solutionId,
        userId,
        riddleId,
        (body \ conf.get[String]("rmt.key.map.answer")).asOpt[String],
        None,
        None
      )
    }
  }

  def apply(body: JsValue, solutionId: String, userId: String, riddleId: String)(implicit conf: Configuration): Try[SolutionDTO] = {
    SolutionDTO(body, Some(solutionId), Some(userId), Some(riddleId))
  }
}
