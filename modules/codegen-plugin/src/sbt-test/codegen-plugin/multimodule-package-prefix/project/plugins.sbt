sys.props.get("plugin.version") match {
  case Some(x) =>
    addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % x)
  case _ =>
    sys.error(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
}

// When running under sbt 2 (Scala 3), exclude Scala 2.13 variants that
// conflict with Scala 3 variants brought in transitively by coursier.
excludeDependencies ++= {
  if (scalaBinaryVersion.value == "3")
    Seq(
      ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
      ExclusionRule("org.scala-lang.modules", "scala-xml_2.13")
    )
  else Seq.empty
}
