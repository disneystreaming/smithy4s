namespace smithy4s.example

use smithy4s.api#simpleRestJson
use smithy4s.api#UUID
use smithy4s.api#uuidFormat

@simpleRestJson
service ObjectService {
  version: "1.0.0",
  errors: [ServerError],
  operations: [PutObject, GetObject]
}

@idempotent
@http(method: "PUT", uri: "/{bucketName}/{key}", code: 200)
operation PutObject {
  input: PutObjectInput,
  errors: [NoMoreSpace]
}

@readonly
@http(method: "GET", uri: "/{bucketName}/{key}", code: 200)
operation GetObject {
  input: GetObjectInput,
  output: GetObjectOutput,
  // Testing that errors do not get generated twice if present in
  // both service AND operation
  errors: [ServerError]
}

service FooService {
  version: "1.0.0",
  operations: [GetFoo]
}

@readonly
@http(method: "GET", uri: "/foo", code: 200)
operation GetFoo {
  output: GetFooOutput
}

structure PutObjectInput {
    // Sent in the URI label named "key".
    @required
    @httpLabel
    key: ObjectKey,

    // Sent in the URI label named "bucketName".
    @required
    @httpLabel
    bucketName: BucketName,

    // Sent in the X-Foo header
    @httpHeader("X-Foo")
    foo: LowHigh,

    // Sent in the query string as paramName
    @httpQuery("paramName")
    someValue: SomeValue,

    // Sent in the body
    @httpPayload
    @required
    data: String
}

structure GetObjectInput {
    // Sent in the URI label named "key".
    @required
    @httpLabel
    key: ObjectKey,

    // Sent in the URI label named "bucketName".
    @required
    @httpLabel
    bucketName: BucketName,
}

structure GetObjectOutput {
  @httpHeader("X-Size")
  @required
  size: ObjectSize,

  @httpPayload
  data: String
}

structure GetFooOutput {
  foo: Foo
}

union Foo {
  int: Integer,
  str: String
}

@enum([{value : "Low"}, {value : "High"}])
string LowHigh

@uuidFormat
string ObjectKey
string BucketName
string SomeValue
integer ObjectSize

@error("server")
structure ServerError {
  message: String
}

@trait
document arbitraryData

@arbitraryData(str: "hello", int: 1, bool: true, arr: ["one", "two", "three"], obj: { str: "s", i : 1})
structure ArbitraryDataTest {}
