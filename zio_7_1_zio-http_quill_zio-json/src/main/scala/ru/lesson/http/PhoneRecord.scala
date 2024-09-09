package ru.lesson.http

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class PhoneRecord(id:Long, phone: String, fio:String)

object PhoneRecord {
  implicit val encoder = DeriveJsonEncoder.gen[PhoneRecord]
  implicit val decoder = DeriveJsonDecoder.gen[PhoneRecord]

}
