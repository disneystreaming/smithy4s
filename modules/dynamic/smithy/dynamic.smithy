$version: "2"

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
  @default
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
  @default
  traits: TraitMap,
  @default
  members: MemberMap
}

structure EnumShape {
  @default
  traits: TraitMap,
  @default
  members: MemberMap
}

structure IntEnumShape {
  @default
  traits: TraitMap,
  @default
  members: MemberMap
}

structure BlobShape {
  @default
  traits: TraitMap
}

structure ByteShape {
  @default
  traits: TraitMap
}

structure BooleanShape {
  @default
  traits: TraitMap
}

structure IntegerShape {
  @default
  traits: TraitMap
}

structure LongShape {
  @default
  traits: TraitMap
}

structure ShortShape {
  @default
  traits: TraitMap
}

structure FloatShape {
  @default
  traits: TraitMap
}

structure DoubleShape {
  @default
  traits: TraitMap
}

structure BigDecimalShape {
  @default
  traits: TraitMap
}

structure BigIntegerShape {
  @default
  traits: TraitMap
}

structure DocumentShape {
  @default
  traits: TraitMap
}

structure TimestampShape {
  @default
  traits: TraitMap
}

structure ListShape {
  @required
  member: MemberShape,
  @default
  traits: TraitMap
}

structure SetShape {
  @required
  member: MemberShape,
  @default
  traits: TraitMap
}

structure MapShape {
  @required
  key: MemberShape,
  @required
  value: MemberShape,
  @default
  traits: TraitMap
}

structure MemberShape {
  @required
  target: IdRef,
  @default
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
  @default
  members: MemberMap,
  @default
  traits: TraitMap
}

structure UnionShape {
  @default
  members: MemberMap,
  @default
  traits: TraitMap
}

structure OperationShape {
  input: MemberShape,
  output: MemberShape,
  @default
  errors: MemberList,
  @default
  traits: TraitMap
}

structure ServiceShape {
  version: String,
  @default
  errors: MemberList,
  @default
  operations: MemberList,
  @default
  resources: MemberList,
  @default
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
  @default
  operations: MemberList,
  @default
  resources: MemberList
}





