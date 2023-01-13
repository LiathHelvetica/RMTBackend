package auth

import pdi.jwt.Jwt
import pdi.jwt.JwtSession
import pdi.jwt.algorithms.JwtHmacAlgorithm
import play.api.Configuration
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.UNAUTHORIZED
import util.control.HttpException
import util.conversion.TryConvertible.OptionTryConvertible

import scala.util.Try

object JwtUtils {

  def sanitiseJwtToken(header: String)(implicit conf: Configuration): String =
    if (header.startsWith(JwtSession.TOKEN_PREFIX)) {
      header.substring(JwtSession.TOKEN_PREFIX.length()).trim
    } else {
      header.trim
    }

  def validateToken(token: String)(implicit conf: Configuration): Try[Unit] = {
    Try {
      Jwt.validate(
        token,
        conf.get[String]("play.http.secret.key"),
        Seq(JwtSession.ALGORITHM.asInstanceOf[JwtHmacAlgorithm])
      )
    }.recover {
      case t: Throwable => throw HttpException("JWT Token authentication failure", UNAUTHORIZED, t)
    }
  }

  def getIdFromToken(session: JwtSession)(implicit conf: Configuration): Try[String] = {

    val subKey = conf.get[String]("rmt.key.map.jwt.sub")

    session.get(conf.get[String]("rmt.key.map.jwt.sub")).flatMap(jsV => jsV.asOpt[String])
      .toTry(HttpException(s"JWT Token does not contain user id under $subKey or is not a string", BAD_REQUEST))
  }
}
