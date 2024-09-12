package ru.lesson.http

import ru.lesson.http.LogAspect._
import zio._
import zio.http._
import zio.json.EncoderOps
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

object PhoneBookRoutes {
  def apply() = Routes(
    Method.GET / "records" -> handler {
      ZIO.logInfo(s"The GET /records endpoint called") *> ZIO.serviceWithZIO[PhoneRecordRepository](_.list())
        .mapBoth(
          e => Response.internalServerError(e.getMessage),
          listPhoneRecords => Response.json(listPhoneRecords.toJson))
    },

    Method.GET / "records" / long("id") -> handler { (id: Long, _: Request) =>
      ZIO.logInfo(s"The GET /records/$id endpoint called") *> PhoneRecordRepository.getBy(id)
        .mapBoth(
          e => Response.internalServerError(e.getMessage),
          {
            case Some(phoneRecord) => "" -> Response(body = Body.from(phoneRecord)).addHeader("content-type", "application/json")
            case None => s"PhoneRecord $id not found!" -> Response.notFound(s"PhoneRecord $id not found!")
          }
        ) flatMap {
        case ("", responce) => ZIO.succeed(responce)
        case (warn: String, responce) => ZIO.logWarning(warn).as(responce)
        case _ => ZIO.logError("Неправильный тип").as(Response.internalServerError("Неправильный тип"))
      }
    },


    Method.POST / "records" -> handler { req: Request =>
      for {
        - <- ZIO.logInfo(s"The POST /records endpoint called")
        phoneRecord <- req.body.to[PhoneRecord]
          .foldZIO(
            e => ZIO.logWarning(s"Error decode bofdy string to Phone Record: $e") *> ZIO.fail(Response.internalServerError(s"Error decode body string to Phone Record: $e")),
            pr => ZIO.succeed(pr)
          )

        r <- ZIO.serviceWithZIO[PhoneRecordRepository](_.insert(phoneRecord))
          .foldZIO(
            e => ZIO.logError(s"Ошибка вставки PhoneRecord ${e.getMessage}") *> ZIO.fail(Response.internalServerError(s"Ошибка вставки PhoneRecord ${e.getMessage}")),
            phr => ZIO.succeed(Response.json(phr.toJson)))
      } yield r
    },

    Method.PUT / "records" -> handler { req: Request =>
      for {
        - <- ZIO.logInfo(s"The PUT /records endpoint called")
        phoneRecord <- req.body.to[PhoneRecord]
          .foldZIO(
            e => ZIO.logWarning(s"Error decode body string to Phone Record: $e") *> ZIO.fail(Response.internalServerError(s"Error decode body string to Phone Record: $e")),
            pr => ZIO.succeed(pr)
          )

        r <- ZIO.serviceWithZIO[PhoneRecordRepository](_.update(phoneRecord))
          .foldZIO(
            e => ZIO.logError(s"Ошибка обновления PhoneRecord ${e.getMessage}") *> ZIO.fail(Response.internalServerError(s"Ошибка обновления PhoneRecord ${e.getMessage}")),
            phr => ZIO.succeed(Response.json(phr.toJson)))
      } yield r
    },

    Method.DELETE / "records" / long("id") -> handler { (id: Long, _: Request) =>
      ZIO.logInfo(s"The DELETE /records endpoint called") *> ZIO.serviceWithZIO[PhoneRecordRepository](_.delete(id))
        .foldZIO(
          e => ZIO.logError(s"Ошибка удаления PhoneRecord ${e.getMessage}") *> ZIO.fail(Response.internalServerError(e.getMessage)),
          _ => ZIO.succeed(Response.text("Delete OK"))
        )
    }

  ) @@ logAnnotateCorrelationId
}
