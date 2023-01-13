package dto.user

import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

import scala.util.Try

case class UserDTO(
  id: Option[String],
  userName: Option[String],
  email: Option[String]
) {

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.userName") -> JsString(userName.get),
        conf.get[String]("rmt.key.map.email") -> JsString(email.get)
      ))
    }
  }
}

object UserDTO {

  def apply(id: String, userName: String, email: String): UserDTO = {
    new UserDTO(Some(id), Some(userName), Some(email))
  }
}
