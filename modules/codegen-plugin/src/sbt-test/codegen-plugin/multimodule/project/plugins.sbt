sys.props.get("plugin.version") match {
  case Some(x) =>
    addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % x)
  case _ =>
        addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "dev")

    // sys.error(
    //   """|The system property 'plugin.version' is not defined.
    //      |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    // )
}
