package smithy4s.compliancetests.internals

private[compliancetests] sealed trait ShouldRun

object ShouldRun {
  case object Yes extends ShouldRun
  case object No extends ShouldRun
  case object NotSure extends ShouldRun
}
