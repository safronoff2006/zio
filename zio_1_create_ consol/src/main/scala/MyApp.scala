import zio.Console._
import zio._

import java.io.IOException
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Try

object MyApp extends ZIOAppDefault {

  def run: ZIO[Any, Throwable, Unit] = myAppLogic

  private val myAppLogic = for {
    _   <- printLine("Изучаем ZIO")
    _   <- printLine("")
   // _   <- part1
   // _   <- part2
   // _   <- part3
    _   <- part4
  } yield ()

  private val part1 =
    for {
      _   <- printLine("Часть 1")
      _   <- printLine("-------------------")
      _    <- printLine("Привет! Как твое имя ?")
      name <- readLine.flatMap(line  => printLine(s"Я тебя понял $line") zipRight ZIO.succeed(line))
      _    <- printLine(s"Здорова, $name, добро пожаловать в  ZIO!")
      _   <- printLine("")
    } yield ()

  private val zoption1: IO[Option[Nothing], String] = ZIO.fromOption(Some("Наш пацан!"))
  private val zoption2: IO[Option[Nothing], Nothing] = ZIO.fromOption(None)


  val whoEffect: IO[IOException, String] = readLine.flatMap(line =>
    line match {
      case "Валера" => zoption1
      case _ => zoption2
    }
  ) orElseFail  (new IOException("Да ты фраер"))

  private val part2 = for {
    _   <- printLine("Часть 2")
    _   <- printLine("-------------------")
    _ <- printLine("Как твое погоняло?")
    name <- whoEffect
    _ <- printLine(name)
    _   <- printLine("")
  } yield ()


  val readIdEffect: ZIO[Any, Throwable, Option[Int]] = readLine.flatMap { line =>
    val opt= Try(line.toInt).toOption
    opt match {
      case Some(_) =>  ZIO.attempt(opt)
      case None =>  printLine("ID должен быть целым  числом, введите заново") zipRight  readIdEffect
    }

  }

  case class User(id: Int, name: String , team: Int)
  case class Team(id: Int, name: String)

  def getUser(userId: Int): IO[Throwable, Option[User]] = userId match {
    case id: Int  if id > 0 => ZIO.succeed(  Some(User(id, s"User $id", 123)) )
    case _ =>  printLine("Не найден User !!!!") zipRight  ZIO.succeed(None)
  }


  def getTeam(teamId: Int): IO[Throwable, Team] = teamId match {
    case id: Int if id == 123 => ZIO.succeed(Team(123,"Рабочая группа 123"))
    case _ =>  printLine("Рабочая группа не найдена !!!!") zipRight  ZIO.fail(new Throwable("Рабочая группа не найдена"))
    }

  val results: IO[Throwable, Option[(Int, User, Team)]] = (for {
    id   <- readIdEffect.some
    user <- getUser(id).some
    team <- getTeam(user.team).asSomeError
  } yield (id, user, team)).unsome






  private val part3 = ( for {
    _   <- printLine("Часть 3")
    _   <- printLine("-------------------")
    _ <- printLine("Введите id:")
    res <- results.some
    id <- ZIO.succeed(res._1)
    user <- ZIO.succeed(res._2)
    team <- ZIO.succeed(res._3)
    _ <- printLine(s"User id =   ${id.toString}")
    _ <- printLine(s"User  =   ${user.toString}")
    _ <- printLine(s"Team  =   ${team.toString}")
    _   <- printLine("")
  } yield () ) orElse(printLine("Что то пошло не так"))


  lazy val future = Future.successful("Hello!")

  val zfuture: ZIO[Any, Throwable, String] =
    ZIO.fromFuture { implicit ec =>
      future.map(_ => "Goodbye!")
    }


  val readLine1: Task[String] =
    ZIO.attempt(StdIn.readLine())


  val readLine2: IO[ IOException, String] =
    ZIO.attempt(StdIn.readLine()).refineToOrDie[IOException]

  private val part4 = for {
    _   <- printLine("Часть 4")
    _   <- printLine("-------------------")
    res <- zfuture
    _   <- printLine(s"res = $res")
    _ <- printLine("Введите строку") zipRight  readLine2.flatMap( line =>    printLine(s"Эхо: $line") )
  } yield ()

/// создание эффекта из асинхронного  кода
  case class AuthError(msg: String)

  object legacy {
    def login(
               onSuccess: User => Unit,
               onFailure: AuthError => Unit): Unit = {


    }
  }

  val login: ZIO[Any, AuthError, User] =
    ZIO.async[Any, AuthError, User] { callback =>
      legacy.login(
        user => callback(ZIO.succeed(user)),
        err  => callback(ZIO.fail(err))
      )
    }

}