package middleware

import auth.JwtUtils.sanitiseJwtToken
import auth.JwtUtils.validateToken
import pdi.jwt.JwtSession
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Request
import play.api.mvc.Result

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.Configuration
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.ActionBuilderImpl
import play.api.mvc.BodyParsers
import util.control.HttpException
import util.conversion.TryConvertible.OptionTryConvertible

import java.time.Clock
import javax.inject.Inject
import scala.util.Failure
import scala.util.Success

case class Authentication[T](action: Action[T])(implicit conf: Configuration) extends Action[T] {

  implicit val clock: Clock = Clock.systemUTC

  def apply(request: Request[T]): Future[Result] = {
    val authHeaderName = JwtSession.REQUEST_HEADER_NAME
    val outcome = request.headers.get(authHeaderName)
      .toTry(HttpException(s"Method requires authentication header $authHeaderName", BAD_REQUEST))
      .map(sanitiseJwtToken(_))
      .flatMap(validateToken(_))

    outcome match {
      case Failure(cause: HttpException) => Future { cause.toHttpResult }(executionContext)
      case Failure(unknownCause) => Future { HttpException(unknownCause.getMessage).toHttpResult }(executionContext)
      case Success(_) => action(request)
    }
  }

  override def parser: BodyParser[T] = action.parser
  override def executionContext: ExecutionContext = action.executionContext
}

class AuthAction @Inject() (
  parser: BodyParsers.Default
)( implicit
  ec: ExecutionContext,
  conf: Configuration
) extends ActionBuilderImpl(parser) {

  override def invokeBlock[T](request: Request[T], block: Request[T] => Future[Result]): Future[Result] = {
    block(request)
  }

  override def composeAction[T](action: Action[T]) = new Authentication[T](action)
}
