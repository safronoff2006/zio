import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object MyApp extends App {
  println("Consol,  Toy Effect")

  object part1_no_composable {
    def lesson = {
      val greet: Unit = {
        println("Как тебя зовут?")
        val name = StdIn.readLine()
        println(s"Привет, $name")
      }

      val askForAge: Unit = {
        println("Сколько тебе лет?")
        val age = StdIn.readInt()
        if (age > 18) println("Можешь проходить")
        else println("Ты не можешь пройти")
      }

    }


  }

  object part2_console {
    def lesson = {

      case class Console[A](run: () => A) {
        self: Console[A] =>
        def flatMap[B](f: A => Console[B]): Console[B] = {

          /*
          val v0: () => A = self.run
          val v1: A = self.run()
          val v2: Console[B] = f(self.run())
          val v3: B = f(self.run()).run()
          */

          Console.succeed(f(self.run()).run())
        }

        /*def map[B](f: A => B): Console[B] = {

          /*
          val v0: () => A = self.run
          val v1: A = self.run()
          val v2: B = f(self.run())
          */

          Console.succeed(f(self.run()))
        }
      */

        def map[B](f: A => B): Console[B] = {
          flatMap(a => Console.succeed(f(a)))
        }

      }

      object Console {
        def succeed[A](a: => A): Console[A] = Console(() => a)

        def printLine(str: String): Console[Unit] = Console(() => println(str))

        def readLine(): Console[String] = Console(() => StdIn.readLine())
      }


      //      val great = Console.printLine("Как твое имя ?") flatMap { _ =>
      //          Console.readLine() flatMap { name =>
      //            Console.printLine("Как твоя фамилия ?") flatMap { _ =>
      //              Console.readLine() flatMap  { family =>
      //                Console.printLine(s"Привет $family $name !")  map {
      //                  _ => family -> name
      //                }
      //              }
      //            }
      //          }
      //        }

      val great: Console[(String, String)] = for {
        _ <- Console.printLine("Как твое имя ?")
        name <- Console.readLine()
        _ <- Console.printLine("Как твоя фамилия ?")
        family <- Console.readLine()
        _ <- Console.printLine(s"Привет $family $name !")
      } yield (family, name)

      val result: (String, String) = great.run()

      println(result)

    }
  }

  object part3_effect {
    def lesson = {
      case class Effect[+E, +A](run: () => Either[E, A]) { self =>

        def map[B](f: A => B): Effect[E,B] =
          flatMap(a => Effect(() => Right(f(a))))

        def flatMap[E1 >: E ,B](f: A => Effect[E1,B]): Effect[E1,B] =
          Effect(() => self.run().fold(
            e => Effect.fail(e),
            a => f(a)
          ).run())
      }

      object Effect {
        def effect[A](a: => A): Effect[Throwable, A] = {
          Try(a) match {
            case Failure(e) => Effect(() => Left(e))
            case Success(a) => Effect(() => Right(a))
          }
        }

        def fail[E](e: => E): Effect[E, Nothing] = Effect(() => Left(e))
      }

      val great: Effect[Throwable, (String, String)] = for {
        _ <- Effect.effect(
          {
            println("Как твое имя ?")
            //throw new Throwable("Ошибка")
          }
        )

        name <- Effect.effect(StdIn.readLine())
        _ <- Effect.effect(println("Как твоя фамилия ?"))
        family <- Effect.effect(StdIn.readLine())
        _ <- Effect.effect(println(s"Привет $family $name !"))
      } yield (family, name)

      println {
        great.run()
      }


    }
  }

  //part1_no_composable.lesson
  //part2_console.lesson
  part3_effect.lesson

}