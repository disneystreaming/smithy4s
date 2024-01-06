package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.example._package.MyPackageString

final case class TestReservedNamespaceImport(_package: Option[MyPackageString] = None)

object TestReservedNamespaceImport extends ShapeTag.Companion[TestReservedNamespaceImport] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "TestReservedNamespaceImport")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestReservedNamespaceImport] = struct(
    MyPackageString.schema.optional[TestReservedNamespaceImport]("package", _._package),
  ){
    TestReservedNamespaceImport.apply
  }.withId(id).addHints(hints)
}
