package util.conversion

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import play.api.Configuration

object ParsableDateTime {

  implicit class FromConfigParsableDateTime(val dt: DateTime) extends AnyVal {

    def parse(implicit conf: Configuration): String = {
      val formatter: DateTimeFormatter = DateTimeFormat.forPattern(conf.get[String]("rmt.dateTime.format"))
      formatter.print(dt)
    }
  }
}
