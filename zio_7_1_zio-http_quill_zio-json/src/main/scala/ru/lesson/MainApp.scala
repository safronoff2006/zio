package ru.lesson

import ru.lesson.http.{PhoneBookRoutes, PhoneRecordRepository}
import zio._
import zio.http._

object MainApp extends ZIOAppDefault {

  private val httpRoutes: Routes[Any, Nothing] = Routes(
    Method.GET / "hello" -> Handler.text("Hello World")
  )


  override def run = {
    Server.serve(PhoneBookRoutes.routes).provide(Server.default, PhoneRecordRepository.layer)
  }
}