package ru.lesson.http

import ru.lesson.files.FilesService
import zio.ZIO
import zio.http.{Body, Header, Headers, MediaType, Method, Response, Routes, Status, handler}
import zio.stream.ZStream

object FilesRoutes {
    def apply() = Routes(
      Method.GET / "video" -> handler {

        for {
          name <- FilesService.getVideoFile
            .mapBoth(
                 err =>  Response.internalServerError(err.getMessage),
                 name => name
             )

          r <-  ZIO.succeed(

            Response(
              status = Status.Ok,
              headers = Headers(
                Header.ContentType(MediaType.application.`octet-stream`),
                Header.ContentDisposition.attachment(name)
              ),
              body = Body.fromStreamChunked(ZStream.fromResource(name))
            )

          )
        } yield r

      }

    )
}
