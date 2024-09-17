package ru.lesson.utils.files

import zio._

import java.io.{BufferedReader, FileReader, IOException}
import scala.io.Source

object UtilsFiles {

  def openReader(name: String): IO[IOException, BufferedReader] = ZIO.attemptBlockingIO {
    new BufferedReader( new FileReader(name))
  }.refineToOrDie[IOException]


  private def acquire(name: => String): ZIO[Any, IOException, Source] =
    ZIO.logInfo("called acquire") *> ZIO.attemptBlockingIO(Source.fromFile(name))

  private def release(source: => Source): ZIO[Any, Nothing, Unit] =
    ZIO.logInfo("called release") *> ZIO.succeedBlocking(source.close())

  def source(name: => String): ZIO[Scope, IOException, Source] =
   ZIO.logInfo("called source") *> ZIO.acquireRelease(acquire(name))(release(_))


  val finalizer: UIO[Unit] = ZIO.logInfo("AutoCloseable Source Open, Work and Close")

  def sourceAutoCloseable(name: => String): ZIO[Scope, IOException, Source] =
    ZIO.fromAutoCloseable(ZIO.attemptBlockingIO(Source.fromFile(name)))
      .ensuring(finalizer)


  private def sourceLayer(name: => String): ZLayer[Any, IOException, Source] =
    ZLayer.scoped(source(name))

  def fileAutoCloseableLayer(name: => String): ZLayer[Any, IOException, Source] =
    ZLayer.scoped{
      ZIO.fromAutoCloseable(
        ZIO.attemptBlockingIO(Source.fromFile(name))
      )
    }


  object FileLayer {
    def apply(name: String): TaskLayer[Source] = sourceLayer(name).mapError(e => new Throwable(e.getMessage))
  }

  object FileAutoCloseableLayer {
    def apply(name:String): TaskLayer[Source] =
      fileAutoCloseableLayer(name).mapError(e => new Throwable(e.getMessage))
  }
}
