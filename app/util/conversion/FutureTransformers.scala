package util.conversion

import play.api.Logger
import play.api.mvc.Result
import util.control.HttpException

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object FutureTransformers {

  implicit class HttpRecoverableFuture(val f: Future[Result]) extends AnyVal {

    def recoverHttp(implicit logger: Logger, ec: ExecutionContext): Future[Result] = {
      f.recoverWith {
        case cause => {
          logger.error("HTTP method failure", cause)
          Future.failed(cause)
        }
      }
      .recover {
        case cause: HttpException => cause.toHttpResult
        case unknownCause => HttpException(unknownCause.getMessage).toHttpResult
      }
    }
  }
}
