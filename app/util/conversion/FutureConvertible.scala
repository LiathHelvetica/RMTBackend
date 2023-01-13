package util.conversion

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

object FutureConvertible {

  implicit class TryFutureConvertible[T](val t: Try[T]) extends AnyVal {
    def toFuture: Future[T] = Future.fromTry(t)
  }
}
