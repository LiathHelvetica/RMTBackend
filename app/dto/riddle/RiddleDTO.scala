package dto.riddle

import dto.answer.CreateAnswerDTO
import dto.riddle.RiddleType.RiddleType
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.JsArray
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import util.conversion.ParsableDateTime.FromConfigParsableDateTime

import scala.util.Try

case class RiddleDTO(
  id: Option[String],
  userId: Option[String],
  title: Option[String],
  contents: Option[String],
  creationDate: Option[DateTime],
  lastUpdateDate: Option[DateTime],
  isAccepted: Option[Boolean],
  riddleType: Option[RiddleType],

  answers: Set[CreateAnswerDTO]
) {

  def toEntity: Try[(String, String, String, Option[String], DateTime, DateTime, Boolean, RiddleType)] = {
    Try {
      (id.get, userId.get, title.get, contents, creationDate.get, lastUpdateDate.get, isAccepted.get, riddleType.get)
    }
  }

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.userId") -> JsString(userId.get),
        conf.get[String]("rmt.key.map.riddleTitle") -> JsString(title.get),
        conf.get[String]("rmt.key.map.creationDate") -> JsString(creationDate.get.parse),
        conf.get[String]("rmt.key.map.lastUpdateDate") -> JsString(lastUpdateDate.get.parse),
        conf.get[String]("rmt.key.map.isAccepted") -> JsBoolean(isAccepted.get),
        conf.get[String]("rmt.key.map.riddleType") -> JsString(riddleType.get.toString),
        conf.get[String]("rmt.key.map.answers") -> JsArray(answers.map(_.toJsObject.get).toSeq)
      )) ++ (contents match {
        case Some(c) => JsObject(Seq(conf.get[String]("rmt.key.map.riddleContents") -> JsString(c)))
        case None => JsObject.empty
      })
    }
  }
}

object RiddleDTO {

  def apply(body: JsValue, userId: Option[String], riddleId: Some[String])(implicit conf: Configuration): Try[RiddleDTO] = {
    Try {
      new RiddleDTO(
        id = riddleId,
        userId = userId,
        title = Some((body \ conf.get[String]("rmt.key.map.riddleTitle")).as[String]),
        contents = (body \ conf.get[String]("rmt.key.map.riddleContents")).asOpt[String],
        creationDate = None,
        lastUpdateDate = None,
        isAccepted = None,
        riddleType = Some((body \ conf.get[String]("rmt.key.map.riddleType")).as[RiddleType]),

        answers = (body \ conf.get[String]("rmt.key.map.answers")).as[JsArray].value.map(jsV => CreateAnswerDTO(jsV, userId, riddleId).get).toSet
      )
    }
  }

  def apply(body: JsValue, userId: String, riddleId: String)(implicit conf: Configuration): Try[RiddleDTO] = {
    RiddleDTO(body, Some(userId), Some(riddleId))
  }
}
