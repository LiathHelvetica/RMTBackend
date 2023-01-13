package models

import slick.lifted.Rep
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class User(tag: Tag) extends Table[(String, String, String, String, String)](tag, "users") {
  def id: Rep[String] = column[String]("id", O.PrimaryKey)
  def userName: Rep[String] = column[String]("user_name")
  def email: Rep[String] = column[String]("email")
  def password: Rep[String] = column[String]("password")
  def salt: Rep[String] = column[String]("salt")

  def * = (id, userName, email, password, salt)
}

object User {
  val users = TableQuery[User]
}
