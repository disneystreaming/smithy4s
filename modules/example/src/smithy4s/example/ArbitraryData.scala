package smithy4s.example

import smithy4s.Document
import smithy4s.Newtype
import smithy4s.syntax._

object ArbitraryData extends Newtype[Document] {
  object T {
    val hints : smithy4s.Hints = smithy4s.Hints(
      smithy.api.Trait(None, None, None),
    )
    val schema : smithy4s.Schema[Document] = document.withHints(hints)
    implicit val staticSchema : schematic.Static[smithy4s.Schema[Document]] = schematic.Static(schema)
  }
  def namespace = NAMESPACE
  val name = "ArbitraryData"
  val hints: smithy4s.Hints = T.hints
  val schema : smithy4s.Schema[ArbitraryData] = bijection(T.schema, ArbitraryData(_), (_ : ArbitraryData).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[ArbitraryData]] = schematic.Static(schema)
}