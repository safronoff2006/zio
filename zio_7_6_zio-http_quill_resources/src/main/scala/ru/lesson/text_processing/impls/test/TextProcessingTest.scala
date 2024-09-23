package ru.lesson.text_processing.impls.test

import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles.{source, sourceAutoCloseable}
import ru.lesson.utils.strings.Inclusions
import zio.stream.{ZPipeline, ZStream}
import zio.{Scope, Task, TaskLayer, ZIO, ZLayer}

object HandConf {
  val path  = "src/main/resources/test.txt"
  val repeatBr = 2
  val concurrency = 10
  val buffer = 100
}

case class TextProcessingTest() extends TextProcessing {
  import HandConf._

  override def readFile: Task[List[String]] = readAutoCloseableFile

  override def readAutoCloseableFile: Task[List[String]] = {
    sourceAutoCloseable(HandConf.path).mapBoth(
      err => new Throwable(err),
      sr => sr.getLines().toList
    ).provide(Scope.default)
  }

  val countLines: ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(path) map(_.getLines)).runCount

  val brRepeat: ZStream[Any, Throwable, String] = ZStream.fromZIO(countLines) flatMap { count => ZStream.repeat("\n").take(repeatBr * count) }

  def includesInDictionary(list: List[String], str: String):Long = list.foldLeft(0L){(acc, world) =>
    acc + Inclusions.contain(world.toLowerCase, str.toLowerCase)
  }

  val  parContainCount: ZPipeline[Any, Throwable, TextStr, TextStr] = ZPipeline.mapZIOPar[Any, Throwable, TextStr, TextStr](concurrency,buffer){ tstr =>
    ZIO.attempt(tstr.copy(dictionaryWordsCount = includesInDictionary(List("один", "два", "девять"),tstr.value)))
  }

  override def streamFile: ZStream[Any, Throwable, String] =
    (  ZStream.fromIteratorScoped(source(path) map (_.getLines())) ++
    brRepeat ) via split >>> toObj >>> merg10 >>> parContainCount >>> toDebug


}



  object TextProcessingTest {
    val layer: TaskLayer[TextProcessingTest] = ZLayer.succeed(TextProcessingTest())
  }
