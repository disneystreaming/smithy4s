package smithy4s
package scalacheck

import schematic.scalacheck.DynData
import org.scalacheck.Gen
import smithy4s.syntax._
import smithy.api.TimestampFormat

object SchemaGenerator {
  def genSchema(
      maxDepth: Int,
      maxWidth: Int
  ): Gen[Schema[DynData]] = {
    val generator =
      new schematic.scalacheck.SchemaGenerator[smithy4s.Schematic](maxWidth) {
        override def primitives: Vector[Schema[Any]] = Vector(
          schematic.boolean.Schema,
          schematic.byte.Schema,
          schematic.string.Schema,
          schematic.int.Schema,
          schematic.long.Schema,
          schematic.float.Schema,
          schematic.double.Schema,
          schematic.short.Schema,
          schematic.uuid.Schema,
          schematic.unit.Schema,
          smithy4s.Document.Schema,
          smithy4s.Timestamp.Schema,
          smithy4s.Timestamp.Schema.withHints(TimestampFormat.DATE_TIME),
          smithy4s.Timestamp.Schema.withHints(TimestampFormat.EPOCH_SECONDS),
          smithy4s.Timestamp.Schema.withHints(TimestampFormat.HTTP_DATE)
        ).asInstanceOf[Vector[DynSchema]]
      }

    generator.gen(maxDepth)
  }
}
