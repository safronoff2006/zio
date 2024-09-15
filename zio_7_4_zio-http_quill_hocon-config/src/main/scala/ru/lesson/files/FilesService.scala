package ru.lesson.files

import ru.lesson.config.FilesConfig
import zio._
import zio.macros.accessible
import java.io.{File, FileNotFoundException}


@accessible[FilesService]
trait FilesService {
    def  getVideoFile: Task[(String,File)]
}

case class FilesServiceImpl(fileConfig: FilesConfig) extends FilesService {
  override def getVideoFile: Task[(String, File)] = {
    ZIO.attempt{
      val  file = new File(fileConfig.videoPath)
      if (file.exists()) fileConfig.videoName -> file
      else throw new FileNotFoundException(s"File not found")
    } foldZIO(
      err => ZIO.logError(s"Error: ${err.getMessage}") *> ZIO.fail(new Throwable(err.getMessage)),
      pair => ZIO.succeed(pair)
    )
  }

}

object FilesServiceImpl {
  val layer: ZLayer[Any, Config.Error, FilesService] = FilesConfig.filesConfig() >>>  ZLayer.fromFunction(FilesServiceImpl(_))
}