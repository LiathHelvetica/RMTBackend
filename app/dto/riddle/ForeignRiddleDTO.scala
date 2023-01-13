package dto.riddle

import dto.riddle.RiddleType.RiddleType
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import util.conversion.ParsableDateTime.FromConfigParsableDateTime

import scala.util.Try

case class ForeignRiddleDTO(
  id: Option[String],
  title: Option[String],
  contents: Option[String],
  lastUpdateDate: Option[DateTime],
  isAccepted: Option[Boolean],
  riddleType: Option[RiddleType],
  creatorId: Option[String],
  creatorName: Option[String]
) {

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.riddleTitle") -> JsString(title.get),
        conf.get[String]("rmt.key.map.lastUpdateDate") -> JsString(lastUpdateDate.get.parse),
        conf.get[String]("rmt.key.map.isAccepted") -> JsBoolean(isAccepted.get),
        conf.get[String]("rmt.key.map.riddleType") -> JsString(riddleType.get.toString),
        conf.get[String]("rmt.key.map.creatorId") -> JsString(creatorId.get),
        conf.get[String]("rmt.key.map.creatorName") -> JsString(creatorName.get)
      )) ++ (contents match {
        case Some(c) => JsObject(Seq(conf.get[String]("rmt.key.map.riddleContents") -> JsString(c)))
        case None => JsObject.empty
      })
    }
  }
}

object ForeignRiddleDTO {

  def apply(data: (String, String, Option[String], DateTime, Boolean, RiddleType, String, String)): ForeignRiddleDTO = {
    new ForeignRiddleDTO(
      Some(data._1),
      Some(data._2),
      data._3,
      Some(data._4),
      Some(data._5),
      Some(data._6),
      Some(data._7),
      Some(data._8)
    )
  }
}
