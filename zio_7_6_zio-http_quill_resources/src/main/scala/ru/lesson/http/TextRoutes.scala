package ru.lesson.http

import ru.lesson.text_processing.TextProcessing
import zio.ZIO
import zio.http._
import zio.json._

object TextRoutes {
  def apply() = Routes(
      Method.GET / "text" -> handler {
        for {
          list <- TextProcessing.readFile.foldZIO(
            e => ZIO.fail(Response.internalServerError(e.getMessage)),
            list => ZIO.succeed(list)
          )
          r <- ZIO.attemptBlocking(Response.json(list.toJson)).mapBoth(
            err => Response.internalServerError(err.getMessage),
            resp => resp
          )

        } yield r
      },
    Method.GET / "textstream" -> handler {
      for {
        stream <- ZIO.serviceWith[TextProcessing](_.streamFile)
        r <- ZIO.attempt(
          Response(
            status = Status.Ok,
            headers = Headers(
              Header.ContentType(MediaType.application.`octet-stream`),
              Header.ContentDisposition.attachment("efef")
            ),
            body = Body.fromCharSequenceStreamChunked(stream)
          )
        ).mapBoth(
          err => Response.internalServerError(err.getMessage),
          resp => resp
        )
      } yield r
    }

  )
}
