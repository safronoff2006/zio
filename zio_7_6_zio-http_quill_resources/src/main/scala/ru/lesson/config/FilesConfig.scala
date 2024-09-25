package ru.lesson.config

import zio._
import zio.config.magnolia.deriveConfig

case class FilesConfig(
                        videoPath: String ="",
                        videoName: String = "",
                        bookPath: String = "" ,
                        resultName: String = "",
                        dictPath: String = "",
                        repeatBr: Int = 2,
                        concurrency: Int = 10,
                        buffer: Int = 10
                      )

object FilesConfig {
  val config: Config[FilesConfig] = deriveConfig[FilesConfig].nested("FilesConfig")

  val config_manual: Config[FilesConfig] = (
      Config.string.nested("videoPath") ++
      Config.string.nested("videoName") ++
        Config.string.nested("bookPath") ++
        Config.string.nested("resultName") ++
        Config.string.nested("dictPath") ++
        Config.int.nested("repeatBr") ++
        Config.int.nested("concurrency") ++
        Config.int.nested("buffer")
    )
    .map { case (path, name, bp, rn, dp, repeat, concurrency, buffer ) =>
      FilesConfig(path, name, bp, rn, dp, repeat, concurrency, buffer)
    }.nested("FilesConfig")

  val  layer: ZLayer[Any, Config.Error, FilesConfig] = ZLayer.fromZIO(
    ZIO.config[FilesConfig](FilesConfig.config)
  )

}
