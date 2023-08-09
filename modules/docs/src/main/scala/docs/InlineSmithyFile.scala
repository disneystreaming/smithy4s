package docs

object InlineSmithyFile {

  def apply(fileName: String) = {
    val workspace = os.pwd
    val smithyFile =
      workspace / "modules" / "docs" / "src" / "main" / "smithy" / fileName
    val contents = os.read(smithyFile).trim()
    val snippet = List("```smithy", contents, "```").mkString("\n")
    println(snippet)
  }

  def fromSample(fileName: String) = {
    val workspace = os.pwd
    val smithyFile =
      workspace / "sampleSpecs" / fileName
    val contents = os.read(smithyFile).trim()
    val snippet = List("```smithy", contents, "```").mkString("\n")
    println(snippet)
  }

}
