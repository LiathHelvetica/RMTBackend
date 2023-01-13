package util.control

import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

object ResponseBody {

  def plainResponse(msg: String)(implicit conf: Configuration): JsObject = {
    JsObject(Seq(
      conf.get[String]("rmt.key.map.message") -> JsString(msg)
    ))
  }
}
