package ru.lesson.text_processing.impls.configured

import ru.lesson.config.FilesConfig
import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles._
import zio._
import zio.cache.{Cache, Lookup}
import zio.stream.{ZPipeline, ZStream}

import scala.annotation.unused
import scala.io.Source


case class TextProcessingConfigured(conf: FilesConfig, dict: Chunk[String]) extends TextProcessing {

  private val path: String = conf.bookPath
  private val dictPath: String = conf.dictPath
  private val repeatBr: Int = conf.repeatBr
  private val concurrency: Int = conf.concurrency
  private val buffer: Int = conf.buffer

  case class ChunkString(value: Ref[Chunk[String]]) {
    def get: UIO[Chunk[String]] = value.get
  }

  object ChunkString {
    def make: Task[ChunkString] = ZIO.logSpan("make-chunk") {
      ZIO.acquireReleaseWith(ZIO.attempt(Source.fromFile(dictPath)) <* ZIO.logInfo("The Dictionary was opened."))(
        x => ZIO.logInfo("The Dictionary was closed.").as(x.close())) { source =>
        Ref.make(Chunk.fromIterator(source.getLines())).map(ChunkString(_))
      }
    }
  }


  override def readFile: Task[List[String]] = {
    ZIO.logSpan("read-file") {
      source(conf.bookPath).mapBoth(
        err => new Throwable(err),
        sr => sr.getLines().toList
      ) <* ZIO.logInfo("read File")
    }.provide(Scope.default)
  }

  override def readAutoCloseableFile: Task[List[String]] = {
    ZIO.logSpan("read-auto-closable-file") {
      sourceAutoCloseable(conf.bookPath).mapBoth(
        err => new Throwable(err),
        sr => sr.getLines().toList
      ) <* ZIO.logInfo("read auto clossable File")
    }.provide(Scope.default)
  }


  val parContainCount: ZPipeline[Any, Throwable, TextStr, TextStr] =
    ZPipeline.mapZIOPar[Any, Throwable, TextStr, TextStr](concurrency, buffer) { tstr =>
        for {
          count <- includesInDictionaryZio(tstr.value.toLowerCase, dict)
          ts <- ZIO.succeed(tstr)
          newts <- ZIO.succeed(ts.copy(dictionaryWordsCount = count))
        } yield newts
    }


  override def streamFile: ZStream[Any, Throwable, String] = for {
    stream <- (ZStream.fromIteratorScoped(source(path) map (source => source.getLines())).via(split) ++ brRepeat(path, repeatBr))
      .via(toObj >>> merg10 >>> notEmptyFilter >>> parContainCount >>> toWeb >>> toJson)
  } yield stream


}


@unused
object TextProcessingConfigured {

  import ru.lesson.text_processing.DictionaryCached._

  val layer: ZLayer[FilesConfig, Throwable, TextProcessing] = ZLayer {
    for {

      cache <- Cache.make(
      capacity = 100,
      timeToLive = Duration.Infinity,
      lookup = Lookup(getDictionary)
      )

      config <- ZIO.service[FilesConfig]
      dict <- cache.get(config.dictPath)

    } yield TextProcessingConfigured(config, dict)
  }
}