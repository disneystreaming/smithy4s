namespace smithy4s.example

use alloy#simpleRestJson
use alloy#UUID
use alloy#uuidFormat
use smithy4s.meta#indexedSeq
use smithy4s.meta#vector
use smithy4s.meta#errorMessage

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

/// The most basics of services
/// GetFoo is it's only operation
service FooService {
  version: "1.0.0",
  operations: [GetFoo]
}

/// Returns a useful Foo
/// No input necessary to find our Foo
/// The path for this operation is "/foo"
@readonly
@http(method: "GET", uri: "/foo", code: 200)
operation GetFoo {
  /// Represents the structure of the output of the Foo
  /// if we find a Foo at all
  /// else we return a generic HTTP error code
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

/// Input for getting an Object
/// all fields are required
/// and are given through HTTP labels
/// See https://smithy.io/2.0/spec/http-bindings.html?highlight=httppayload#http-uri-label
structure GetObjectInput {
    /// Sent in the URI label named "key".
    /// Key can also be seen as the filename
    /// It is always required for a GET operation
    @required
    @httpLabel
    key: ObjectKey,

    /// Sent in the URI label named "bucketName".
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

/// Helpful information for Foo
/// int, bigInt and bDec are useful number constructs
/// The string case is there because.
union Foo {
  int: Integer,
  /// this is a comment saying you should be careful for this case
  /// you never know what lies ahead with Strings like this
  str: String,
  bInt: BigInteger,
  bDec: BigDecimal
}

@enum([{value: "Low", name: "LOW"}, {value: "High", name: "HIGH"}])
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


@error("server")
structure ServerErrorCustomMessage {
  @errorMessage
  messageField: String
}

@trait
document arbitraryData

@arbitraryData(str: "hello", int: 1, bool: true, arr: ["one", "two", "three"], obj: { str: "s", i: 1})
structure ArbitraryDataTest {}

@indexedSeq
list SomeIndexSeq {
  member: String
}

@vector
list SomeVector {
  member: String
}
