namespace smithy4s.example

use smithy4s.api#simpleRestJson
use aws.iam#actionPermissionDescription

@simpleRestJson
service ObjectService {
  version: "1.0.0",
  operations: [PutObject, GetObject]
}


@idempotent
@http(method: "PUT", uri: "/{bucketName}/{key}", code: 200)
@actionPermissionDescription("lorem ipsum")
operation PutObject {
  input: PutObjectInput,
  errors: [NoMoreSpace]
}

@readonly
@http(method: "GET", uri: "/{bucketName}/{key}", code: 200)
operation GetObject {
  input: GetObjectInput,
  output: GetObjectOutput
}

structure PutObjectInput {
    // Sent in the URI label named "key".
    @required
    @httpLabel
    key: String,

    // Sent in the URI label named "bucketName".
    @required
    @httpLabel
    bucketName: String,

    // Sent in the X-Foo header
    @httpHeader("X-Foo")
    foo: String,

    // Sent in the query string as paramName
    @httpQuery("paramName")
    someValue: String,

    // Sent in the body
    @httpPayload
    @required
    data: String
}

structure GetObjectInput {
    // Sent in the URI label named "key".
    @required
    @httpLabel
    key: String,

    // Sent in the URI label named "bucketName".
    @required
    @httpLabel
    bucketName: String,
}

structure GetObjectOutput {
  @httpHeader("X-Size")
  @required
  size: Integer,
  @httpPayload
  data: String
}

union Foo {
  int: Integer,
  str: String
}

@error("server")
@httpError(507)
structure NoMoreSpace {
  @required
  message: String,
  foo: Foo
}

