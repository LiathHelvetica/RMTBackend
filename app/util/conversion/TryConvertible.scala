package util.conversion

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object TryConvertible {

  implicit class OptionTryConvertible[T](val o: Option[T]) extends AnyVal {

    def toTry(otherwise: Throwable): Try[T] = o match {
      case Some(t) => Success(t)
      case None => Failure(otherwise)
    }
  }
}
