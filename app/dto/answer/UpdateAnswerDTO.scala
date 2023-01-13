package dto.answer

import dto.meta.ActionType.ActionType
import dto.meta.ActionType.UpdateAction
import play.api.Configuration
import play.api.libs.json.JsValue

import scala.util.Try

case class UpdateAnswerDTO(
  id: Option[String],
  answer: Option[String],
  isCorrect: Option[Boolean],
  action: Option[ActionType]
) {

  def toDeleteEntity: Try[String] = {
    Try { id.get }
  }

  def toUpdateEntity: Try[(String, String, Boolean)] = {
    Try {
      (id.get, answer.get, isCorrect.get)
    }
  }

  def toCreateEntity(id: String, userId: String, riddleId: String): Try[(String, String, String, Boolean, String)] = {
    Try {
      (id, riddleId, answer.get, isCorrect.getOrElse(true), userId)
    }
  }

  def readyForUpdate(present: AnswerDTO): Try[UpdateAnswerDTO] = {
    Try {
      new UpdateAnswerDTO(
        id.orElse(present.id),
        answer.orElse(present.answer),
        isCorrect.orElse(present.isCorrect),
        action
      )
    }
  }
}

object UpdateAnswerDTO {

  def apply(body: JsValue)(implicit conf: Configuration): Try[UpdateAnswerDTO] = {
    Try {
      new UpdateAnswerDTO(
        (body \ conf.get[String]("rmt.key.map.id")).asOpt[String],
        (body \ conf.get[String]("rmt.key.map.answer")).asOpt[String],
        (body \ conf.get[String]("rmt.key.map.isCorrect")).asOpt[Boolean],
        Some((body \ conf.get[String]("rmt.key.map.action")).as[ActionType])
      )
    }
  }

  // TODO: this is pessimistically O(n^2)
  def readyForUpdate(candidates: Set[UpdateAnswerDTO], present: Set[AnswerDTO]): Try[Set[UpdateAnswerDTO]] = {
    Try {
      candidates.flatMap {
        case updateAnswer if updateAnswer.action.get == UpdateAction => {
          val id = updateAnswer.id.get
          val presentAnswer = present.find(pA => pA.id.contains(id))
          presentAnswer match {
            case Some(pA) => updateAnswer.readyForUpdate(pA).toOption
            case None => Some(updateAnswer)
          }
        }
        case answer => Some(answer)
      }
    }
  }
}
