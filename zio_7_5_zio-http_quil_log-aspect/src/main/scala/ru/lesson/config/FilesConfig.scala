package ru.lesson.config

import zio._
import zio.config.magnolia.deriveConfig

case class FilesConfig(videoPath: String, videoName: String )

object FilesConfig {
  val config: Config[FilesConfig] = deriveConfig[FilesConfig].nested("FilesConfig")

  val config_manual: Config[FilesConfig] = (Config.string.nested("videoPath") ++ Config.string.nested("videoName"))
    .map { case (path, name) =>
      FilesConfig(path, name)
    }.nested("FilesConfig")

  def filesConfig(): ZLayer[Any, Config.Error, FilesConfig] = ZLayer.fromZIO(
    ZIO.config[FilesConfig](FilesConfig.config)
  )

}
