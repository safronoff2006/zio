import zio._
import zio.macros.accessible

import java.util.UUID

// 3. Service Dependencies
// Metadata Service

case class Metadata(
                     title: String,
                     description: String,
                     language: String,
                     format: String
                   )
@accessible[MetadataRepo]
trait MetadataRepo {
  def get(id: String): Task[Metadata]
  def put(id: String, metadata: Metadata): Task[Unit]
  def delete(id: String): Task[Unit]
  def findByTitle(title: String): Task[Map[String, Metadata]]
}

@accessible[BlobStorage]
trait BlobStorage {
  def get(id: String): Task[Array[Byte]]
  def put(content: Array[Byte]): Task[String]
  def delete(id: String): Task[Unit]
}

class MetadataRepoImpl extends MetadataRepo {
  val metadataMap = scala.collection.mutable.Map[String, Metadata]()

  override def get(id: String): Task[Metadata] =
    ZIO.fromOption(metadataMap.get(id)).orElseFail(new Throwable("Metadata not found"))

  override def put(id: String, metadata: Metadata): Task[Unit] =
    ZIO.from(metadataMap += id -> metadata).unit


  override def delete(id: String): Task[Unit] =
    ZIO.fromOption(metadataMap.remove(id)).orElseFail(new Throwable("Error delete metadata")).unit

  override def findByTitle(title: String): Task[Map[String, Metadata]] =
    ZIO.from(metadataMap.filter(_._2.title == title).toMap)
}

class BlobStorageImpl extends BlobStorage {
  val storageMap = scala.collection.mutable.Map[String,  Array[Byte]]()

  override def get(id: String): Task[Array[Byte]] =
    ZIO.fromOption(storageMap.get(id)).orElseFail(new Throwable("Content not found"))

  override def put(content: Array[Byte]): Task[String] = for {
   id <- ZIO.succeed(UUID.randomUUID().toString)
    _ <- Console.printLine(s"id = $id")
    _ <- ZIO.from(storageMap += id -> content)
  } yield  id

  override def delete(id: String): Task[Unit] =
   ZIO.from( storageMap.remove(id) ).orElseFail(new Throwable("Error delete Content")).unit
}

object MetadataRepo {
  val layer: ULayer[MetadataRepoImpl] = ZLayer.succeed(new MetadataRepoImpl)
}

object BlobStorage {
  val layer: ULayer[BlobStorageImpl] = ZLayer.succeed(new BlobStorageImpl)
}