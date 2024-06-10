package smithy4s.codegen

import sbt._
import sbt.util.CacheStore
import sbt.util.Logger
import sjsonnew._
import sjsonnew.support.scalajson.unsafe.Converter
import sjsonnew.support.scalajson.unsafe.PrettyPrinter

import scala.util.Try

private[codegen] object CachedTask {

  // This implementation is inspired by sbt.util.Tracked.inputChanged
  // The main difference is that when the values don't match, the difference is calculated
  // using munit-diff and recorded to debug log
  def inputChanged[I: JsonFormat, O](store: CacheStore, logger: Logger)(
      f: (Boolean, I) => O
  ): I => O = { in =>
    def debug(str: String): Unit = logger.debug(s"[smithy4s]$str")

    val previousValue = Try(store.read[I]()).toOption

    previousValue match {
      case None =>
        debug(
          "Could not read previous value from inputs, smithy4s codegen needs to be executed."
        )
        store.write[I](in)
        f(true, in)

      case Some(oldValue) =>
        (serializeCodegenArgs(oldValue), serializeCodegenArgs(in)) match {
          case (Some(oldArgs), Some(newArgs)) if (oldArgs != newArgs) =>
            val diff = new munit.diff.Diff(oldArgs, newArgs)
            val report = diff.createReport(
              "Arguments changed between smithy4s codegen invocations, diff:",
              printObtainedAsStripMargin = false
            )
            debug(report)
            store.write[I](in)
            f(true, in)
          case (_, _) =>
            debug("Input didn't change between codegen invocations")
            f(false, in)
        }
    }

  }

  private def serializeCodegenArgs[I: JsonFormat](args: I): Option[String] =
    Converter
      .toJson(args)
      .map(v => PrettyPrinter(v))
      .toOption
}
