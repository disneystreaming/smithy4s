package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example._package.MyPackageString
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class TestReservedNamespaceImport(_package: Option[MyPackageString] = None)
object TestReservedNamespaceImport extends ShapeTag.Companion[TestReservedNamespaceImport] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "TestReservedNamespaceImport")

  val hints: Hints = Hints.empty

  object Lenses {
    val _package = Lens[TestReservedNamespaceImport, Option[MyPackageString]](_._package)(n => a => a.copy(_package = n))
  }

  implicit val schema: Schema[TestReservedNamespaceImport] = struct(
    MyPackageString.schema.optional[TestReservedNamespaceImport]("package", _._package),
  ){
    TestReservedNamespaceImport.apply
  }.withId(id).addHints(hints)
}
