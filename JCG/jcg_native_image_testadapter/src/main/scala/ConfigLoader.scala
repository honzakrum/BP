import java.nio.file.{Paths, Path, Files}
import java.nio.charset.StandardCharsets
import play.api.libs.json._
import scala.util.{Try, Using}

case class PathsConfig(graalJava: String, nativeImage: String)
case class Config(paths: PathsConfig)

object ConfigLoader {
  private val CONFIG_PATH = Paths.get("./NativeImage_config.json")

  implicit val pathsConfigReads: Reads[PathsConfig] = Json.reads[PathsConfig]
  implicit val configReads: Reads[Config] = Json.reads[Config]

  def loadConfig(): Either[String, Config] = {
    if (!Files.exists(CONFIG_PATH)) {
      Left(s"[ERROR] Config file not found at ${CONFIG_PATH.toAbsolutePath}")
    } else {
      val jsonString = Try {
        new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8)
      }.fold(
        error => return Left(s"[ERROR] Failed to read config file: ${error.getMessage}"),
        identity
      )

      Try(Json.parse(jsonString).as[Config])
        .fold(
          error => Left(s"[ERROR] Failed to parse config: ${error.getMessage}"),
          Right(_)
        )
    }
  }
}