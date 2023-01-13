package models

import org.joda.time.DateTime
import slick.lifted.Rep
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.Riddle.riddles
import models.User.users

class Solution(tag: Tag) extends Table[(String, String, String, String, Boolean, DateTime)](tag, "solutions") {
  def id: Rep[String] = column[String]("id", O.PrimaryKey)
  def userId: Rep[String] = column[String]("user_id")
  def riddleId: Rep[String] = column[String]("riddle_id")
  def answer: Rep[String] = column[String]("answer")
  def isCorrect: Rep[Boolean] = column[Boolean]("is_correct")
  def answerTime: Rep[DateTime] = column[DateTime]("answer_time")

  def userFk = foreignKey("solution_user_fk", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def riddleFk = foreignKey("solution_riddle_fk", riddleId, riddles)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id, userId, riddleId, answer, isCorrect, answerTime)
}

object Solution {
  val solutions = TableQuery[Solution]
}