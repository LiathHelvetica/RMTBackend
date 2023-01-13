package dto.meta

import dto.meta.ActionType.{Value => ThisType}
import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

object ActionType extends Enumeration {

  type ActionType = Value

  val CreateAction: ThisType = Value("create")
  val DeleteAction: ThisType = Value("delete")
  val UpdateAction: ThisType = Value("update")

  implicit val mapper: JdbcType[ActionType] with BaseTypedType[ActionType] =
    MappedColumnType.base[ActionType, String](
      e => e.toString,
      s => ActionType.withName(s)
    )

  implicit val jsonFormat: Format[ActionType] = new Format[ActionType] {
    def reads(json: JsValue): JsResult[ActionType] = JsSuccess(ActionType.withName(json.as[String].value))
    def writes(r: ActionType): JsString = JsString(r.toString)
  }
}
