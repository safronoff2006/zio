package ru.lesson.http

import ru.lesson.text_processing.TextProcessing
import zio.ZIO
import zio.http._
import zio.json._

object TextRoutes {

  import ru.lesson.config.LogAspect._

  def apply(): Routes[TextProcessing, Nothing] = Routes(
    Method.GET / "text" -> handler { req: Request => {
      for {
        _ <- ZIO.logInfo(s"The GET /text endpoint called")
        list <- TextProcessing.readFile.foldZIO(
          e => ZIO.fail(Response.internalServerError(e.getMessage)),
          list => ZIO.succeed(list)
        )
        r <- ZIO.attemptBlocking(
          Response.json(list.toJson))
          .mapBoth(
            err => Response.internalServerError(err.getMessage),
            resp => resp
        )
      } yield r
    } @@ logAnnotateCorrelationId(req) @@ logSpan("get-text")


    },
    Method.GET / "textstream" -> handler { req:Request =>
    {
      for {
           _ <- ZIO.logInfo(s"The GET /textstream endpoint called")
          stream <- ZIO.serviceWith[TextProcessing](_.streamFile)
          r <- ZIO.attempt(
            Response(
              status = Status.Ok,
              headers = Headers(
                //Header.ContentType(MediaType.application.`octet-stream`),
                // Header.ContentDisposition.attachment("efef")
                //Header.ContentType(MediaType.application.`octet-stream`),
                Header.ContentType(MediaType.text.plain, charset = Some(Charsets.Utf8)),
              ),
              body = Body.fromCharSequenceStreamChunked(stream)
            )
          ).mapBoth(
            err => Response.internalServerError(err.getMessage),
            resp => resp
          )
        } yield r
      } @@ logAnnotateCorrelationId(req) @@ logSpan("get-textstream")
    }

  ) @@ logAnnotateCorrelationId
}
