package ru.lesson.text_processing.impls.configured

import ru.lesson.config.FilesConfig
import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles._
import ru.lesson.utils.strings.Inclusions
import zio._
import zio.cache.{Cache, Lookup}
import zio.stream.{ZPipeline, ZStream}

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


  val countLines: ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(path) map (_.getLines)).runCount

  val brRepeat: ZStream[Any, Throwable, (String, Chunk[String])] = ZStream.fromZIO(countLines) flatMap { count => ZStream.repeat("\n").take(repeatBr * count).map(str => str -> Chunk.empty) }

  def includesInDictionary(list: Chunk[String], str: String): ZIO[Any, Nothing, Long] = ZIO.succeed(list.foldLeft(0L) { (acc, world) =>
    acc + Inclusions.contain(world.toLowerCase, str.toLowerCase)
  })

  def includesInDictionaryZio(str: String, dict: Chunk[String]): ZIO[Any, Throwable, Long] = for {
    includesIn <- includesInDictionary(dict, str)
  } yield includesIn


  val parContainCount: ZPipeline[Any, Throwable, TextStr, TextStr] =
    ZPipeline.mapZIOPar[Any, Throwable, TextStr, TextStr](concurrency, buffer) {


      tstr =>
        for {

          count <- includesInDictionaryZio(tstr.value.toLowerCase, dict)
          ts <- ZIO.succeed(tstr)
          newts <- ZIO.succeed(ts.copy(dictionaryWordsCount = count))
        } yield newts
    }


  override def streamFile: ZStream[Any, Throwable, String] = for {
    stream <- (ZStream.fromIteratorScoped(source(path) map (source => source.getLines()))
      .via(split)
      .map(fileStr => fileStr -> Chunk.empty[String])
      ++ brRepeat)
      .via(toObj >>> merg10 >>> notEmptyFilter >>> parContainCount >>> toWeb >>> toJson)

  } yield stream


}

object DictionaryCached {
  def getDictionary(key: String): Task[Chunk[String]] =
    ZIO.logSpan("dictionary") {
      ZIO.acquireReleaseWith(ZIO.attempt(Source.fromFile(key)) <* ZIO.logInfo("The Dictionary was opened."))(
        x => ZIO.logInfo("The Dictionary was closed.").as(x.close())) { source => ZIO.attempt(Chunk.fromIterator(source.getLines()))}
    }
}

object TextProcessingConfigured {

  import DictionaryCached._

  val layer: ZLayer[FilesConfig, Throwable, TextProcessing] = ZLayer {
    for {

      cache <- Cache.make(
      capacity = 100,
      timeToLive = Duration.Infinity,
      lookup = Lookup(getDictionary)
      )

      config <- ZIO.service[FilesConfig]
      dict <- cache.get(config.dictPath)
      _ <- cache.get(config.dictPath)
      _ <- cache.get(config.dictPath)
      _ <- cache.get(config.dictPath)
    } yield TextProcessingConfigured(config, dict)
  }
}