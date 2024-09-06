import zio.Console._
import zio._

import java.io.IOException

object Main_DI extends ZIOAppDefault {
  private val app = for {
    _ <- printLine("Изучаем ZIO")
    _ <- printLine("Инверсия управления. Внедрение зависимостей. ZEnvironment. Конструкторы. ZLayer.")
    _ <- printLine("")
    _ <- part1_inversion
    - <- part2_вecoupling_from_шmplementations
    _ <- part3_binding_interfaces_to_implementations
    - <- part4_effectful_constructors
    _ <- part4_workflow_1
    _ <- part4_workflow_2
    _ <- part4_workflow_3
    _ <- printLine("---------------------")
  } yield ()

  override def run = app


  class Formatter_1 {
    def format(code: String): UIO[String] =
      ZIO.succeed(code) // dummy implementation
  }

  class Compiler_1 {
    def compile(code: String): UIO[String] =
      ZIO.succeed(code) // dummy implementation
  }

  // Part1_Inversion - инверсия управлени

  class Editor_1(formatter: Formatter_1, compiler: Compiler_1) {
    def formatAndCompile(code: String): UIO[String] =
      formatter.format(code).flatMap(compiler.compile)
  }

  val part1_inversion = for {
    _ <- printLine("Part1_Inversion - инверсия управления")
    formatter = new Formatter_1() // creating formatter
    compiler = new Compiler_1() // creating compiler
    editor = new Editor_1(formatter, compiler)
    _ <- editor.formatAndCompile("Data Data").flatMap(str => printLine(str) )
    _ <- printLine("------\n")
  } yield ()


  //Part2_Decoupling_from_Implementations - отсоединение от реализации

  trait Formatter {
    def format(code: String): UIO[String]
  }

  class Formatter_2 extends Formatter {
    def format(code: String): UIO[String] =
      ZIO.succeed(s"Work format: $code") // dummy implementation
  }

  trait Compiler {
    def compile(code: String): UIO[String]
  }

  class Compiler_2 extends Compiler {
    def compile(code: String): UIO[String] =
      ZIO.succeed(s"Work compile: $code") // dummy implementation
  }

  trait Editor {
    def formatAndCompile(code: String): UIO[String]
  }

  class Editor_2(formatter: Formatter, compiler: Compiler) extends Editor {
    def formatAndCompile(code: String): UIO[String] =
      formatter.format(code).flatMap(compiler.compile)
  }

  /// тестовые реализации
  class TestFormatter extends Formatter {
    def format(code: String): UIO[String] =
      ZIO.succeed(s"Test format: $code") // dummy implementation
  }

  class TestCompiler extends Compiler {
    def compile(code: String): UIO[String] =
      ZIO.succeed(s"Test compile: $code") // dummy implementation
  }

  val part2_вecoupling_from_шmplementations = for {
    _ <- printLine("Part2_Decoupling_from_Implementations - отсоединение от реализации")
    formatter = new Formatter_2() // creating formatter
    compiler = new Compiler_2() // creating compiler
    editor = new Editor_2(formatter, compiler)
    _ <- editor.formatAndCompile("Data Data").flatMap(str => printLine(str) )
    _ <- printLine("------\n")

    testformatter = new TestFormatter()
    testCompiler = new TestCompiler()
    editor_for_test = new Editor_2(testformatter, testCompiler)
    _ <- editor_for_test.formatAndCompile("Data Data").flatMap(str => printLine(str) )
    _ <- printLine("------\n")
  } yield ()

  //Part3_Binding_Interfaces_to_Implementations - Привязка интерфейсов к их реализациям



  val part3_binding_interfaces_to_implementations = for {
    _ <- printLine("Part3_Binding_Interfaces_to_Implementations - привязка интерфейсов к их реализациям")
    formatter = new Formatter_2() // creating formatter
    compiler = new Compiler_2() // creating compiler
    editor = new Editor_2(formatter, compiler)
    environment = ZEnvironment[Formatter, Compiler, Editor](formatter, compiler, editor)
    _ <- environment.get[Editor].formatAndCompile("Data for work").flatMap(str => printLine(str))
    _ <- printLine("------\n")
  } yield ()


  //Part4_Effectful_Constructors - эффективные конструкторы

  trait Counter {
    def inc: UIO[Unit]
    def dec: UIO[Unit]
    def get: UIO[Int]
  }

  case class CounterLive(ref: Ref[Int]) extends Counter {
    def inc: UIO[Unit] = ref.update(_ + 1)
    def dec: UIO[Unit] = ref.update(_ - 1)
    def get: UIO[Int]  = ref.get
  }

  object CounterLive {
    // эффективный конструктор
    def make: UIO[Counter] = Ref.make(0).map(new CounterLive(_))

    val layer: ULayer[Counter] = ZLayer.fromZIO(CounterLive.make)
  }

  case class Formatter_3() extends Formatter {
    def format(code: String): UIO[String] = ZIO.succeed(s"Work format: $code")
  }

  object Formatter_3 {
    val layer: ULayer[Formatter] = ZLayer.succeed(Formatter_3())
  }

  case class Compiler_3() extends Compiler {
    def compile(code: String): UIO[String] =
      ZIO.succeed(s"Work compile: $code") // dummy implementation
  }

  object Compiler_3 {
    val layer: ULayer[Compiler] = ZLayer.succeed(Compiler_3())
  }

  case class EditorLive(
                    formatter: Formatter,
                    compiler: Compiler,
                    counter: Counter
                  ) extends Editor {

    def formatAndCompile(code: String): UIO[String] = for {
        frm <- formatter.format(code)
        cmp <- compiler.compile(frm)
        _ <- ZIO.foreachDiscard(1 to 55)(_ => counter.inc)
        cou <- counter.get.flatMap(cou => ZIO.succeed(cou.toString))
      } yield s"$cmp Count: $cou"

  }

  object EditorLive {
    val layer: ZLayer[Counter with Compiler with Formatter, Nothing, Editor] = ZLayer {
      for {
        formatter <- ZIO.service[Formatter]
        compiler  <- ZIO.service[Compiler]
        counter   <- ZIO.service[Counter]
      } yield EditorLive(formatter, compiler, counter)
    }
  }


  val part4_effectful_constructors: ZIO[Scope, IOException, Unit] = for {
    _ <- printLine("Part4_Effectful_Constructors - эффективные конструкторы")
    environment = ((Formatter_3.layer ++ Compiler_3.layer ++ CounterLive.layer) >>> EditorLive.layer).build
    editor <- environment.map(_.get[Editor])
    _ <- editor.formatAndCompile("Data + count").flatMap(str => printLine(str))
    _ <- printLine("------\n")
  } yield ()

  val part4_workflow_1: ZIO[Scope, IOException, Unit] = for {
    _ <- printLine("Part4_workflow_1")
    env <-  (Formatter_3.layer ++ Compiler_3.layer ++ CounterLive.layer ).build
    form <- env.get[Formatter].format("Data")
    comp <- env.get[Compiler].compile(form)
    counter = env.get[Counter]
    _ <- ZIO.foreachDiscard(1 to 10){ _ => counter.inc }
    cou <- counter.get.flatMap(cou =>  ZIO.succeed(cou.toString))
    _ <- printLine(s"$comp Count: $cou")
    _ <- printLine("------\n")
  } yield ()

  val workflow_2: ZIO[Counter with Compiler with Formatter, IOException, Unit] = for {
    _ <- printLine("Part4_workflow_2")
    formatter <- ZIO.service[Formatter]
    compiler  <- ZIO.service[Compiler]
    counter <- ZIO.service[Counter]
    _ <- ZIO.foreachDiscard(1 to 100) {_ => counter.inc}
    form <- formatter.format("D a t a")
    comp <- compiler.compile(form)
    count <- counter.get
    _ <- printLine(s"$comp  Count: $count")
    _ <- printLine("------\n")
  } yield ()

  val part4_workflow_2: IO[IOException, Unit] = workflow_2.provideLayer(Formatter_3.layer ++ Compiler_3.layer ++ CounterLive.layer)
  val part4_workflow_3: IO[IOException, Unit] = workflow_2.provide(Formatter_3.layer, Compiler_3.layer, CounterLive.layer)
}

