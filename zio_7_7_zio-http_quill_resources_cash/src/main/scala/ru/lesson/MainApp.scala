package ru.lesson

import ru.lesson.config.FilesConfig
import ru.lesson.files.FilesServiceImpl
import ru.lesson.http.{FilesRoutes, PhoneBookRoutes, PhoneRecordRepository, TextRoutes}
import ru.lesson.text_processing.impls.configured.TextProcessingConfigured
import zio._
import zio.config.typesafe._
import zio.http._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object MainApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    SLF4J.slf4j(LogFormat.colored) ++  Runtime.setConfigProvider(
      TypesafeConfigProvider
        .fromResourcePath()
    )

  object Hello {
    def apply(): Routes[Any, Nothing] = Routes(
      Method.GET / "hello" -> Handler.text("Hello World")
    )
  }


  override def run = {
    Server.serve(PhoneBookRoutes() ++ Hello() ++ FilesRoutes() ++ TextRoutes()).provide(
      Server.default,
      PhoneRecordRepository.layer,
      FilesServiceImpl.layer,
      TextProcessingConfigured.layer,
      FilesConfig.layer
    )
  }
}