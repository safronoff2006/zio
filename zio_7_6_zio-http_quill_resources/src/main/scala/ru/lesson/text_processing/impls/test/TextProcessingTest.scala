package ru.lesson.text_processing.impls.test

import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles.{FileAutoCloseableLayer, FileLayer, source, sourceAutoCloseable}
import zio.stream.ZStream
import zio.{Scope, Task, TaskLayer, ZIO, ZLayer}

import scala.io.Source

object HandConf {
  val path  = "src/main/resources/test.txt"
  val repeatBr = 2
}

case class TextProcessingTest() extends TextProcessing {

  override def readFile: Task[List[String]] = readAutoCloseableFile

  override def readAutoCloseableFile: Task[List[String]] = {
    sourceAutoCloseable(HandConf.path).mapBoth(
      err => new Throwable(err),
      sr => sr.getLines().toList
    ).provide(Scope.default)
  }

  val countLines: ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(HandConf.path) map(_.getLines)).runCount

  val brRepeat: ZStream[Any, Throwable, String] = ZStream.fromZIO(countLines) flatMap { count => ZStream.repeat("\n").take(HandConf.repeatBr * count) }



  override def streamFile  =

    (  ZStream.fromIteratorScoped(source(HandConf.path) map (_.getLines())) ++
    brRepeat ) via split >>> toObj >>> merg10 >>> toDebug

}



  object TextProcessingTest {
    val sourceTaskLayer: TaskLayer[Source] = FileLayer apply HandConf.path
    val sourceAutoCLoseableTaskLayer: TaskLayer[Source] = FileAutoCloseableLayer apply HandConf.path

    val layer: TaskLayer[TextProcessingTest] =
      sourceTaskLayer >>> ZLayer.succeed(TextProcessingTest())

    val layerAutoCloseable: TaskLayer[TextProcessingTest] =
      sourceAutoCLoseableTaskLayer >>> ZLayer.succeed(TextProcessingTest())

  }
