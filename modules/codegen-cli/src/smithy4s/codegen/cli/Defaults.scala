package smithy4s.codegen.cli

import smithy4s.codegen.BuildInfo._

private[cli] object Defaults{
  val defaultDependencies : List[String] = List(
    s"$alloyOrg:alloy-core:$alloyVersion"
  )
}
