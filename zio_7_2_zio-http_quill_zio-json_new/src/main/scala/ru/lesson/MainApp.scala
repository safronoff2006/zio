package ru.lesson

import ru.lesson.http.{PhoneBookRoutes, PhoneRecordRepository}
import zio._
import zio.http._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object MainApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    SLF4J.slf4j(LogFormat.colored)

  object Hello {
    def apply(): Routes[Any, Nothing] = Routes(
      Method.GET / "hello" -> Handler.text("Hello World")
    )
  }


  override def run: ZIO[Any, Throwable, Nothing] = {
    Server.serve(PhoneBookRoutes() ++ Hello()).provide(Server.default, PhoneRecordRepository.layer)
  }
}