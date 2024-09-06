import zio.Console.printLine
import zio._

import java.io.IOException
import scala.io.BufferedSource

object Main_DI_more extends ZIOAppDefault {
  private val app = for {
    _ <- printLine("Изучаем ZIO")
    _ <- printLine("Внедрение зависимостей. Продолжение.")
    _ <- printLine("")
    _ <- app_resource_run
    _ <- app_autoclossable_run
    _ <- printLine("-----------------------------------")
  } yield ()

  override def run = app

  // поднимаем scoped ZIO до ZLayer с помощью конструктора ZLayer.scoped

  case class ResourceA(a:Int)

  object ResourceA {
    val layer: ULayer[ResourceA] = ZLayer.scoped {
       ZIO.acquireRelease(
         acquire = ZIO.debug(
              "Поднимаем scoped ZIO до ZLayer с помощью конструктора ZLayer.scoped\n" +
              "Инициализация ResourceA").as(ResourceA(5)))(
         release = _ => ZIO.debug("Освобождение ResourceA\n-----------"
        )
      )
    }
  }

  private val app_resource_effect: ZIO[ResourceA, IOException, Int] = for {
    a <- ZIO.serviceWith[ResourceA](_.a)
  } yield a * a

  private val app_resource_run = app_resource_effect
    .debug("result")
    .provide(ResourceA.layer)


  // ZLayer.scoped  из ZIO  с конструктором  ZIO.fromAutoCloseable

  val fileLayer: TaskLayer[BufferedSource] =
    ZLayer.scoped {
      ZIO.fromAutoCloseable(
        ZIO.attemptBlocking(scala.io.Source.fromFile("file.txt"))
      )
    }

  var app_autoclossable_run: ZIO[Scope, Throwable, Unit] = for {
    _ <- printLine("ZLayer.scoped  из ZIO  с конструктором  ZIO.fromAutoCloseable")
    env <- fileLayer.build
    lines <- ZIO.attempt(env.get[BufferedSource].getLines)
    _ <- ZIO.foreachDiscard(lines.toList)(str => printLine(str))
    _ <- printLine("-----------")
  } yield ()




}