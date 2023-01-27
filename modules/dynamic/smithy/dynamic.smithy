metadata suppressions = [
    {
        id: "UnreferencedShape",
        namespace: "smithy4s.dynamic.model",
        reason: "This is a library namespace."
    }
]


namespace smithy4s.dynamic.model

use alloy#discriminated

/// This is a best-effort meta-representation of the smithy-model, that we should be able
/// to deserialise from Json.
structure Model {
  smithy: String,
  metadata: MetadataMap,
  @required
  shapes: ShapeMap
}

string IdRef

map MetadataMap {
  key: String,
  value: Document
}

map ShapeMap {
  key: IdRef,
  value: Shape
}

map TraitMap {
  key: IdRef,
  value: Document
}

@discriminated("type")
union Shape {
  blob: BlobShape,
  byte: ByteShape,
  string: StringShape,
  boolean: BooleanShape,
  integer: IntegerShape,
  short: ShortShape,
  long: LongShape,
  double: DoubleShape,
  float: FloatShape,
  bigDecimal: BigDecimalShape,
  bigInteger: BigIntegerShape,
  document: DocumentShape,
  timestamp: TimestampShape,
  list: ListShape,
  set: SetShape,
  map: MapShape,
  structure: StructureShape,
  union: UnionShape,
  operation: OperationShape,
  service: ServiceShape,
  resource: ResourceShape,
  @jsonName("enum")
  _enum: EnumShape,
  intEnum: IntEnumShape,
}

structure StringShape {
  traits: TraitMap,
  members: MemberMap
}

structure EnumShape {
  traits: TraitMap,
  members: MemberMap
}

structure IntEnumShape {
  traits: TraitMap,
  members: MemberMap
}

structure BlobShape {
  traits: TraitMap
}

structure ByteShape {
  traits: TraitMap
}

structure BooleanShape {
  traits: TraitMap
}

structure IntegerShape {
  traits: TraitMap
}

structure LongShape {
  traits: TraitMap
}

structure ShortShape {
  traits: TraitMap
}

structure FloatShape {
  traits: TraitMap
}

structure DoubleShape {
  traits: TraitMap
}

structure BigDecimalShape {
  traits: TraitMap
}

structure BigIntegerShape {
  traits: TraitMap
}

structure DocumentShape {
  traits: TraitMap
}

structure TimestampShape {
  traits: TraitMap
}

structure ListShape {
  @required
  member: MemberShape,
  traits: TraitMap
}

structure SetShape {
  @required
  member: MemberShape,
  traits: TraitMap
}

structure MapShape {
  @required
  key: MemberShape,
  @required
  value: MemberShape,
  traits: TraitMap
}

structure MemberShape {
  @required
  target: IdRef,
  traits: TraitMap,
}

list MemberList {
  member: MemberShape
}

map MemberMap {
  key: String,
  value: MemberShape
}

structure StructureShape {
  members: MemberMap,
  traits: TraitMap
}

structure UnionShape {
  members: MemberMap,
  traits: TraitMap
}

structure OperationShape {
  input: MemberShape,
  output: MemberShape,
  errors: MemberList,
  traits: TraitMap
}

structure ServiceShape {
  version: String,
  errors: MemberList,
  operations: MemberList,
  resources: MemberList,
  traits: TraitMap
}

structure ResourceShape {
  /// ignored: identifiers, properties, collectionOperations
  create: MemberShape,
  put: MemberShape,
  read: MemberShape,
  update: MemberShape,
  delete: MemberShape,
  list: MemberShape,
  operations: MemberList,
  resources: MemberList
}





