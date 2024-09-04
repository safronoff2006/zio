import zio.Console._
import zio._

import scala.language.postfixOps

object MyApp_Services extends ZIOAppDefault {


  private val app: ZIO[DocRepo, Throwable, Unit] = for {

   id1 <- DocRepo.save(
     Doc(
       "title",
       "description",
       "en",
       "text/plain",
       "content".getBytes()
     )
   )

   doc1 <- DocRepo.get(id1)
   _ <- printLine(
     s"""
        |Downloaded the document with $id1 id:
        |  title: ${doc1.title}
        |  description: ${doc1.description}
        |  language: ${doc1.language}
        |  format: ${doc1.format}
        |""".stripMargin
   )




   _ <- printLine(s"----------------------------")
   ///////////////////////


   id2 <- DocRepo.save(
     Doc(
       "title",
       "description",
       "en",
       "text/plain",
       "content".getBytes()
     )
   )

   doc2 <- DocRepo.get(id2)
   _ <- printLine(
     s"""
        |Downloaded the document with $id2 id:
        |  title: ${doc2.title}
        |  description: ${doc2.description}
        |  language: ${doc2.language}
        |  format: ${doc2.format}
        |""".stripMargin
   )

   _ <- DocRepo.delete(id1)
   _ <- printLine(s"Deleted the document with $id1 id")
   _ <- DocRepo.delete(id2)
   _ <- printLine(s"Deleted the document with $id2 id")

  } yield ()

  def run: ZIO[Any, Throwable, Unit] = app.provide(
    DocRepoImpl.layer,
    MetadataRepo.layer,
    BlobStorage.layer
  )


}