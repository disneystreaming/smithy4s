package smithy4s

import smithy.api.IdRef

/**
  * Provides default refinements for types from smithy prelude
  */
object refinements {

  implicit val shapeIdRefinement: RefinementProvider[IdRef, String, ShapeId] =
    Refinement
      .drivenBy[IdRef]
      .apply(
        ShapeId.parse(_: String).toRight("Not a valid ShapeId"),
        (_: ShapeId).show
      )

}
