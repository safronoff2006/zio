package ru.lesson.http

import io.getquill.{EntityQuery, PostgresZioJdbcContext, Quoted, SnakeCase}
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

  val dsLayer: ULayer[DataSource] = ZLayer.succeed(ds)

  val phoneReckordSchema: Quoted[EntityQuery[PhoneRecord]] = quote {
    query[PhoneRecord]
  }

  override def list(): Task[List[PhoneRecord]] =
    ZIO.logSpan("list") {
      ZIO.logInfo("list method") *> ctx.run(phoneReckordSchema)
        .mapError(e => new Throwable(e.getMessage))
    }
      .provide(dsLayer)

  override def getBy(id: Long): Task[Option[PhoneRecord]] =
    ZIO.logSpan("get-by") {
        ZIO.logInfo("getBy method") *> ctx.run(phoneReckordSchema.filter(_.id == lift(id)))
          .mapBoth(e => new Throwable(e.getMessage), _.headOption)
      }
      .provide(dsLayer)

  override def insert(phoneRecord: PhoneRecord): Task[PhoneRecord] =
    ZIO.logSpan("insert") {
        ZIO.logInfo("insert method") *> ctx.run(phoneReckordSchema.insertValue(lift(phoneRecord)))
          .mapBoth(e => new Throwable(e.getMessage), _ => phoneRecord)
      }
      .provide(dsLayer)

  override def update(phoneRecord: PhoneRecord): Task[PhoneRecord] =
    ZIO.logSpan("update") {
        ZIO.logInfo("update method") *> ctx.run(phoneReckordSchema.filter(_.id == lift(phoneRecord.id))
            .updateValue(lift(phoneRecord))
            .returning(v => v)
          )
          .mapBoth(e => new Throwable("Херня! "+ e.getMessage), r => r)
      }
      .provide(dsLayer)

  override def delete(id: Long): Task[Unit] =
    ZIO.logSpan("delete") {
        ZIO.logInfo("delete method") *> ctx.run(phoneReckordSchema.filter(_.id == lift(id)).delete)
          .mapError(e => new Throwable(e.getMessage))
         .unit
      }
      .provide(dsLayer)
}

object PhoneRecordRepository {
  val layer: TaskLayer[PhoneRecordRepository] =
    DB.layer >>> ZLayer.fromFunction[DataSource => PhoneRecordRepository](ds => new PhoneRecordRepositoryImpl(ds))
}

