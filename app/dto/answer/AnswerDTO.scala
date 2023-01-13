package dto.answer

import dto.riddle.RiddleType.RiddleType
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

import scala.util.Try

case class AnswerDTO(
  id: Option[String],
  answer: Option[String],
  isCorrect: Option[Boolean]
) {

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.answer") -> JsString(answer.get),
        conf.get[String]("rmt.key.map.isCorrect") -> JsBoolean(isCorrect.get),
      ))
    }
  }
}

object AnswerDTO {

  def apply(data: (String, String, Option[String], DateTime, DateTime, Boolean, RiddleType, String, String, Boolean)): AnswerDTO = {
    new AnswerDTO(
      Some(data._8),
      Some(data._9),
      Some(data._10)
    )
  }
}
