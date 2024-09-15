package dev.zio.quickstart

import dev.zio.quickstart.users.{PersistentUserRepo, UserRoutes}
import zio._
import zio.http._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object MainApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    SLF4J.slf4j(LogFormat.colored)

  def run = {
    Server
      .serve(UserRoutes())
      .provide(
        Server.defaultWithPort(8080),

        // To use the persistence layer, provide the `PersistentUserRepo.layer` layer instead
        PersistentUserRepo.layer
      )
  }
}
