package ru.lesson.text_processing.impls.configured

import ru.lesson.config.FilesConfig
import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.impls.configured.HandConf.{buffer, concurrency, repeatBr}
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles._
import ru.lesson.utils.strings.Inclusions
import zio.Console.printLine
import zio._
import zio.stream.{ZPipeline, ZStream}

import scala.io.Source

object HandConf {

  val repeatBr = 2
  val concurrency = 2
  val buffer = 2
}


case class TextProcessingConfigured(conf: FilesConfig) extends TextProcessing {

  private val path: String = conf.bookPath
  private val dictPath: String = conf.dictPath

  case class ChunkString(value: Ref[Chunk[String]]) {
    def get: UIO[Chunk[String]] = value.get
  }

  object ChunkString {
    def make: Task[ChunkString] = ZIO.acquireReleaseWith(ZIO.attempt(Source.fromFile(dictPath)) <* printLine("The Dictionary was opened."))(
      x => printLine("The Dictionary was closed.").orDie.as(x.close())) { source =>
      Ref.make(Chunk.fromIterator(source.getLines())).map(ChunkString(_))
    }
  }


  override def readFile: Task[List[String]] = {
    source(conf.bookPath).mapBoth(
      err => new Throwable(err),
      sr => sr.getLines().toList
    ).provide(Scope.default)
  }

  override def readAutoCloseableFile: Task[List[String]] = {
    sourceAutoCloseable(conf.bookPath).mapBoth(
      err => new Throwable(err),
      sr => sr.getLines().toList
    ).provide(Scope.default)
  }


  val countLines: ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(path) map (_.getLines)).runCount

  val brRepeat: ZStream[Any, Throwable, (String, Chunk[String])] = ZStream.fromZIO(countLines) flatMap { count => ZStream.repeat("\n").take(repeatBr * count).map(str => str -> Chunk.empty) }

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
    } yield (newts)
  }


  override def streamFile: ZStream[Any, Throwable, String] = for {
    chankString <- ZStream.fromZIO(ChunkString.make)
    dict <- ZStream.fromZIO(chankString.value.get)
    _ <- ZStream.fromZIO(printLine(s"Dict  $dict"))
    stream <- (ZStream.fromIteratorScoped(source(path) map (source => source.getLines()))
      .via(split)
      .map(fileStr => fileStr -> dict)
      ++ brRepeat)
      .via(toObj >>> merg10 >>> notEmptyFilter >>> parContainCount >>> toDebug)


  } yield stream


}

object TextProcessingConfigured {
  val layer: ZLayer[FilesConfig, Throwable, TextProcessingConfigured] = ZLayer.fromFunction(TextProcessingConfigured(_))
}