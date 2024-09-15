package ru.lesson.files

import zio._
import zio.macros.accessible

import java.io.{File, FileNotFoundException}




@accessible[FilesService]
trait FilesService {
    def  getVideoFile: Task[String]
}

case class FilesServiceImpl() extends FilesService {
  override def getVideoFile: Task[String] = {

    val path = "src/main/resources/TestVideoFile.mp4"
    ZIO.attempt{
      val  file = new File(path)
      if (file.exists()) file.getName
      else throw new FileNotFoundException(s"File not found")
    } foldZIO(
      err => ZIO.logError(s"Error: ${err.getMessage}") *> ZIO.fail(new Throwable(err.getMessage)),
      name => ZIO.succeed(name)
    )
  }

}

object FilesServiceImpl {
  val layer: ULayer[FilesService] = ZLayer.succeed(FilesServiceImpl())
}