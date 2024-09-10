package ru.lesson.http

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class PhoneRecord(id:Long, phone: String, fio:String)

object PhoneRecord {

  implicit val encoder: JsonEncoder[PhoneRecord] = DeriveJsonEncoder.gen[PhoneRecord]
  implicit val decoder: JsonDecoder[PhoneRecord] = DeriveJsonDecoder.gen[PhoneRecord]

}
