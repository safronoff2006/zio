package ru.lesson.utils.files

import zio._

import java.io.{BufferedReader, FileReader, IOException}
import scala.annotation.unused
import scala.io.Source

object UtilsFiles {

  @unused
  def openReader(name: String): IO[IOException, BufferedReader] = ZIO.attemptBlockingIO {
    new BufferedReader( new FileReader(name))
  }.refineToOrDie[IOException]


  private def acquire(name: => String): IO[IOException, Source] = ZIO.logSpan("acquire") {
    ZIO.logInfo("called acquire") *> ZIO.attemptBlockingIO(Source.fromFile(name))}

  private def release(source: => Source): UIO[Unit] =  ZIO.logSpan("release") {
    ZIO.logInfo("called release") *> ZIO.succeedBlocking(source.close())}

  def source(name: => String): ZIO[Scope, IOException, Source] =  ZIO.logSpan("source") {
   ZIO.logInfo("called source") *> ZIO.acquireRelease(acquire(name))(release(_)) }


  private val finalizer: UIO[Unit] = ZIO.logSpan("finalizer") { ZIO.logInfo("AutoCloseable Source Open, Work and Close")}

  def sourceAutoCloseable(name: => String): ZIO[Scope, IOException, Source] =
    ZIO.fromAutoCloseable(ZIO.attemptBlockingIO(Source.fromFile(name)))
      .ensuring(finalizer)


  private def sourceLayer(name: => String): ZLayer[Any, IOException, Source] =
    ZLayer.scoped(source(name))

  private def fileAutoCloseableLayer(name: => String): ZLayer[Any, IOException, Source] =
    ZLayer.scoped{
      ZIO.fromAutoCloseable(
        ZIO.attemptBlockingIO(Source.fromFile(name))
      )
    }


  @unused
  object FileLayer {
    def apply(name: String): TaskLayer[Source] = sourceLayer(name).mapError(e => new Throwable(e.getMessage))
  }

  @unused
  object FileAutoCloseableLayer {
    def apply(name:String): TaskLayer[Source] =
      fileAutoCloseableLayer(name).mapError(e => new Throwable(e.getMessage))
  }
}
