import java.io.Writer

object NativeImageJCGAdapter extends JavaTestAdapter {

    // Implementation
    val frameworkName: String = "NativeImage"

    val possibleAlgorithms: Array[String] = Array("HelloWorld") // points-to, RTA

    def serializeCG(
        algorithm: String,
        inputDirPath: String,
        output:         Writer,
        adapterOptions: AdapterOptions
    ): Long = {
        val startTime = System.nanoTime()
        
        output.write(s"Running $algorithm algorithm\n")
        output.write(s"Input: $inputDirPath\n")
        //output.write(s"JVM Args: ${jvm_args.mkString(" ")}\n")
        //output.write(s"Program Args: ${program_args.mkString(" ")}\n")
        output.close()

        System.nanoTime() - startTime
    }
}
