

object Main_FunctionalDesign extends App {
  println("Оьъектно ориентированный дизайн")
  val event1 =  OOP_Design.ClickEvent("click", 123, "element 1")
  event1.print
 val newevent1 = OOP_Design.updateTimestamp(event1, 345L)
  newevent1.print

  val event2 = OOP_Design.ViewEvent("view", 134L, "page 1")
  event2.print
  val newevent2 = OOP_Design.updateTimestamp(event2, 373747473L)
  newevent2.print
  println("--------------------------")

  println("Функциональный дизайн. Исполняемый стиль.")
  val app1: Execution_Design.IO[Unit] =
    for {
      _    <- Execution_Design.IO.succeed(print("Введите ваше имя: "))
      name <- Execution_Design.IO.succeed(scala.io.StdIn.readLine())
      _    <- Execution_Design.IO.succeed(println(s"Привет, $name"))
    } yield ()


  app1.unsafeRunSync()
  println("--------------------------")

  println("Функциональный дизайн. Декларативный  стиль.")
  val app2: Declarative_Design.IO[Unit] =
    for {
      _    <- Declarative_Design.IO.succeed(print("Введите ваше имя: "))
      name <- Declarative_Design.IO.succeed(scala.io.StdIn.readLine())
      _    <- Declarative_Design.IO.succeed(println(s"Привет, $name"))
    } yield ()


  app2.unsafeRunSync()
  println("--------------------------")

}

/// ООП дизайн
object OOP_Design {

  abstract class Event {
    def id: String
    def timestamp: Long
    def print: Unit = println(s"$id   $timestamp")
  }

  case class ClickEvent(id: String, timestamp: Long, element: String) extends Event
  case class ViewEvent(id: String, timestamp: Long, page: String) extends Event

  def updateTimestamp[E <: Event](event: E, timestamp: Long): Event =  event match {
      case e: ClickEvent => e.copy(timestamp = timestamp).asInstanceOf[E]
      case e: ViewEvent => e.copy(timestamp = timestamp).asInstanceOf[E]
  }

}

// Функциональный дизайн. Исполняемый стиль.

object Execution_Design {
  final case class IO[+A](private val thunk: () => A) {
    def map[B](f: A => B): IO[B] = IO.succeed(f(thunk()))

    def flatMap[B](f: A => IO[B]): IO[B] = IO.succeed(f(thunk()).unsafeRunSync())

    def unsafeRunSync(): A = thunk()
  }

  object IO {
    def succeed[A](value: => A): IO[A] = IO(() => value)
  }

}


// Функциональный дизайн. Исполняемый стиль.

object Declarative_Design {
  sealed trait IO[+A] { self =>
    def map[B](f: A => B): IO[B] = flatMap(f andThen IO.succeedNow)

    def flatMap[B](f: A => IO[B]): IO[B] = IO.FlatMap(self, f)

    def unsafeRunSync(): A = {
      type Cont = Any => IO[Any]

      def run(stack: List[Cont], currentIO: IO[Any]): A = {
        def continue(value: Any) =
          stack match {
            case ::(cont, next) => run(next, cont(value))
            case Nil            => value.asInstanceOf[A]
          }

        currentIO match {
          case IO.SucceedNow(value) => continue(value)
          case IO.Succeed(thunk)    => continue(thunk())
          case IO.FlatMap(io, cont) => run(stack appended cont, io)
        }
      }

      run(stack = Nil, currentIO = self)
    }

  }

  object IO {
    def succeedNow[A](value: A): IO[A] = IO.SucceedNow(value)
    def succeed[A](value: => A): IO[A] = IO.Succeed(() => value)

    final case class SucceedNow[A](value: A)                    extends IO[A]
    final case class Succeed[A](thunk: () => A)                 extends IO[A]
    final case class FlatMap[A, B](io: IO[A], cont: A => IO[B]) extends IO[B]
  }
}