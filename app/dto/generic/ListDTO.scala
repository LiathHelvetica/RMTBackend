package dto.generic

import play.api.Configuration
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject

import scala.util.Try

case class ListDTO(data: Seq[JsObject]) {

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.list.data") -> JsArray(data)
      ))
    }
  }
}
