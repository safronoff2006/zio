package ru.lesson.text_processing.impls.test

import ru.lesson.config.FilesConfig
import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles.{source, sourceAutoCloseable}
import zio.Console.printLine
import zio.cache.{Cache, Lookup}
import zio.stream.{ZPipeline, ZStream}
import zio.{Chunk, Duration, Ref, Scope, Task, UIO, ZIO, ZLayer}

import scala.annotation.unused
import scala.io.Source

object HandConf {
  val path  = "src/main/resources/test.txt"
  val repeatBr = 2
  val concurrency = 10
  val buffer = 100
  val dictPath = "src/main/resources/dictionary.txt"
}

case class TextProcessingTest(dict: Chunk[String]) extends TextProcessing {
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


  val parContainCount: ZPipeline[Any, Throwable, TextStr, TextStr] = ZPipeline.mapZIOPar[Any, Throwable, TextStr, TextStr](concurrency, buffer) { tstr =>
    for {
      count <- includesInDictionaryZio(tstr.value.toLowerCase, dict)
      ts <- ZIO.succeed(tstr)
      newts <- ZIO.succeed(ts.copy(dictionaryWordsCount = count))
    } yield newts
  }


  override def streamFile: ZStream[Any, Throwable, String] = for {
    stream <- (ZStream.fromIteratorScoped(source(path) map (source => source.getLines())).via(split) ++ brRepeat(path, repeatBr))
      .via(toObj >>> merg10 >>> notEmptyFilter >>> parContainCount >>> toDebug)
  } yield stream
}



  @unused
  object TextProcessingTest {

    import ru.lesson.text_processing.DictionaryCached._

    val layer: ZLayer[FilesConfig, Throwable, TextProcessingTest] = ZLayer {
      for {

        cache <- Cache.make(
          capacity = 100,
          timeToLive = Duration.Infinity,
          lookup = Lookup(getDictionary)
        )

        config <- ZIO.service[FilesConfig]
        dict <- cache.get(config.dictPath)

      } yield TextProcessingTest(dict)

    }
  }
