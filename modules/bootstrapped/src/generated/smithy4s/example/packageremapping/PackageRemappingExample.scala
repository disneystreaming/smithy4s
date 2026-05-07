package smithy4s.example.packageremapping

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example.packageremapping.nested.NestedString
import smithy4s.schema.Schema.struct

final case class PackageRemappingExample(value: NestedString)

object PackageRemappingExample extends ShapeTag.Companion[PackageRemappingExample] {
  val id: ShapeId = ShapeId("pkg.remapping", "PackageRemappingExample")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(value: NestedString): PackageRemappingExample = PackageRemappingExample(value)

  implicit val schema: Schema[PackageRemappingExample] = struct[PackageRemappingExample](
    NestedString.schema.required[PackageRemappingExample]("value", _.value),
  )(make).withId(id).addHints(hints)
}
