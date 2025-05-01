import java.nio.file.{Files, Path, Paths}

object GraalPathResolver {

  def resolveGraalPaths(): (Path, Path) = {
    val graalHome = sys.env.get("GRAAL_HOME") match {
      case Some(path) => path
      case None =>
        println("[ERROR] GRAAL_HOME environment variable is not set.")
        sys.exit(1)
    }

    val graalBin = Paths.get(graalHome, "bin")
    val graalJava = graalBin.resolve("java")
    val nativeImage = graalBin.resolve("native-image")

    if (!Files.isExecutable(graalJava)) {
      println(s"[ERROR] Graal Java executable not found or not executable at: $graalJava")
      sys.exit(1)
    }

    if (!Files.isExecutable(nativeImage)) {
      println(s"[ERROR] Native Image executable not found or not executable at: $nativeImage")
      sys.exit(1)
    }

    (graalJava, nativeImage)
  }
}