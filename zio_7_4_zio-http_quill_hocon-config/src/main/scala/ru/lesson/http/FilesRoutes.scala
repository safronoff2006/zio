package ru.lesson.http

import ru.lesson.files.FilesService
import zio.ZIO
import zio.http._


import zio.stream.ZStream

object FilesRoutes {
    def apply() = Routes(
      Method.GET / "video" -> handler {

        for {
          pair <- FilesService.getVideoFile
            .mapBoth(
              err =>  Response.internalServerError(err.getMessage),
              pair => pair
             )

          r <-  ZIO.succeed(

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

      }

    )
}
