package ru.lesson.text_processing.impls.configured

import ru.lesson.config.FilesConfig
import ru.lesson.text_processing.TextProcessing
import ru.lesson.text_processing.impls.configured.HandConf.{buffer, concurrency, repeatBr}
import ru.lesson.text_processing.pipelines._
import ru.lesson.utils.files.UtilsFiles._
import ru.lesson.utils.strings.Inclusions
import zio.Console.printLine
import zio._
import zio.stream.{ZPipeline, ZSink, ZStream}

import scala.io.Source

object HandConf {

  val repeatBr = 2
  val concurrency = 2
  val buffer = 2
}

case class TextProcessingConfigured(conf: FilesConfig) extends TextProcessing {

  val path: String = conf.bookPath
  val dictPath = conf.dictPath
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


  val  dictionaryStream: ZStream[Any, Throwable, String] = ZStream.acquireReleaseWith(
    ZIO.attempt(
      Source.fromFile(dictPath)) <* printLine("The Dictionary was opened."))(
      x => ZIO.succeed(x.close()) <* printLine("The Dictionary was closed.").orDie)
      .flatMap { is => ZStream.fromIterator(is.getLines())}

  val dictionary: ZIO[Any, Throwable, Chunk[String]] =  dictionaryStream.run(ZSink.collectAll)

  val countLines: ZIO[Any, Throwable, Long] = ZStream.fromIteratorScoped(source(path) map (_.getLines)).runCount

  val brRepeat: ZStream[Any, Throwable, String] = ZStream.fromZIO(countLines) flatMap { count => ZStream.repeat("\n").take(repeatBr * count) }

  def includesInDictionary(list: Chunk[String], str: String): ZIO[Any, Nothing, Long] = ZIO.succeed(list.foldLeft(0L) { (acc, world) =>
    acc + Inclusions.contain(world.toLowerCase, str.toLowerCase)
  })

  def includesInDictionaryZio(str: String, di: ZIO[Any, Throwable, Chunk[String]]): ZIO[Any, Throwable, Long] = for {
    dict <- di
    includesIn <- includesInDictionary(dict, str)
  } yield includesIn

  val parContainCount: ZPipeline[Any, Throwable, TextStr, TextStr] =  ZPipeline.mapZIOPar[Any, Throwable, TextStr, TextStr](concurrency, buffer) { tstr =>
  for {
      count <- includesInDictionaryZio(tstr.value.toLowerCase, dictionary)
      ts <- ZIO.succeed(tstr)
      newts <- ZIO.succeed(ts.copy(dictionaryWordsCount = count))
    } yield(newts)
  }


  override def streamFile: ZStream[Any, Throwable, String] =
    (ZStream.fromIteratorScoped(source(path) map (_.getLines())) ++
      brRepeat) via split >>> toObj >>> merg10 >>> parContainCount >>> toDebug

}

object TextProcessingConfigured {
  val layer: ZLayer[FilesConfig, Throwable, TextProcessingConfigured] = ZLayer.fromFunction(TextProcessingConfigured(_))
}