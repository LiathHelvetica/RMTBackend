package dto.riddle

import dto.answer.UpdateAnswerDTO
import dto.riddle.RiddleType.RiddleType
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue

import scala.util.Try

case class UpdateRiddleDTO(
  title: Option[String],
  contents: Option[String],
  lastUpdateDate: Option[DateTime],
  riddleType: Option[RiddleType],

  answers: Set[UpdateAnswerDTO]
) {

  def toEntity: Try[(String, Option[String], DateTime, RiddleType)] = {
    Try {
      (title.get, contents, lastUpdateDate.get, riddleType.get)
    }
  }

  def readyForUpdate(present: MyRiddleDTO): Try[UpdateRiddleDTO] = {
    Try {
      new UpdateRiddleDTO(
        title.orElse(present.title),
        contents.orElse(present.contents),
        Some(DateTime.now),
        riddleType.orElse(present.riddleType),
        UpdateAnswerDTO.readyForUpdate(answers, present.answers).get
      )
    }
  }
}

object UpdateRiddleDTO {

  def apply(body: JsValue)(implicit conf: Configuration): UpdateRiddleDTO = {
    new UpdateRiddleDTO(
      (body \ conf.get[String]("rmt.key.map.riddleTitle")).asOpt[String],
      (body \ conf.get[String]("rmt.key.map.riddleContents")).asOpt[String],
      None,
      (body \ conf.get[String]("rmt.key.map.riddleType")).asOpt[RiddleType],
      (body \ conf.get[String]("rmt.key.map.answers")).asOpt[JsArray] match {
        case Some(aArr) => aArr.value.flatMap(a => UpdateAnswerDTO(a).toOption).toSet
        case None => Set.empty
      }
    )
  }
}
