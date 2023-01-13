package models

import models.User.users
import slick.lifted.Rep
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class Admin(tag: Tag) extends Table[(Int, String)](tag, "admins") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey)
  def userId: Rep[String] = column[String]("user_id")

  def adminsFk = foreignKey("admins_fk", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id, userId)
}

object Admin {
  val admins = TableQuery[Admin]
}
