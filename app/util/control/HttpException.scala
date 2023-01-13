package util.control

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Writeable.writeableOf_JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.mvc.ResponseHeader
import play.api.mvc.Result
import util.control.HttpException.fallbackContentType

case class HttpException(
  message: String,
  header: ResponseHeader,
  body: Option[HttpEntity],
  cause: Option[Throwable]
) extends Exception(message, cause.orNull) {

  def toHttpResult: Result = new Result(
    header,
    body.getOrElse(HttpEntity.Strict(ByteString(message), Some(fallbackContentType)))
  )
}

object HttpException {

  val fallbackContentType: String = "application/json"
  val jsonExceptionMessageKey: String = "message"

  def apply(message: String, status: Int = INTERNAL_SERVER_ERROR, cause: Option[Throwable] = None): HttpException = {
    new HttpException(
      message,
      ResponseHeader(status),
      Some(writeableOf_JsValue.toEntity(
        JsObject(
          Seq(jsonExceptionMessageKey -> JsString(message))
        )
      )),
      cause
    )
  }

  def apply(message: String, status: Int, cause: Throwable): HttpException = HttpException(message, status, Some(cause))
}
