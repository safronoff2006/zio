package ru.lesson.http

import zio._
import zio.http._
import zio.json.EncoderOps

object PhoneBookRoutes {
   val routes = Routes(
  Method.GET / "records" ->  handler {
      ZIO.serviceWithZIO[PhoneRecordRepository](_.list())
        .mapBoth (
          e => Response.internalServerError(e.getMessage),
          listPhoneRecords => Response.json(listPhoneRecords.toJson))
    },

    Method.GET / "records" / long("id") -> handler {  (id: Long, _: Request ) =>
      ZIO.serviceWithZIO[PhoneRecordRepository](_.getBy(id))
        .mapBoth (
          e => Response.internalServerError(e.getMessage),
          phoneRecord => Response.json(phoneRecord.toJson) )
    },

//    Method.POST  / "records" -> handler { req:Request =>
//      for {
//        body <- req.body.asString
//        phoneRecord <- ZIO.fromEither(JsonDecoder[PhoneRecord]
//          .decodeJson(body))
//          .orElseFail(new Throwable("Fail decode json"))
//        repo <- ZIO.service[PhoneRecordRepository]
//        r <- repo.insert(phoneRecord)
//          .mapBoth(
//            e => Response.internalServerError(e.getMessage),
//            phr => Response.json(phr.toJson) )
//      }  yield r
//    },

//    Method.PUT  / "records" -> handler { req:Request =>
//      for {
//        body <- req.body.asString
//        phoneRecord <- ZIO.fromEither(JsonDecoder[PhoneRecord]
//            .decodeJson(body))
//          .orElseFail(new Throwable("Fail decode json"))
//        repo <- ZIO.service[PhoneRecordRepository]
//        phr <- repo.update(phoneRecord)
//      }  yield Response.json(phr.toJson)
//    },

//    Method.DELETE / "records" / long("id") -> handler {  (id: Long, _: Request ) =>
//      ZIO.serviceWithZIO[PhoneRecordRepository](_.delete(id)).as(Response.text("Delete OK"))
//    }

  )
}
