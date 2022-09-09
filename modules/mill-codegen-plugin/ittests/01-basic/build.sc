import mill._, scalalib._
val localVersion = sys.env("PLUGIN_TEST_VERSION")

interp.load.ivy(
  "com.disneystreaming.smithy4s" %% "smithy4s-mill-codegen-plugin" % localVersion
)

// @required for Multi stage scripts

@
import smithy4s.codegen.mill.Smithy4sModule

def verify = T {
  test.compile()
  test.testCodegen()
}

object test extends ScalaModule with Smithy4sModule {
  def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:$localVersion"
  )

  def scalaVersion = "2.13.8"

  def testCodegen = T {
    checkFileExist(
      os.pwd / "out" / "test" / "smithy4sOutputDir.dest" / "scala" / "basic" / "MyNewString.scala",
      shouldExist = true
    )
  }
}

def checkFileExist(path: os.Path, shouldExist: Boolean) = {
  if (!os.exists(path) && shouldExist) {
    sys.error(s"${path} file not found")
  }
  if (os.exists(path) && !shouldExist) {
    sys.error(s"${path} file should not exist")
  }
}
