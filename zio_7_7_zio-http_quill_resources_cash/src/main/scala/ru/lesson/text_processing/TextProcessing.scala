package ru.lesson.text_processing


import zio._
import zio.macros.accessible
import zio.stream.ZPipeline.mapAccum
import zio.stream.{ZPipeline, ZStream}
import zio.json._

import scala.util.{Failure, Success, Try}

@accessible[TextProcessing]
trait TextProcessing {
  def readFile: Task[List[String]]

  def readAutoCloseableFile: Task[List[String]]

  def streamFile: ZStream[Any, Throwable, String]

}



object pipelines {

  final case class TextStr(
                            value: String = "",
                            dict: Chunk[String] = Chunk.empty,
                            dictionaryWordsCount: Long = 0 )

  object TextStr {
    def apply(v: String, dict: Chunk[String]): TextStr = new TextStr(v, dict)

  }


  @jsonDerive  case class TextWeb(
                            value: String = "",
                            dictionaryWordsCount: Long = 0
                          )


  val split: ZPipeline[Any, Nothing, String, String] = ZPipeline.map[String, String](str => (str + "\n").replace(". ", ".\n")) >>>
    ZPipeline.splitLines >>>
    ZPipeline.map[String, String](_.trim)

  val toObj: ZPipeline[Any, Nothing, (String, Chunk[String]), TextStr] = ZPipeline.map[(String, Chunk[String]), TextStr]( par => TextStr(par._1, par._2))

  val toDebug: ZPipeline[Any, Nothing, TextStr, String] = ZPipeline.map[TextStr, String](ts => ts.value + "  _  " + ts.dictionaryWordsCount + "\n")

  val toView: ZPipeline[Any, Nothing, TextStr, String] = ZPipeline.map[TextStr, String](_.value)

  def mergF(acc: TextStr, next: TextStr): (TextStr, TextStr) = {
    def firstIsLowercase(str: String): Boolean = Try(str(0)) match {
      case Failure(_) => false
      case Success(ch) => ch.isLower || ch.isDigit
    }

    val a = acc.value
    val n = next.value
    val dict = next.dict

    if (firstIsLowercase(n)) TextStr.apply("",dict) -> TextStr.apply(a + n,dict) else TextStr.apply(n,dict) -> TextStr.apply(a, dict)

  }

  val merg: ZPipeline[Any, Nothing, TextStr, TextStr] = mapAccum(TextStr.apply( "",Chunk.empty))((acc, next) => (mergF(acc, next)._1, mergF(acc, next)._2))


  val notEmptyFilter: ZPipeline[Any, Nothing, TextStr, TextStr] = ZPipeline.filter[TextStr](!_.value.isBlank)


  val merg10: ZPipeline[Any, Throwable, TextStr, TextStr] =
      merg >>> merg >>> merg >>> merg >>> merg >>>
      merg >>> merg >>> merg >>> merg >>> merg >>> notEmptyFilter


  val toWeb: ZPipeline[Any, Nothing, TextStr, TextWeb]
  = ZPipeline.map[TextStr, TextWeb](ts => TextWeb(ts.value,ts.dictionaryWordsCount))

  val toJson: ZPipeline[Any, Nothing, TextWeb, String] = ZPipeline.map[TextWeb, String](_.toJson).map(_ + "\n")
    .intersperse(",")
    .intersperse("[","","]")



}





