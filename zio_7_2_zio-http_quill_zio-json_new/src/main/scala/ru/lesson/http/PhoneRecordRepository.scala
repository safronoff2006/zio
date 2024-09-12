package ru.lesson.http

import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import ru.lesson.db.DB
import zio._
import zio.macros.accessible
import javax.sql.DataSource


@accessible[PhoneRecordRepository]
trait PhoneRecordRepository {
  def list(): Task[List[PhoneRecord]]

  def getBy(id: Long): Task[Option[PhoneRecord]]

  def insert(phoneRecord: PhoneRecord): Task[PhoneRecord]

  def update(phoneRecord: PhoneRecord): Task[PhoneRecord]

  def delete(id: Long): Task[Unit]

}

class PhoneRecordRepositoryImpl(ds: DataSource) extends PhoneRecordRepository {

  val ctx = new PostgresZioJdbcContext(SnakeCase)

  import ctx._

  val dsLayer = ZLayer.succeed(ds)

  val phoneReckordSchema = quote {
    query[PhoneRecord]
  }

  override def list(): Task[List[PhoneRecord]] =
    ctx.run(phoneReckordSchema)
      .mapError(e => new Throwable(e.getMessage)).provide(dsLayer)

  override def getBy(id: Long): Task[Option[PhoneRecord]] =
    ctx.run(phoneReckordSchema.filter(_.id == lift(id)))
      .mapBoth(e => new Throwable(e.getMessage), _.headOption)
      .provide(dsLayer)

  override def insert(phoneRecord: PhoneRecord): Task[PhoneRecord] =
    ctx.run(phoneReckordSchema.insertValue(lift(phoneRecord)))
      .mapBoth(e => new Throwable(e.getMessage), _ => phoneRecord)
      .provide(dsLayer)

  override def update(phoneRecord: PhoneRecord): Task[PhoneRecord] =
    ctx.run(phoneReckordSchema.filter(_.id == lift(phoneRecord.id))
        .updateValue(lift(phoneRecord)).returning(v => v))
      .mapBoth(e => new Throwable(e.getMessage), r => r)
      .provide(dsLayer)

  override def delete(id: Long): Task[Unit] =
    ctx.run(phoneReckordSchema.filter(_.id == lift(id)).delete)
      .mapError(e => new Throwable(e.getMessage))
      .unit
      .provide(dsLayer)
}

object PhoneRecordRepository {
  val layer: TaskLayer[PhoneRecordRepository] =
    DB.layer >>> ZLayer.fromFunction[DataSource => PhoneRecordRepository](ds => new PhoneRecordRepositoryImpl(ds))
}

