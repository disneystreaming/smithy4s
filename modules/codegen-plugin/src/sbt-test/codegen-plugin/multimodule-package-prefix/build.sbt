ThisBuild / scalaVersion := "2.13.18"

// Module A generates code for `com.example.first` under a custom package prefix
// (`gen`). It carries a smithy4sGenerated manifest entry listing
// the namespace as already-generated AND its `renderedPackages` mapping
// (com.example.first -> gen.com.example.first), so downstream modules can both
// skip regenerating that namespace and resolve cross-namespace references to
// the right Scala package.
lazy val a = (project in file("a"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    TaskKey[Unit]("checkLayout") := {
      val src = (Compile / sourceManaged).value / "smithy4s"
      val prefixed = src / "gen" / "com" / "example" / "first" / "MyString.scala"
      val unprefixed = src / "com" / "example" / "first" / "MyString.scala"
      assert(
        prefixed.exists,
        s"Expected $prefixed to exist"
      )
      assert(
        !unprefixed.exists,
        s"Expected $unprefixed to be absent"
      )
    }
  )

// Module B references `com.example.first#MyString` from its own
// `com.example.second` shapes. It must NOT regenerate `com.example.first`, and
// the Scala code it generates for `Second` must import the upstream-rendered
// `gen.com.example.first.MyString` (otherwise compilation would fail).
lazy val b = (project in file("b"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .dependsOn(a)
  .settings(
    TaskKey[Unit]("checkLayout") := {
      val src = (Compile / sourceManaged).value / "smithy4s"
      val ownNs = src / "com" / "example" / "second" / "Second.scala"
      val upstreamUnprefixed =
        src / "com" / "example" / "first" / "MyString.scala"
      val upstreamPrefixed =
        src / "gen" / "com" / "example" / "first" / "MyString.scala"
      assert(
        ownNs.exists,
        s"Expected $ownNs to exist"
      )
      assert(
        !upstreamUnprefixed.exists,
        s"Expected $upstreamUnprefixed to be absent (upstream namespace must not be regenerated)"
      )
      assert(
        !upstreamPrefixed.exists,
        s"Expected $upstreamPrefixed to be absent (upstream namespace must not be regenerated)"
      )
    }
  )
