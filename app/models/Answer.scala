package models

import slick.lifted.Rep
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._
import models.Riddle.riddles
import models.User.users

class Answer(tag: Tag) extends Table[(String, String, String, Boolean, String)](tag, "answers") {
  def id: Rep[String] = column[String]("id", O.PrimaryKey)
  def riddleId: Rep[String] = column[String]("riddle_id")
  def answer: Rep[String] = column[String]("answer")
  def isCorrect: Rep[Boolean] = column[Boolean]("is_correct")
  def userId: Rep[String] = column[String]("user_id")

  def answerFkRiddles = foreignKey("answers_fk_riddles", riddleId, riddles)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def answerFkUsers = foreignKey("answers_fk_users", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id, riddleId, answer, isCorrect, userId)
}

object Answer {
  val answers = TableQuery[Answer]
}
