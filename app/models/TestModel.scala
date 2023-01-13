package models

import slick.lifted.Rep
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class TestModel(tag: Tag) extends Table[(String, Int, Boolean)](tag, "test"){
  def id: Rep[String] = column[String]("id", O.PrimaryKey)
  def age: Rep[Int] = column[Int]("age")
  def isHappy: Rep[Boolean] = column[Boolean]("is_happy")

  def * = (id, age, isHappy)
}

object TestModel {
  val testTable = TableQuery[TestModel]
}