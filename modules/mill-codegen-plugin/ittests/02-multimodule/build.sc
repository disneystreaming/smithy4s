import mill._, scalalib._
val localVersion = sys.env("PLUGIN_TEST_VERSION")

interp.load.ivy(
  "com.disneystreaming.smithy4s" %% "smithy4s-mill-codegen-plugin" % localVersion
)

// @required for Multi stage scripts

@
import smithy4s.codegen.mill.Smithy4sModule

def verify = T {
  foo.compile()
  bar.compile()
  foo.testCodegen()
  bar.testCodegen()
}

object foo extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.8"

  def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:$localVersion"
  )

  def testCodegen = T {
    checkFileExist(
      os.pwd / "out" / "foo" / "smithy4sOutputDir.dest" / "scala" / "foo" / "Foo.scala",
      shouldExist = true
    )
  }
}

object bar extends ScalaModule with Smithy4sModule {
  def moduleDeps = Seq(foo)

  def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:$localVersion"
  )

  def scalaVersion = "2.13.8"

  def testCodegen = T {
    checkFileExist(
      os.pwd / "out" / "bar" / "smithy4sOutputDir.dest" / "scala" / "foo" / "Foo.scala",
      shouldExist = false
    )

    checkFileExist(
      os.pwd / "out" / "bar" / "smithy4sOutputDir.dest" / "scala" / "bar" / "Bar.scala",
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
