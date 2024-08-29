import zio.Console._
import zio._


import scala.language.postfixOps

object MyApp_Services extends ZIOAppDefault {

  def run: ZIO[Any, Throwable, Unit] = myAppLogic

  private val myAppLogic = for {
    _   <- printLine("Изучаем ZIO")

  } yield ()



}