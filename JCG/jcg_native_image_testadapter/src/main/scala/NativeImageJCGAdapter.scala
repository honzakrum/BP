import org.apache.commons.io.FileUtils
import java.io.{File, PrintWriter, Writer}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import scala.sys.process._
import scala.util.{Failure, Success, Try}
// csv format
import play.api.libs.json.{Format, Json}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import scala.jdk.CollectionConverters._

object NativeImageJCGAdapter extends JavaTestAdapter {

    val frameworkName: String = "NativeImage"

    val possibleAlgorithms: Array[String] = Array("PTA") // points-to

    def serializeCG(
                     algorithm:      String,
                     inputDirPath:   String,
                     output:         Writer,
                     adapterOptions: AdapterOptions
                   ): Long = {
        val startTime = System.nanoTime()

        // Get bin paths of Graal and Native Image
        val (graalJavaPath, nativeImagePath) = ConfigLoader.loadConfig() match {
            case Right(cfg) =>
                (Paths.get(cfg.paths.graalJava), Paths.get(cfg.paths.nativeImage))
            case Left(err) => throw new RuntimeException(err)
        }
        // Directory for native image reachability metadata
        val configDirectory = Paths.get("./config")
        // Current testcase
        val jarPath = Paths.get(inputDirPath)
        val jarFileName = jarPath.getFileName.toString
        val configOutputDir = configDirectory.resolve(jarFileName.stripSuffix(".jar"))

        //if(jarFileName == "CFNE1.jar" ) // just one
        //{
            try {
                // Create configuration files that could be necessary for reflection etc
                createConfig(jarPath, configOutputDir, graalJavaPath)

                // Generate call graph for current test case
                generateCallGraph(jarPath, configDirectory, nativeImagePath)

                // Cleanup
                //cleanArtifact(jarFileName)

                // Serialization
                val json = serializeCallGraph(jarFileName)
                println("CG Serialized")

                output.write(json)
            } catch {
                case e: Exception =>
                    println(s"Unexpected error: ${e.getClass.getName} - ${e.getMessage}")
                    e.printStackTrace()
            } finally {
                output.close()
            }
        //}


        System.nanoTime() - startTime
    }

    /**
     * Generates configuration files using the native-image-agent to their respective folder.
     *
     * @param jarFile         The path to the JAR file.
     * @param configOutputDir The directory to store the configuration files.
     * @param graalJavaPath The path to Native Image executable.
     */
    def createConfig(jarFile: Path, configOutputDir: Path, graalJavaPath: Path): Unit = {
        println(s"Creating directory ${configOutputDir.toString}")
        Files.createDirectories(configOutputDir)
        val agentCommand = Seq(
            graalJavaPath.toString,
            s"-agentlib:native-image-agent=config-output-dir=${configOutputDir.toString}",
            "-jar",
            jarFile.toString
        )

        println(s"Running agent command: ${agentCommand.mkString(" ")}")
        val agentResult = Try(agentCommand.!!)
        agentResult match {
            case Success(_) => println(s"Configuration generated for ${jarFile.getFileName}")
            case Failure(e) => println(s"Failed to generate configuration for ${jarFile.getFileName}: ${e.getMessage}")
        }
    }

    /**
     * Generates the call graph using the native-image tool.
     *
     * @param jarFile         The path to the JAR file.
     * @param configOutputDir The directory containing the configuration files.
     * @param nativeImagePath The path to Native Image executable.
     */
    def generateCallGraph(jarFile: Path, configOutputDir: Path, nativeImagePath : Path): Unit = {

        val nativeImageCommand = Seq(
            nativeImagePath.toString,
            "-H:+UnlockExperimentalVMOptions",
            "-H:+ReturnAfterAnalysis",
            "-H:PrintAnalysisCallTreeType=CSV",
            s"-H:ConfigurationFileDirectories=${configOutputDir.toString}",
            "-jar",
            jarFile.toString
        )

        // Generating call graphs
        println(s"Running native-image command: ${nativeImageCommand.mkString(" ")}")
        val nativeImageResult = Try(nativeImageCommand.!!)
        nativeImageResult match {
            case Success(output) =>
                println(s"Call graph generated for ${jarFile.getFileName}:\n$output")
            case Failure(e) => println(s"Failed to generate call graph for ${jarFile.getFileName}: ${e.getMessage}")
        }

        // Moving them to their own folder for each test case
        val callGraphDir = Paths.get("./CallGraphs").resolve(jarFile.getFileName.toString.stripSuffix(".jar"))
        // Delete if exists (recursively)
        if (Files.exists(callGraphDir)) {
            FileUtils.deleteDirectory(callGraphDir.toFile)
        }
        Files.createDirectories(callGraphDir)
        val reportsFolder = Paths.get("./reports")
        if (Files.exists(reportsFolder)) {
            // Move the folder
            Files.move(reportsFolder, callGraphDir, StandardCopyOption.REPLACE_EXISTING)
            println(s"Moved folder from $reportsFolder to $callGraphDir")
        } else {
            println(s"Source folder $reportsFolder does not exist.")
        }
    }

    /**
     * Deletes the executable artifact generated by the native-image tool.
     *
     * @param jarFileName The name of the JAR file (used to derive the executable name).
     */
    def cleanArtifact(jarFileName: String): Unit = {
        val executableName = jarFileName.stripSuffix(".jar") // Name of the executable
        val executablePath = Paths.get(executableName) // Path to the executable
        if (Files.exists(executablePath)) {
            println(s"Deleting executable artifact: $executableName")
            Files.delete(executablePath) // Delete the executable
        }
    }

    /**
     * Parses CSV file into a list.
     *
     * @param filePath The name of the .csv file.
     */
    def readCsv(filePath: String): List[Map[String, String]] = {
        val reader = Files.newBufferedReader(Paths.get(filePath))
        val csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())
        csvParser.asScala.map(_.toMap.asScala.toMap).toList
    }

    /**
     * Maps CSV list of methods into individual methods.
     *
     * @param csvData Methods data from the generated csv.
     */
    def parseMethods(csvData: List[Map[String, String]]): List[NativeImageMethod] = {
        csvData.map { row =>
            NativeImageMethod(
                id = row("Id").toInt,
                name = row("Name"),
                declaringClass = row("Type"),
                parameterTypes = row("Parameters"),
                returnType = row("Return"),
                display = row("Display"),
                flags = row("Flags"),
                isEntryPoint = row("IsEntryPoint").toBoolean
            )
        }
    }

    /**
     * Maps CSV list of invokes into individual invokes.
     *
     * @param csvData Invokes data from the generated csv.
     */
    def parseInvocations(csvData: List[Map[String, String]]): List[NativeImageInvocation] = {
        csvData.map { row =>
            NativeImageInvocation(
                id = row("Id").toInt,
                methodId = row("MethodId").toInt,
                bytecodeIndexes = row("BytecodeIndexes"),
                targetId = row("TargetId").toInt,
                isDirect = row("IsDirect").toBoolean
            )
        }
    }

    /**
     * Maps CSV list of targets into individual targets.
     * Targets are used for connecting methods with invokes.
     *
     * @param csvData Targets data from the generated csv.
     */
    def parseTargets(csvData: List[Map[String, String]]): List[NativeImageTarget] = {
        csvData.map { row =>
            NativeImageTarget(
                invokeId = row("InvokeId").toInt,
                targetId = row("TargetId").toInt
            )
        }
    }

    /**
     * Builds call graph using the csv data.
     *
     * @param methods Methods data from the generated csv.
     * @param invocations Invokes data from the generated csv.
     * @param targets Targets data from the generated csv.
     */
    def buildCallGraph(
                        methods: List[NativeImageMethod],
                        invocations: List[NativeImageInvocation],
                        targets: List[NativeImageTarget]
                      ): ReachableMethods = {
        val methodMap = methods.map(m => m.id -> m).toMap

        // Build invocation to targets mapping
        val invocationTargets = targets.groupBy(_.invokeId).mapValues(_.map(_.targetId))

        val reachableMethods = methods.map { method =>
            val methodSignature = Method(
                name = method.name,
                declaringClass = toJvmTypeDescriptor(method.declaringClass),
                returnType = toJvmTypeDescriptor(method.returnType),
                parameterTypes = parseParameterTypes(method.parameterTypes)
            )

            val callSites = invocations
              .filter(_.methodId == method.id)
              .map { invocation =>
                  // Get ALL targets for this invocation
                  val targetIds = invocationTargets.getOrElse(invocation.id, List(invocation.targetId))
                  val targetSignatures = targetIds.map { targetId =>
                      val targetMethod = methodMap(targetId)
                      Method(
                          name = targetMethod.name,
                          declaringClass = toJvmTypeDescriptor(targetMethod.declaringClass),
                          returnType = toJvmTypeDescriptor(targetMethod.returnType),
                          parameterTypes = parseParameterTypes(targetMethod.parameterTypes)
                      )
                  }.toSet

                  CallSite(
                      declaredTarget = targetSignatures.head, // is this right?
                      line = -1,
                      pc = None,
                      targets = targetSignatures
                  )
              }.toSet

            ReachableMethod(methodSignature, callSites)
        }.toSet

        ReachableMethods(reachableMethods)
    }

    /**
     * Transforms params string to Jvm notation.
     *
     * @param paramTypes Params to be transformed.
     */
    def parseParameterTypes(paramTypes: String): List[String] = {
        if (paramTypes.trim.isEmpty || paramTypes == "Lempty;") {
            List.empty
        } else {
            paramTypes.split(" ")
              .map(toJvmTypeDescriptor)
              .filter(_.nonEmpty)
              .toList
        }
    }

    /**
     * Transforms class name to Jvm notation.
     *
     * @param className Name of the class to be transformed.
     */
    def toJvmTypeDescriptor(className: String): String = {
        if (className == "empty" || className == "Lempty;") {
            return ""
        }

        className match {
            case "void"    => "V"
            case "int"     => "I"
            case "boolean" => "Z"
            case "byte"    => "B"
            case "char"    => "C"
            case "short"   => "S"
            case "long"    => "J"
            case "float"   => "F"
            case "double"  => "D"
            case s if s.endsWith("[]") =>
                "[" + toJvmTypeDescriptor(s.substring(0, s.length - 2))
            case _ =>
                if (className.startsWith("L") && className.endsWith(";")) {
                    className
                } else if (className.contains(".")) {
                    "L" + className.replace('.', '/') + ";"
                } else {
                    "L" + className + ";"
                }
        }
    }

    /**
     * Serializes call graph from CSV format generated by Native Image
     * to the json format that Evaluation accepts.
     *
     * @param jarFileName Testcase name.
     */
    def serializeCallGraph(jarFileName: String): String = {
        // Read CSV files
        val testFolderName = "./CallGraphs/" + jarFileName.stripSuffix(".jar")
        println(s"[INFO] Test folder name: $testFolderName")
        val methodsCsv = readCsv(s"$testFolderName/call_tree_methods.csv")
        val invocationsCsv = readCsv(s"$testFolderName/call_tree_invokes.csv")
        val targetsCsv = readCsv(s"$testFolderName/call_tree_targets.csv")

        // Parse CSV data
        val methods = parseMethods(methodsCsv)
        val invocations = parseInvocations(invocationsCsv)
        val targets = parseTargets(targetsCsv)

        // Build call graph
        val callGraph = buildCallGraph(methods, invocations, targets)

        // Serialize to JSON
        Json.prettyPrint(Json.toJson(callGraph))
    }
}

// NativeImage-specific case classes for CSV parsing
// TODO maybe move them to their own file
case class NativeImageMethod(
                              id: Int,
                              name: String,
                              declaringClass: String,
                              parameterTypes: String,
                              returnType: String,
                              display: String,
                              flags: String,
                              isEntryPoint: Boolean
                            )

case class NativeImageInvocation(
                                  id: Int,
                                  methodId: Int,
                                  bytecodeIndexes: String,
                                  targetId: Int,
                                  isDirect: Boolean
                                )

case class NativeImageTarget(
                              invokeId: Int,
                              targetId: Int
                            )