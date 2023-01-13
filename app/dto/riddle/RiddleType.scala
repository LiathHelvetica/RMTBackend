package dto.riddle

import dto.riddle.RiddleType.{Value => ThisType}
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import slick.jdbc.PostgresProfile.api._

object RiddleType extends Enumeration {

  type RiddleType = Value

  val StringAnswer: ThisType = Value("stringAnswer")

  implicit val mapper: JdbcType[RiddleType] with BaseTypedType[RiddleType] =
    MappedColumnType.base[RiddleType, String](
      e => e.toString,
      s => RiddleType.withName(s)
    )

  implicit val jsonFormat: Format[RiddleType] = new Format[RiddleType] {
    def reads(json: JsValue): JsResult[RiddleType] = JsSuccess(RiddleType.withName(json.as[String].value))
    def writes(r: RiddleType): JsString = JsString(r.toString)
  }
}
