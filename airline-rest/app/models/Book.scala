package models

import play.api.libs.json.Json

object Book {

  case class Book(name: String, author: String)

  implicit val bookWrites = Json.writes[Book]
  implicit val bookReads = Json.reads[Book]

  var books = List(Book("TAOCP", "Knuth"), Book("SICP", "Sussman, Abelson"))

  def addBook(b: Book) = books = books ::: List(b)
}
