package ru.lesson.text_processing


import ru.lesson.utils.files.UtilsFiles.source
import ru.lesson.utils.strings.Inclusions
import zio._
import zio.macros.accessible
import zio.stream.ZPipeline.mapAccum
import zio.stream.{ZPipeline, ZStream}
import zio.json._

import scala.annotation.unused
import scala.io.Source
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
                            dictionaryWordsCount: Long = 0 )

  object TextStr {
    def apply(v: String): TextStr = new TextStr(v)

  }


  @jsonDerive  case class TextWeb(
                            value: String = "",
                            dictionaryWordsCount: Long = 0
                          )


  val split: ZPipeline[Any, Nothing, String, String] = ZPipeline.map[String, String](str => (str + "\n").replace(". ", ".\n")) >>>
    ZPipeline.splitLines >>>
    ZPipeline.map[String, String](_.trim)

  val toObj: ZPipeline[Any, Nothing, String, TextStr] = ZPipeline.map[String, TextStr](TextStr(_))

  val toDebug: ZPipeline[Any, Nothing, TextStr, String] = ZPipeline.map[TextStr, String](ts => ts.value + "  _  " + ts.dictionaryWordsCount + "\n")

  @unused
  val toView: ZPipeline[Any, Nothing, TextStr, String] = ZPipeline.map[TextStr, String](_.value)

  private def mergF(acc: TextStr, next: TextStr): (TextStr, TextStr) = {
    def firstIsLowercase(str: String): Boolean = Try(str(0)) match {
      case Failure(_) => false
      case Success(ch) => ch.isLower || ch.isDigit
    }

    val a = acc.value
    val n = next.value


    if (firstIsLowercase(n)) TextStr.apply("") -> TextStr.apply(a + n) else TextStr.apply(n) -> TextStr.apply(a)

  }

  private val merg: ZPipeline[Any, Nothing, TextStr, TextStr] = mapAccum(TextStr.apply( ""))((acc, next) => (mergF(acc, next)._1, mergF(acc, next)._2))


  val notEmptyFilter: ZPipeline[Any, Nothing, TextStr, TextStr] = ZPipeline.filter[TextStr](!_.value.isBlank)


  val merg10: ZPipeline[Any, Throwable, TextStr, TextStr] =
      merg >>> merg >>> merg >>> merg >>> merg >>>
      merg >>> merg >>> merg >>> merg >>> merg >>> notEmptyFilter


  val toWeb: ZPipeline[Any, Nothing, TextStr, TextWeb]
  = ZPipeline.map[TextStr, TextWeb](ts => TextWeb(ts.value,ts.dictionaryWordsCount))

  val toJson: ZPipeline[Any, Nothing, TextWeb, String] = ZPipeline.map[TextWeb, String](_.toJson).map(_ + "\n")
    .intersperse(",")
    .intersperse("[","","]")



  def includesInDictionary(list: Chunk[String], str: String): ZIO[Any, Nothing, Long] = ZIO.succeed(list.foldLeft(0L) { (acc, world) =>
    acc + Inclusions.contain(world.toLowerCase, str.toLowerCase)
  })

  def includesInDictionaryZio(str: String, dict: Chunk[String]): ZIO[Any, Throwable, Long] = for {
    includesIn <- includesInDictionary(dict, str)
  } yield includesIn

  def countLines(path: String): ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(path) map (_.getLines)).runCount

  def brRepeat(path: String, repeatBr: Int): ZStream[Any, Throwable, String] = ZStream.fromZIO(countLines(path)) flatMap { count =>
    ZStream.repeat("\n").take(repeatBr * count)
  }



}

object DictionaryCached {
  def getDictionary(key: String): Task[Chunk[String]] =
    ZIO.logSpan("dictionary") {
      ZIO.acquireReleaseWith(ZIO.attempt(Source.fromFile(key)) <* ZIO.logInfo("The Dictionary was opened."))(
        x => ZIO.logInfo("The Dictionary was closed.").as(x.close())) { source => ZIO.attempt(Chunk.fromIterator(source.getLines()))}
    }
}



