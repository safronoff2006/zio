package ru.lesson.text_processing

import zio._
import zio.macros.accessible
import zio.stream.ZPipeline.mapAccum
import zio.stream.{ZPipeline, ZStream}

import scala.util.{Failure, Success, Try}

@accessible[TextProcessing]
trait TextProcessing {
  def readFile: Task[List[String]]

  def readAutoCloseableFile: Task[List[String]]

  def streamFile: ZStream[Any, Throwable, String]

}

object pipelines {

  final case class TextStr(value: String)

  object TextStr {
    def apply(v: String): TextStr = new TextStr(v)
  }

  val br = ZPipeline.map[String, String](_ + "\n")

  val insideBr: ZPipeline[Any, Nothing, TextStr, TextStr] = ZPipeline.map[TextStr, TextStr](ts => ts.copy(value = ts.value + "\n"))

  val split: ZPipeline[Any, Nothing, String, String] = ZPipeline.map[String, String](str => (str + "\n").replace(". ", ".\n")) >>>
    ZPipeline.splitLines >>>
    ZPipeline.map[String, String](_.trim)

  val toObj = ZPipeline.map[String, TextStr](TextStr(_))

  val toDebug = ZPipeline.map[TextStr, String](_.toString + "\n")

  val toView = ZPipeline.map[TextStr, String](_.value)

  def mergF(acc: TextStr, next: TextStr): (TextStr, TextStr) = {
    def firstIsLowercase(str: String): Boolean = Try(str(0)) match {
      case Failure(_) => false
      case Success(ch) => ch.isLower || ch.isDigit
    }

    val a = acc.value
    val n = next.value

    if (firstIsLowercase(n)) (TextStr apply "") -> (TextStr apply a + n) else (TextStr apply n) -> (TextStr apply a)

  }

  val merg: ZPipeline[Any, Nothing, TextStr, TextStr] = mapAccum(TextStr apply "")((acc, next) => (mergF(acc, next)._1, mergF(acc, next)._2))


  val notEmptyFilter: ZPipeline[Any, Nothing, TextStr, TextStr] = ZPipeline.filter[TextStr](_.value.nonEmpty)

  val merg10: ZPipeline[Any, Nothing, TextStr, TextStr] =
    merg >>>
      merg >>>
      merg >>>
      merg >>>
      merg >>>
      merg >>>
      merg >>>
      merg >>>
      merg >>> notEmptyFilter
}





