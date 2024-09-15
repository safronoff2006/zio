package ru.lesson.db

import io.getquill.jdbczio.Quill
import zio._

import javax.sql.DataSource

object DB {
  val layer: TaskLayer[DataSource] = Quill.DataSource.fromPrefix("app")
}
