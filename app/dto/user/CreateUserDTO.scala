package dto.user

import play.api.Configuration
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import util.control.HttpException

import scala.util.Failure
import scala.util.Success
import scala.util.Try

case class CreateUserDTO(
  id: Option[String],
  userName: Option[String],
  email: Option[String],
  password: Option[String],
  salt: Option[String]
) {
  def validate: Try[CreateUserDTO] = {
    Success(this)
  }

  def toEntity: Try[(String, String, String, String, String)] = {
    Try {(
      id.get, userName.get, email.get, password.get, salt.get
    )}
  }

  def toJsObject(implicit conf: Configuration): Try[JsObject] = {
    Try {
      JsObject(Seq(
        conf.get[String]("rmt.key.map.id") -> JsString(id.get),
        conf.get[String]("rmt.key.map.userName") -> JsString(userName.get),
        conf.get[String]("rmt.key.map.email") -> JsString(email.get),
        conf.get[String]("rmt.key.map.password") -> JsString(conf.get[String]("rmt.security.redacted.string")),
        conf.get[String]("rmt.key.map.salt") -> JsString(conf.get[String]("rmt.security.redacted.string"))
      ))
    }
  }
}

object CreateUserDTO {

  def apply(body: JsValue)(implicit conf: Configuration): Try[CreateUserDTO] = {
    Try {
      new CreateUserDTO(
        id = None,
        userName = Some((body \ conf.get[String]("rmt.key.map.userName")).as[String]),
        email = Some((body \ conf.get[String]("rmt.key.map.email")).as[String]),
        password = Some((body \ conf.get[String]("rmt.key.map.password")).as[String]),
        salt = None
      )
    }.recoverWith(t =>
      Failure(HttpException(s"Failed to transfer to UserDTO. Body: $body", BAD_REQUEST, Some(t)))
    )
  }
}
