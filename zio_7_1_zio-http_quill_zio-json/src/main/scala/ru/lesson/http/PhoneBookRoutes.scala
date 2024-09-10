package ru.lesson.http

import ru.lesson.http.LogAspect._
import zio._
import zio.http._
import zio.json.{EncoderOps, JsonDecoder}

object PhoneBookRoutes {
   def apply() = Routes(
  Method.GET / "records" ->  handler {
    ZIO.logInfo(s"The GET /records endpoint called") *> ZIO.serviceWithZIO[PhoneRecordRepository](_.list())
        .mapBoth (
          e =>   Response.internalServerError(e.getMessage),
          listPhoneRecords => Response.json(listPhoneRecords.toJson))
    } ,

   Method.GET / "records" / long("id") -> handler {  (id: Long, _: Request ) =>
     ZIO.logInfo(s"The GET /records/$id endpoint called") *> ZIO.serviceWithZIO[PhoneRecordRepository](_.getBy(id))
        .foldZIO (
          e => ZIO.logWarning(e.getMessage) *> ZIO.fail( Response.internalServerError(e.getMessage)),
          phoneRecord => ZIO.succeed(Response.json(phoneRecord.toJson)))
    } ,

   Method.POST  / "records" -> handler { req:Request =>
      for{
        - <-  ZIO.logInfo(s"The POST /records endpoint called")
       body <- req.body.asString.foldZIO(
            e => ZIO.logError(s"Error convert body to String: ${e.getMessage}") *> ZIO.fail(Response.internalServerError(s"Error convert body to String: ${e.getMessage}")),
            body => ZIO.succeed(body)
         )

       phoneRecord <- ZIO.fromEither(JsonDecoder[PhoneRecord]
          .decodeJson(body))
          .foldZIO(
            e =>  ZIO.logWarning(s"Error decode body string to Phone Record: $e") *> ZIO.fail(Response.internalServerError(s"Error decode body string to Phone Record: $e")),
            pr => ZIO.succeed(pr)
          )

        r <- ZIO.serviceWithZIO[PhoneRecordRepository](_.insert(phoneRecord))
          .foldZIO(
            e => ZIO.logError(s"Ошибка вставки PhoneRecord ${e.getMessage}") *> ZIO.fail(Response.internalServerError(s"Ошибка вставки PhoneRecord ${e.getMessage}")),
            phr => ZIO.succeed(Response.json(phr.toJson)))
      }  yield r
    },

    Method.PUT  / "records" -> handler { req:Request =>
      for{
        - <-  ZIO.logInfo(s"The PUT /records endpoint called")
        body <- req.body.asString.foldZIO(
          e => ZIO.logError(s"Error convert body to String: ${e.getMessage}") *> ZIO.fail(Response.internalServerError(s"Error convert body to String: ${e.getMessage}")),
          body => ZIO.succeed(body)
        )

        phoneRecord <- ZIO.fromEither(JsonDecoder[PhoneRecord]
            .decodeJson(body))
          .foldZIO(
            e =>  ZIO.logWarning(s"Error decode body string to Phone Record: $e") *> ZIO.fail(Response.internalServerError(s"Error decode body string to Phone Record: $e")),
            pr => ZIO.succeed(pr)
          )

        r <- ZIO.serviceWithZIO[PhoneRecordRepository](_.update(phoneRecord))
          .foldZIO(
            e => ZIO.logError(s"Ошибка обновления PhoneRecord ${e.getMessage}") *> ZIO.fail(Response.internalServerError(s"Ошибка обновления PhoneRecord ${e.getMessage}")),
            phr => ZIO.succeed(Response.json(phr.toJson)))
      }  yield r
    },

    Method.DELETE / "records" / long("id") -> handler {  (id: Long, _: Request ) =>
      ZIO.logInfo(s"The DELETE /records endpoint called") *> ZIO.serviceWithZIO[PhoneRecordRepository](_.delete(id))
        .foldZIO(
        e => ZIO.logError(s"Ошибка удаления PhoneRecord ${e.getMessage}") *> ZIO.fail(Response.internalServerError(e.getMessage)),
        _ => ZIO.succeed(Response.text("Delete OK"))
      )
    }

  ) @@ logAnnotateCorrelationId
}
