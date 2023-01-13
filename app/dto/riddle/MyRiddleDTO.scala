package dto.riddle

import dto.answer.AnswerDTO
import dto.riddle.RiddleType.RiddleType
import org.joda.time.DateTime
import play.api.Configuration
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.JsArray
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import util.control.HttpException
import util.conversion.ParsableDateTime.FromConfigParsableDateTime
import util.conversion.TryConvertible.OptionTryConvertible

import scala.util.Try

case class MyRiddleDTO (
  id: Option[String],
  title: Option[String],
  contents: Option[String],
  creationDate: Option[DateTime],
  lastUpdateDate: Option[DateTime],
  isAccepted: Option[Boolean],
  riddleType: Option[RiddleType],

  answers: Set[AnswerDTO]
) {

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
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

object MyRiddleDTO {

  def apply(riddleId: String, userId: String, data: Seq[(String, String, Option[String], DateTime, DateTime, Boolean, RiddleType, String, String, Boolean)]): Try[MyRiddleDTO] = {
    data.headOption.toTry(HttpException(s"No riddle with id $riddleId found for user $userId", BAD_REQUEST))
      .map(d => {
        new MyRiddleDTO(
          Some(d._1),
          Some(d._2),
          d._3,
          Some(d._4),
          Some(d._5),
          Some(d._6),
          Some(d._7),
          data.map(d => AnswerDTO(d)).toSet
        )
      })
  }
}
