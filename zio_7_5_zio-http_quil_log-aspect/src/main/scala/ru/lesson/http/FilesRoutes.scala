package ru.lesson.http

import ru.lesson.files.FilesService
import zio.ZIO
import zio.http._
import zio.stream.ZStream

object FilesRoutes {
  import ru.lesson.config.LogAspect._

    def apply(): Routes[FilesService, Nothing] = Routes(
      Method.GET / "video" -> handler { req: Request => {

        for {
          pair <- FilesService.getVideoFile
            .mapBoth(
              err => Response.internalServerError(err.getMessage),
              pair => pair
            )

          r <- ZIO.succeed(

            Response(
              status = Status.Ok,
              headers = Headers(
                Header.ContentType(MediaType.application.`octet-stream`),
                Header.ContentDisposition.attachment(pair._1)
              ),
              body = Body.fromStreamChunked(ZStream.fromFile(pair._2))
            )

          )
        } yield r

      }  @@ logAnnotateCorrelationId(req) @@ logSpan("get-video")
      }
    ) @@ logAnnotateCorrelationId
}
