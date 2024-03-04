package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example._package.MyPackageString
import smithy4s.schema.Schema.struct

final case class TestReservedNamespaceImport(_package: Option[MyPackageString] = None)

object TestReservedNamespaceImport extends ShapeTag.Companion[TestReservedNamespaceImport] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "TestReservedNamespaceImport")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(_package: Option[MyPackageString]): TestReservedNamespaceImport = TestReservedNamespaceImport(_package)

  implicit val schema: Schema[TestReservedNamespaceImport] = struct(
    MyPackageString.schema.optional[TestReservedNamespaceImport]("package", _._package),
  )(make).withId(id).addHints(hints)
}
