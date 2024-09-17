package ru.lesson.config

import zio._
import zio.config.magnolia.deriveConfig

case class FilesConfig(videoPath: String ="", videoName: String = "", bookPath: String = "" , resultName: String = "")

object FilesConfig {
  val config: Config[FilesConfig] = deriveConfig[FilesConfig].nested("FilesConfig")

  val config_manual: Config[FilesConfig] = (
      Config.string.nested("videoPath") ++
      Config.string.nested("videoName") ++
        Config.string.nested("bookPath") ++
        Config.string.nested("resultName")
    )
    .map { case (path, name, bp, rn) =>
      FilesConfig(path, name, bp, rn)
    }.nested("FilesConfig")

  val  layer: ZLayer[Any, Config.Error, FilesConfig] = ZLayer.fromZIO(
    ZIO.config[FilesConfig](FilesConfig.config)
  )

}
