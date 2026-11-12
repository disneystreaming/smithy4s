import sbt._

object BuildCompat {
  // sbt 1 has no native cache; sbt 2 uses its real Def.uncached member.
  implicit class DefOps(private val singleton: sbt.Def.type) extends AnyVal {
    def uncached[A](value: A): A = value
  }
}
