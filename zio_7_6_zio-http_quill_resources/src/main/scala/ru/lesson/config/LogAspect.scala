package ru.lesson.config

import zio._
import zio.http._

object LogAspect {

  def logSpan(label: String): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](zio: ZIO[R, E, A])(implicit trace: Trace): ZIO[R, E, A] =
        ZIO.logSpan(label)(zio)
    }


  def logAnnotateCorrelationId(req: Request): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](zio: ZIO[R, E, A])(implicit trace: Trace): ZIO[R, E, A] =
        correlationId(req).flatMap(id => ZIO.logAnnotate("request-correlation-id", id)(zio))

      def correlationId(req: Request): UIO[String] =
        ZIO
          .succeed(req.headers.get("X-Correlation-ID"))
          .flatMap(id => Random.nextUUID.map(uuid => id.getOrElse(uuid.toString)))
    }


    def logAnnotateCorrelationId: Middleware[Any] =
      new Middleware[Any] {
        override def apply[Env1 <: Any, Err](
            app: Routes[Env1, Err]
        ): Routes[Env1, Err] =
          app.transform { h =>
            handler { (req: Request) =>
              def correlationId(req: Request): UIO[String] =
                ZIO
                  .succeed(req.headers.get("X-Correlation-ID"))
                  .flatMap(x =>
                    Random.nextUUID.map(uuid => x.getOrElse(uuid.toString))
                  )

              correlationId(req).flatMap(id =>
                ZIO.logAnnotate("correlation-id", id)(h(req))
              )
            }
          }
      }

}
