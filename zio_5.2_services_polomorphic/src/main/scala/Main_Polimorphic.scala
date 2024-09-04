import zio.Console._
import zio._
import zio.macros.accessibleMM

object Main_Polimorphic extends ZIOAppDefault {

  private val app = for {
    _ <- KeyValueStore.set[String, Int, String]("key1", 3).debug
    message <- KeyValueStore.get[String, Int, String]("key1").map(key => s"Получен ключ $key")
   _ <- KeyValueStore.remove[String, Int, String]("key1")
    _ <- KeyValueStore.get[String, Int, String]("key1").either.debug
  } yield  message

  override def run: IO[String, String] = app.provide(InmemoryKeyValueStore.layer).debug

  @accessibleMM[IO]  // c accesible макросом
  trait KeyValueStore[K, V, E, F[_, _]] {
    def get(key: K): F[E, V]

    def set(key: K, value: V): F[E, V]

    def remove(key: K): F[E, Unit]
  }

// менне полиморфный вариант
//  object KeyValueStore {
//    def get[K: Tag, V: Tag, E: Tag](key: K): ZIO[KeyValueStore[K, V, E, IO], E, V] =
//      ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.get(key))
//
//    def set[K: Tag, V: Tag, E: Tag](key: K, value: V): ZIO[KeyValueStore[K, V, E, IO], E, V] =
//      ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.set(key, value))
//
//    def remove[K: Tag, V: Tag, E: Tag](key: K): ZIO[KeyValueStore[K, V, E, IO], E, Unit] =
//      ZIO.serviceWithZIO(_.remove(key))
//  }key

  //более полиморфный вариант
//  object KeyValueStore {
//    def get[K: Tag, V: Tag, E: Tag, F[_, _] : TagKK](key: K): ZIO[KeyValueStore[K, V, E, F], Nothing, F[E, V]] =
//      ZIO.serviceWith[KeyValueStore[K, V, E, F]](_.get(key))
//
//    def set[K: Tag, V: Tag, E: Tag, F[_, _] : TagKK](key: K, value: V): ZIO[KeyValueStore[K, V, E, F], Nothing, F[E, V]] =
//      ZIO.serviceWith[KeyValueStore[K, V, E, F]](_.set(key, value))
//
//    def remove[K: Tag, V: Tag, E: Tag, F[_, _] : TagKK](key: K): ZIO[KeyValueStore[K, V, E, F], E, Unit] =
//      ZIO.serviceWith(_.remove(key))
//  }

  case class InmemoryKeyValueStore(map: Ref[Map[String, Int]])
    extends KeyValueStore[String, Int, String, IO] {

    override def get(key: String): IO[String, Int] =
      map.get.map(_.get(key)).someOrFail(s"$key not found")

    override def set(key: String, value: Int): IO[String, Int] =
      map.update(_.updated(key, value)).map(_ => value)

    override def remove(key: String): IO[String, Unit] =
      map.update(_.removed(key))
  }

  object InmemoryKeyValueStore {
    def layer: ULayer[KeyValueStore[String, Int, String, IO]] =
      ZLayer {
        Ref.make(Map[String, Int]()).map(InmemoryKeyValueStore.apply)
      }
  }


}