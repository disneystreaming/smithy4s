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
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
  )

// Module B references `com.example.first#MyString` from its own
// `com.example.second` shapes. It must NOT regenerate `com.example.first`, and
// the Scala code it generates for `Second` must import the upstream-rendered
// `gen.com.example.first.MyString` (otherwise compilation would fail).
lazy val b = (project in file("b"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .dependsOn(a)
