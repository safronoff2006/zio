package ru.lesson.text_processing.impls.configured

import ru.lesson.config.FilesConfig
import ru.lesson.text_processing.TextProcessing
import ru.lesson.utils.files.UtilsFiles._
import zio._
import zio.stream.ZStream

import scala.io.Source

case class TextProcessingConfigured(conf: FilesConfig) extends TextProcessing {

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



  override def streamFile: ZStream[Any, Throwable, String] =
    ZStream.fromIteratorScoped(
      source(conf.bookPath) map (_.getLines())
    ) via br

}

object TextProcessingConfigured {

  val sourceTaskLayer: TaskLayer[Source] =
    FilesConfig.layer.flatMap(FileLayer apply _.get[FilesConfig].bookPath)

  val sourceAutoCLoseableTaskLayer: TaskLayer[Source] =
    FilesConfig.layer.flatMap(FileAutoCloseableLayer apply _.get[FilesConfig].bookPath)

  val layer: ZLayer[FilesConfig, Throwable, TextProcessingConfigured] =
    sourceTaskLayer >>> ZLayer.fromFunction(TextProcessingConfigured(_))

  val layerAutoCloseable: ZLayer[FilesConfig, Throwable, TextProcessingConfigured] =
    sourceAutoCLoseableTaskLayer >>> ZLayer.fromFunction(TextProcessingConfigured(_))

}