package ru.lesson.text_processing


import ru.lesson.utils.files.UtilsFiles._
import zio._
import zio.macros.accessible
import zio.stream.{ZPipeline, ZStream}

@accessible[TextProcessing]
trait TextProcessing {
  def readFile: Task[List[String]]

  def readAutoCloseableFile: Task[List[String]]

  def streamFile:   ZStream[Any, Throwable, String]

  val br: ZPipeline[Any, Nothing, String, String] =
    ZPipeline.map[String, String](_ + "\n")
}





