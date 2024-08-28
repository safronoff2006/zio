import zio.Console._
import zio._

import java.io.{BufferedReader, BufferedWriter, Closeable, FileReader, FileWriter, IOException}
import scala.io.{BufferedSource, Source}
import scala.io.Source.fromFile
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.postfixOps

object MyApp extends ZIOAppDefault {



  private val myAppLogic = for {
    _   <- printLine("Изучаем ZIO")
   // _  <- part8
    //_ <- part9
    //_ <- part10
    //_ <- part11
    //_ <- part12
    // _ <- part13
    _ <- part14

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


  def inOpen(name:String): IO[IOException, BufferedReader] =  ZIO.attemptBlockingIO {
    new BufferedReader( new FileReader(name))
  }

  def outOpen(name:String): IO[IOException, BufferedWriter] =  ZIO.attemptBlockingIO {
    new BufferedWriter( new FileWriter(name))
  }

  def close(resource: Closeable): UIO[Unit] = ZIO.succeed(resource.close)

  def copy(from: BufferedReader, to: BufferedWriter): IO[IOException, Long] = ZIO.attemptBlockingIO{
    from.transferTo(to)
  }

  def transfer1(src: String, dst: String): IO[IOException, Long] =
    ZIO.acquireReleaseWith(inOpen(src))(close) { in =>
      ZIO.acquireReleaseWith(outOpen(dst))(close) { out =>
        copy(in, out)
      }
    }


  def transfer2(src: String, dst: String): IO[ IOException, Long] =
    ZIO.acquireReleaseWith ( inOpen(src).zipPar(outOpen(dst)) ) {
      case (in, out) => ZIO.succeed(close(in)).zipPar(close(out))
    } {
      case (in, out) => copy(in,out)
    }


  //// использование Scope - область действия

  def transfer3(src: String, dst: String): IO[IOException, Long] = {
    val resources: ZIO[Any with Scope, IOException, (BufferedReader, BufferedWriter)] = for {
      from <- ZIO.acquireRelease(inOpen(src))(close)
      to <- ZIO.acquireRelease(outOpen(dst))(close)
    } yield (from,to)

    ZIO.scoped {
      resources.flatMap { case (in,out) =>
        copy(in,out)
      }
    }
  }


  //def transfer(src: String, dst: String): IO[IOException, Long] = transfer1(src, dst)
  //def transfer(src: String, dst: String): IO[IOException, Long] = transfer2(src, dst)
  def transfer(src: String, dst: String): IO[IOException, Long] = transfer3(src, dst)

  private val part11 = for {
    _   <- printLine("Часть 11")
    _   <- printLine("-------------------")
    copyCount <- transfer("data.txt","out.txt").catchAll { error => printLine(error.getMessage) zipRight ZIO.succeed(0L)}
    _   <- printLine(s"COPIED $copyCount BYTES")
  } yield ()


  val scopeDebug = for {
    scope <- Scope.make
    _ <- ZIO.debug("Scope is created!")
    _ <- scope.addFinalizer(
      for {
        _ <- ZIO.debug("The finalizer is started!")
        _ <- ZIO.sleep(2 seconds)
        _ <- ZIO.debug("The finalizer is done!")
      } yield ()
    )
    _ <- ZIO.debug("Leaving scope!")
    _ <- ZIO.debug("Begin work!")
    _ <- ZIO.sleep(3 seconds)
    _ <- ZIO.debug("End work!")
    _ <- scope.close(Exit.succeed(()))
    _ <- ZIO.debug("Scope is closed!")
  } yield ()

  private val part12 = for {
    _   <- printLine("Часть 12")
    _   <- printLine("-------------------")
    _ <- scopeDebug
    _   <- printLine("")
  } yield ()

  /// универсальный паттерн использования Scope

  def acquire(name: => String): IO[IOException, Source] = ZIO.attemptBlockingIO(Source.fromFile(name))

  def release(source: => Source): UIO[Unit] = ZIO.succeedBlocking(source.close())

  def source(name: => String): ZIO[Any with Scope, IOException, Source] = ZIO.acquireRelease(acquire(name))(release(_))

  def contents(name: => String): ZIO[Any, IOException, Chunk[String]] =
    ZIO.scoped {
      source(name).flatMap { source =>
        ZIO.attemptBlockingIO(Chunk.fromIterator(source.getLines))
      }
    }

  private val part13 = for {
    _   <- printLine("Часть 13")
    _   <- printLine("-------------------")
    _   <- printLine("")
    _ <- contents("data.txt").flatMap { ch =>
        ZIO.foreach(ch) { str => printLine(str) }
    }
  } yield ()

  //////////////  Получение Scope как ZIO сервиса - для понимания
  // на практике так не делать использовать б а использовать высокоуровневое ZIO.acquireReleaseи ZIO.scoped


  val resourcefulApp: ZIO[Scope, Nothing, Unit] =
    for {
      scope <- ZIO.service[Scope]
      _     <- ZIO.debug("Entering the scope!")
      _ <- scope.addFinalizer(
        for {
          _ <- ZIO.debug("The finalizer is started!")
          _ <- ZIO.sleep(5.seconds)
          _ <- ZIO.debug("The finalizer is done!")
        } yield ()
      )
      _ <- ZIO.debug("Leaving scope!")
    } yield ()

  val finalApp: UIO[Unit] =
    Scope.make.flatMap(scope => resourcefulApp.provide(ZLayer.succeed(scope)).onExit(scope.close(_)))

  private val part14 = for {
    _   <- printLine("Часть 14")
    _   <- printLine("-------------------")
    _ <- finalApp
    _   <- printLine("")


  } yield ()

  def run: ZIO[Any, Throwable, Unit] = zioapp

  val zioapp = myAppLogic

  /////////////////////////////////////////////////////////////////////////////////
  /// добавление ресурсов в main Scope

    val resource1: ZIO[Scope, Nothing, Unit] =
      ZIO.acquireRelease(ZIO.debug("Acquiring the resource 1"))(_ =>
        ZIO.debug("Releasing the resource one") *> ZIO.sleep(5.seconds)
      )
    val resource2: ZIO[Scope, Nothing, Unit] =
      ZIO.acquireRelease(ZIO.debug("Acquiring the resource 2"))(_ =>
        ZIO.debug("Releasing the resource two") *> ZIO.sleep(3.seconds)
      )

  val mainScopeApp = ZIO.scoped(
    for {
      scope <- ZIO.scope
      _     <- ZIO.debug("Entering the main scope!")
      _     <- scope.addFinalizer(ZIO.debug("Releasing the main resource!") *> ZIO.sleep(2.seconds))
      _     <- scope.extend(resource1)
      _     <- scope.extend(resource2)
      _     <- ZIO.debug("Leaving scope!")
    } yield ()
  )

  //val zioapp = mainScopeApp



}