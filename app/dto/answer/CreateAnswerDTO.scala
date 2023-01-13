package dto.answer

import play.api.Configuration
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue

import scala.util.Try

case class CreateAnswerDTO(
  id: Option[String],
  riddleId: Option[String],
  answer: Option[String],
  isCorrect: Option[Boolean],
  userId: Option[String]
) {

  def toEntity: Try[(String, String, String, Boolean, String)] = {
    Try {
      (id.get, riddleId.get, answer.get, isCorrect.get, userId.get)
    }
  }

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.riddleId") -> JsString(riddleId.get),
        conf.get[String]("rmt.key.map.answer") -> JsString(answer.get),
        conf.get[String]("rmt.key.map.isCorrect") -> JsBoolean(isCorrect.get),
        conf.get[String]("rmt.key.map.userId") -> JsString(userId.get)
      ))
    }
  }
}

object CreateAnswerDTO {
  def apply(body: JsValue, userId: Option[String], riddleId: Option[String])(implicit conf: Configuration): Try[CreateAnswerDTO] = {
    Try {
      new CreateAnswerDTO(
        id = None,
        riddleId = riddleId,
        answer = Some((body \ conf.get[String]("rmt.key.map.answer")).as[String]),
        isCorrect = Some((body \ conf.get[String]("rmt.key.map.isCorrect")).asOpt[Boolean].getOrElse(true)),
        userId = userId
      )
    }
  }
}
