package util

import play.api.Configuration

import scala.util.Random

object SaltGenerator {
  val RNG = new Random()
  val defaultSaltLength = 256

  def generateSalt(implicit conf: Configuration): Array[Byte] = {

    val stringPartPrecision: Int = conf.getOptional[Int]("rmt.security.salt.length").getOrElse(defaultSaltLength)

    RNG.nextString(stringPartPrecision).getBytes(conf.get[String]("rmt.encoding"))
  }
}
