package ru.lesson.db

import io.getquill.jdbczio.Quill

object DB {
  val layer = Quill.DataSource.fromPrefix("app")
}
