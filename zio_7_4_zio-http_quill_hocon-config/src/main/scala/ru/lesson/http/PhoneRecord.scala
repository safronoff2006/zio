package ru.lesson.http


import zio.json._
import zio.schema._


case class PhoneRecord(id: Long, phone: String, fio: String)

object PhoneRecord {

  implicit val encoder: JsonEncoder[PhoneRecord] = DeriveJsonEncoder.gen[PhoneRecord]
  implicit val decoder: JsonDecoder[PhoneRecord] = DeriveJsonDecoder.gen[PhoneRecord]

  implicit val schema: Schema[PhoneRecord] = DeriveSchema.gen[PhoneRecord]
  implicit val jsCodec: JsonCodec[PhoneRecord] = zio.schema.codec.JsonCodec.jsonCodec(schema)

}
