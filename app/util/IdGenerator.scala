package util

import org.joda.time.DateTimeUtils.currentTimeMillis
import play.api.Configuration

import scala.util.Random

object IdGenerator {
  val RNG = new Random()
  val defaultStringPartPrecision = 16

  def generateId(implicit conf: Configuration): String = {

    val stringPartPrecision: Int = conf.getOptional[Int]("rmt.security.id.string.part").getOrElse(defaultStringPartPrecision)

    val time: String = currentTimeMillis.toString
    val random: String = RNG.alphanumeric.take(stringPartPrecision).mkString
    random + time
  }
}
