import sbt._
import sbt.Keys._
import sys.process._

object MillTests {
  def millCleanEnv(
      logger: Logger,
      cwd: sbt.File
  ) = {
    val clean = Process.apply(Seq("rm", "-rf", "out"), cwd)
    if (clean.!(logger) != 0) {
      sys.error(
        "Failed to clean mill-codegen-plugin-tests environment , exiting.."
      )
    }
  }
  def millVerify(
      logger: Logger,
      cwd: sbt.File,
      version: String
  ) = {
    val proc = Process.apply(
      Seq("mill", "-i", "verify"),
      cwd,
      "PLUGIN_TEST_VERSION" -> version
    )
    val exitCode = proc.!(logger)
    if (exitCode != 0) {
      sys.error("mill test failed")
    }
  }
}
