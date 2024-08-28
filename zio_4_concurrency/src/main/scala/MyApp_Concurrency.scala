import zio.Console._
import zio._
import zio.stream._

import scala.language.postfixOps

object MyApp_Concurrency extends ZIOAppDefault {

  def run: ZIO[Any, Throwable, Unit] = myAppLogic

  private val myAppLogic = for {
    _   <- printLine("Изучаем ZIO")
    //_  <- part11
    // _  <- part12
    //_  <- part13
    _  <- part14


  } yield ()


  def fib(n: Long): UIO[Long] =
    ZIO.suspendSucceed {
      if (n <= 1) ZIO.succeed(n)
      else fib(n - 1).zipWith(fib(n - 2))(_ + _)
    }

  def fibFiber(n:Int): UIO[Fiber[Nothing, Long]] =
    for {
      fiber <- fib(n).fork
    } yield fiber

  private val part11 =
    for {
      _   <- printLine("Часть 11")
      _   <- printLine("-------------------")
      n <- ZIO.succeed(40)
      fiber <- fibFiber(n)
      result <- fiber.join
      _ <- printLine(s"Fibonachi $n = $result")
      _   <- printLine("")
    } yield ()

  private val hiFiber =   for {
    fiber   <- ZIO.succeed("Hi!").fork
    message <- fiber.join
  } yield message

  private val awaitFiber = for {
    fiber <- ZIO.fail("Hi!").fork
    exit  <- fiber.await
  } yield exit


  val interrupt1  =for {
    fiber <- ZIO.succeed("Hi!").forever.fork
    exit  <- fiber.interrupt
  } yield exit

  val interrupt2: ZIO[Any, Nothing, Exit[Nothing, Exit[Nothing, Nothing]]] = for {
    fiber <- ZIO.succeed("Hi!").forever.fork
    fiber2     <- fiber.interrupt.fork // I don't care!
    exit <- fiber2.await
  } yield exit

  val interrupt3: ZIO[Any, Nothing, Unit] = for {
    fiber <- ZIO.succeed("Hi!").forever.fork
    unit   <- fiber.interruptFork // I don't care!
  } yield unit


  private val part12 = for {
    _   <- printLine("Часть 12")
    _   <- printLine("-------------------")
   // _ <- awaitFiber.flatMap(exit => printLine(s"$exit"))
   // _ <- hiFiber.flatMap(str => printLine(str))
    _ <- interrupt1.flatMap(exit => printLine(s"$exit"))
    _ <- interrupt2.flatMap(exit => printLine(s"$exit"))
    _ <- interrupt3.flatMap(unit => printLine(s"$unit"))

    _   <- printLine("")
  } yield ()



  val fibzip =  for {
    fiber1 <- ZIO.succeed("Hi!").fork
    fiber2 <- ZIO.succeed("Bye!").fork
    fiber   = fiber1.zip(fiber2)
    tuple  <- fiber.join
  } yield tuple

  val fibOrElse = for {
    bool <- ZIO.succeed(false)
    fiber1 <- ( if (bool)  ZIO.succeed("Uh oh!") else  ZIO.fail("Uh oh!") ).fork
    fiber2 <- ZIO.succeed("Hurray!").fork
    fiber  = fiber1.orElse(fiber2)
    message  <- fiber.join
  } yield message

  private val part13 = for {
    _   <- printLine("Часть 13")
    _   <- printLine("-------------------")
    tuple <- fibzip
    _ <- printLine(s"${tuple._1}   ${tuple._2}")
    _ <- fibOrElse.flatMap(str => printLine(str))
    _   <- printLine("")
  } yield ()

val effectGood: UIO[String] = for {
  good <- ZIO.succeed("Good!")
  _ <- printLine(good).orDie
} yield  good

  val effectBy: UIO[String] = for {
    by <- ZIO.succeed("By!")
    _ <- printLine(by).orDie
  } yield  by

  val effectMan: UIO[String] = for {
    man <- ZIO.succeed("Man")
    _ <- printLine(man).orDie
  } yield  man

  val listEffects = List(effectGood, effectBy, effectMan)
  val colEffect = ZIO.collectAllPar(listEffects)



  val zippar: UIO[(String, String)] =  for {
    tuple <- effectGood zipPar effectBy
  } yield  tuple




  val zipWPar: UIO[String] =  effectGood.zipWithPar(effectBy)( (a:String,b:String) => (a + " " + b).toUpperCase)

  private val part14 = for {
    _   <- printLine("Часть 14")
    _   <- printLine("-------------------")
    tuplepar <- zippar
    _ <- printLine(s"${tuplepar._1}   ${tuplepar._2}")
    _ <- zipWPar.flatMap(str => printLine(str))
    _ <- colEffect.flatMap(list => printLine( list.foldLeft("")((acc, elem) => acc + " " +  elem ).trim))
    _   <- printLine("")
  } yield ()

}