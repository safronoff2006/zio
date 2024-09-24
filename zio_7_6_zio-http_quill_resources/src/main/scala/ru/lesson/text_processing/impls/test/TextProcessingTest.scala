package ru.lesson.text_processing.impls.test

import ru.lesson.text_processing.TextProcessing

import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles.{source, sourceAutoCloseable}
import ru.lesson.utils.strings.Inclusions
import zio.Console.printLine
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Ref, Scope, Task, TaskLayer, UIO, ZIO, ZLayer}

import scala.io.Source

object HandConf {
  val path  = "src/main/resources/test.txt"
  val repeatBr = 2
  val concurrency = 10
  val buffer = 100
  val dictPath = "src/main/resources/dictionary.txt"
}

case class TextProcessingTest() extends TextProcessing {
  import HandConf._


  case class ChunkString(value: Ref[Chunk[String]]) {
    def get: UIO[Chunk[String]] = value.get
  }

  object ChunkString {
    def make: Task[ChunkString] =  ZIO.acquireReleaseWith(ZIO.attempt(Source.fromFile(dictPath))  <* printLine("The Dictionary was opened."))(
      x => printLine("The Dictionary was closed.").orDie.as(x.close())){ source =>
      Ref.make(Chunk.fromIterator(source.getLines())).map(ChunkString(_))
    }
  }

  override def readFile: Task[List[String]] = readAutoCloseableFile

  override def readAutoCloseableFile: Task[List[String]] = {
    sourceAutoCloseable(HandConf.path).mapBoth(
      err => new Throwable(err),
      sr => sr.getLines().toList
    ).provide(Scope.default)
  }

  val countLines: ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(path) map (_.getLines)).runCount

  val brRepeat: ZStream[Any, Throwable, (String, Chunk[String])] = ZStream.fromZIO(countLines) flatMap { count => ZStream.repeat("\n").take(repeatBr * count).map(str => str -> Chunk.empty ) }

  def includesInDictionary(list: Chunk[String], str: String): ZIO[Any, Nothing, Long] = ZIO.succeed(list.foldLeft(0L) { (acc, world) =>
    acc + Inclusions.contain(world.toLowerCase, str.toLowerCase)
  })

  def includesInDictionaryZio(str: String, dict: Chunk[String]): ZIO[Any, Throwable, Long] = for {
    includesIn <- includesInDictionary(dict, str)
  } yield includesIn

  val parContainCount: ZPipeline[Any, Throwable, TextStr, TextStr] = ZPipeline.mapZIOPar[Any, Throwable, TextStr, TextStr](concurrency, buffer) { tstr =>
    for {
      count <- includesInDictionaryZio(tstr.value.toLowerCase, tstr.dict)
      ts <- ZIO.succeed(tstr)
      newts <- ZIO.succeed(ts.copy(dictionaryWordsCount = count))
    } yield(newts)
  }


  override def streamFile: ZStream[Any, Throwable, String] = for {
    chankString <-ZStream.fromZIO(ChunkString.make)
    dict <- ZStream.fromZIO(chankString.value.get)
    stream <- (ZStream.fromIteratorScoped(source(path) map (source => source.getLines()))
      .via (ZPipeline.splitLines)
      .map(fileStr => fileStr -> dict) ++
      brRepeat) via toObj >>> merg10 >>> notEmptyFilter >>> parContainCount >>> toDebug
  } yield stream

}



  object TextProcessingTest {
    val layer: TaskLayer[TextProcessingTest] = ZLayer.succeed(TextProcessingTest())
  }
