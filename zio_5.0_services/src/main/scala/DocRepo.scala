
import zio._

// 1.Service definition
case class Doc(
                title: String,
                description: String,
                language: String,
                format: String,
                content: Array[Byte]
              )


trait DocRepo {
  def get(id: String): ZIO[Any, Throwable, Doc]

  def save(document: Doc): ZIO[Any, Throwable, String]

  def delete(id: String): ZIO[Any, Throwable, Unit]

  def findByTitle(title: String): ZIO[Any, Throwable, List[Doc]]
}

// 2.Service Implementation
case class DocRepoImpl(
                        metadataRepo: MetadataRepo,
                        blobStorage: BlobStorage
                      ) extends DocRepo {

  override def get(id: String): ZIO[Any, Throwable, Doc] =
    for {
      metadata <- metadataRepo.get(id)
      content <- blobStorage.get(id)
    } yield Doc(
      metadata.title,
      metadata.description,
      metadata.language,
      metadata.format,
      content
    )

  override def save(document: Doc): ZIO[Any, Throwable, String] =
    for {
      id <- blobStorage.put(document.content)
      _ <- metadataRepo.put(
        id,
        Metadata(
          document.title,
          document.description,
          document.language,
          document.format
        )
      )
    } yield id

  override def delete(id: String): ZIO[Any, Throwable, Unit] =
    for {
      _ <- blobStorage.delete(id)
      _ <- metadataRepo.delete(id)
    } yield ()

  override def findByTitle(title: String): ZIO[Any, Throwable, List[Doc]] =
    for {
      map <- metadataRepo.findByTitle(title)
      content <- ZIO.foreach(map)((id, metadata) =>
        for {
          content <- blobStorage.get(id)
        } yield id -> Doc(
          metadata.title,
          metadata.description,
          metadata.language,
          metadata.format,
          content
        )

      )
    } yield content.values.toList
}

// 4. Zlayer  constructor

object DocRepoImpl {
  val layer: ZLayer[BlobStorage with MetadataRepo, Nothing, DocRepo] =
    ZLayer {
      for {
        metadataRepo <- ZIO.service[MetadataRepo]
        blobStorage  <- ZIO.service[BlobStorage]
      } yield  DocRepoImpl(metadataRepo, blobStorage)
    }
}

// 5. Accessor Methods

object DocRepo {
  def get(id:String): ZIO[DocRepo, Throwable, Doc] = ZIO.serviceWithZIO[DocRepo](_.get(id))

  def save(document: Doc): ZIO[DocRepo, Throwable, String] = ZIO.serviceWithZIO[DocRepo](_.save(document))

  def delete(id: String): ZIO[DocRepo, Throwable, Unit] =  ZIO.serviceWithZIO[DocRepo](_.delete(id))

  def findByTitle(title: String): ZIO[DocRepo, Throwable, List[Doc]] = ZIO.serviceWithZIO[DocRepo](_.findByTitle(title))
}
