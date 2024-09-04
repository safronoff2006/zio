import zio.Console._
import zio._

import scala.language.postfixOps

object MyApp_Services extends ZIOAppDefault {


  private val app: ZIO[DocRepo, Throwable, Unit] = for {

    id <- DocRepo.save(
     Doc(
       "title",
       "description",
       "en",
       "text/plain",
       "content".getBytes()
     )
   )

    doc <- DocRepo.get(id)
    _ <- printLine(
     s"""
        |Downloaded the document with $id id:
        |  title: ${doc.title}
        |  description: ${doc.description}
        |  language: ${doc.language}
        |  format: ${doc.format}
        |""".stripMargin
   )



    _ <- DocRepo.delete(id)
    _ <- printLine(s"Deleted the document with $id id")


  } yield ()

  def run: ZIO[Any, Throwable, Unit] = app.provide(
    DocRepoImpl.layer,
    MetadataRepo.layer,
    BlobStorage.layer
  )


}