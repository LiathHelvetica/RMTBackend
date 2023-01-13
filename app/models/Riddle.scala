package models

import org.joda.time.DateTime
import slick.lifted.Rep
import slick.lifted.Tag
import models.User.users

import slick.jdbc.PostgresProfile.api._
import com.github.tototoshi.slick.PostgresJodaSupport._
import dto.riddle.RiddleType._

class Riddle(tag: Tag) extends Table[(String, String, String, Option[String], DateTime, DateTime, Boolean, RiddleType)](tag, "riddles") {
  def id: Rep[String] = column[String]("id", O.PrimaryKey)
  def userId: Rep[String] = column[String]("user_id")
  def title: Rep[String] = column[String]("title")
  def contents: Rep[Option[String]] = column[Option[String]]("contents")
  def creationDate: Rep[DateTime] = column[DateTime]("creation_date")
  def lastUpdateDate: Rep[DateTime] = column[DateTime]("last_update_date")
  def isAccepted: Rep[Boolean] = column[Boolean]("is_accepted")
  def riddleType: Rep[RiddleType] = column[RiddleType]("riddle_type")

  def riddlesFk = foreignKey("riddles_fk", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id, userId, title, contents, creationDate, lastUpdateDate, isAccepted, riddleType)
}

object Riddle {
  val riddles = TableQuery[Riddle]
}