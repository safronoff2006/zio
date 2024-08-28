import zio.Console._
import zio._

import java.io.{BufferedReader, FileReader, IOException}
import scala.io.BufferedSource
import scala.io.Source.fromFile
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps

object MyApp extends ZIOAppDefault {

  def run: ZIO[Any, Throwable, Unit] = myAppLogic

  private val myAppLogic = for {
    _   <- printLine("Изучаем ZIO")
    _  <- part8
    _ <- part9
    _ <- part10

  } yield ()

  val finalizer: UIO[Unit] =
    ZIO.succeed(println("Finalizing!"))

  val finalized1  =
    ZIO.fail("Effect Failed!").catchAll( es => ZIO.fail(new Throwable(es))).ensuring(finalizer)
  val finalized2  =
    ZIO.fail("Effect Failed!").catchAll( es => printLine(es)  zipRight  ZIO.succeed(es)).ensuring(finalizer)


  def openFile(name: String): IO[ IOException, BufferedSource] = printLine(s"File $name open") zipRight  ZIO.attemptBlockingIO(fromFile(name)).refineToOrDie[IOException]
  def closeFile(bs: BufferedSource): UIO[Unit] =  printLine(s"File close").orDie  zipRight   ZIO.succeed(bs.close())
  def readList(bs: BufferedSource): List[String] = bs.getLines().toList

  def  dataWorkEffect(name: String): Task[List[String]] = ZIO.acquireReleaseWith(openFile(name)) (closeFile(_)) {
    file => printLine("Обработка данных") zipRight  ZIO.attemptBlockingIO(readList(file))
  }


  private val part8 =
    for {
      _   <- printLine("Часть 8")
      _   <- printLine("-------------------")
      //_ <- finalized1
      _ <- finalized2
      _   <- printLine("")
    } yield ()

  private val part9 = for {
    _   <- printLine("Часть 9")
    _   <- printLine("-------------------")
    list  <- dataWorkEffect("data.txt")
    _ <- ZIO.foreach(list){ word  =>printLine(word) }
    _   <- printLine("")
  } yield ()


  def openReader(name: String): IO[IOException, BufferedReader] = ZIO.attemptBlockingIO {
    new BufferedReader( new FileReader(name))
  }.refineToOrDie[IOException]

  def getDataFromFile(name: String): IO[IOException, List[String]] =
    ZIO.scoped {
      for {
        reader <- ZIO.fromAutoCloseable(openReader(name))
        list   <- printLine(s"Open with resource $name") zipRight  ZIO.attemptBlockingIO(reader.lines().toList.asScala.toList)
      } yield list
    }

  private val part10 = for {
    _   <- printLine("Часть 10")
    _   <- printLine("-------------------")
    list  <- getDataFromFile("data.txt")
    _ <- ZIO.foreach(list){ word  => printLine(word) }
    _   <- printLine("")
  } yield ()

}