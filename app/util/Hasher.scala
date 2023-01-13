package util

import org.apache.commons.codec.digest.DigestUtils.sha256
import play.api.Configuration

object Hasher {

  def hash(s: String, salt: Array[Byte])(implicit conf: Configuration): Array[Byte] = {

    val encoding = conf.get[String]("rmt.encoding")
    val nRounds = conf.get[Int]("rmt.security.hash.rounds")
    val target = s.getBytes(encoding) ++ salt ++ conf.get[String]("rmt.security.pepper").getBytes(encoding)

    (1 to nRounds).foldLeft(target)((acc, _) => sha256(acc))
  }
}
