import zio.Console._
import zio._

import java.io.{FileNotFoundException, IOException}
import scala.io.Source._
import scala.language.postfixOps

object MyApp extends ZIOAppDefault {

  def run: ZIO[Any, Throwable, Unit] = myAppLogic

  private val myAppLogic = for {
    _   <- printLine("Изучаем ZIO")
    _  <- part5
    _  <- part6
    _  <- part7


  } yield ()

  private val zeither: Task[Either[String, Nothing]] = ZIO.fail("Uh oh!").either

  def getError(ei: Either[String, Nothing]): String = ei.fold(l => l, _ => "")

  def readFile(fileName: String): List[String] = fromFile(fileName).getLines().toList

  def zreadFile(fileName: String): IO[IOException, List[String]] = ZIO.attempt(readFile(fileName)).refineToOrDie[IOException]

  val reserved1: IO[ IOException, List[String]] = zreadFile("main.txt").catchAll { error =>
    for {
     // _    <- ZIO.logErrorCause("Could not open primary file", Cause.fail(error))
      _    <- ZIO.logError("Could not open primary file")
      file <- zreadFile("backup.txt")
    } yield  file
  }

  val reserved2: IO[IOException, List[String]] = zreadFile("main.txt").orElse(zreadFile("backup.txt"))

  val reserved3: IO[IOException, List[String]] =  zreadFile("main.txt").catchSome {
    case _: FileNotFoundException => zreadFile("backup.txt")
  }

  private val part5 =
    for {
      _   <- printLine("Часть 5")
      _   <- printLine("-------------------")
      ei <- zeither
      _   <- printLine(getError(ei))
      _   <- printLine("")
    } yield ()

  private val part6 =
    for {
      _   <- printLine("Часть 6")
      _   <- printLine("-------------------")
      _ <- reserved1.flatMap(list => ZIO.foreach(list){ world => printLine(world) })
      _ <- reserved2.flatMap(list => ZIO.foreach(list){ world => printLine(world) })
      _ <- reserved3.flatMap(list => ZIO.foreach(list){ world => printLine(world) })
      _   <- printLine("")
    } yield ()

  lazy val DefaultData: List[String] = List("error1","error2", "error3")

 val  primaryOrDefaultData: Task[List[String]] =  zreadFile("main.txt").fold (
   _ => DefaultData,
   data => data
 )

  val primaryOrSecondaryData: IO[IOException, List[String]] = zreadFile("main.txt").foldZIO(
    _ => zreadFile("backup.txt"),
    data => ZIO.succeed(data)
  )

  val retryOpenFile =  zreadFile("main.txt").retryOrElse( Schedule.recurs(1) , (e,_:Long ) => printLine(s"${e.getMessage}") zipRight  ZIO.succeed(DefaultData))

  private val part7 =
    for {
      _   <- printLine("Часть 7")
      _   <- printLine("-------------------")
      _ <- primaryOrDefaultData.flatMap(list => ZIO.foreach(list){ world => printLine(world) })
      _ <- primaryOrSecondaryData.flatMap(list => ZIO.foreach(list){ world => printLine(world) })
      _ <- retryOpenFile.flatMap(list => ZIO.foreach(list){ world => printLine(world) })
      _   <- printLine("")
    } yield ()
}