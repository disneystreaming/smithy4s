$version: "2"

namespace smithy4s.benchmark

service BenchmarkService {
  version: "1.0.0",
  operations: [CreateObject, SendString]
}

@http(method: "POST", uri: "/complex/{bucketName}/{key}", code: 200)
operation CreateObject {
  input: CreateObjectInput
}

@http(method: "POST", uri: "/simple/{bucketName}/{key}", code: 200)
operation SendString {
  input: SendStringInput
}

structure SendStringInput {
  // Sent in the URI label named "key".
  @required
  @httpLabel
  key: String,

  // Sent in the URI label named "bucketName".
  @required
  @httpLabel
  bucketName: String,

  @required
  @httpPayload
  body: String
}

structure CreateObjectInput {
    // Sent in the URI label named "key".
    @required
    @httpLabel
    key: String,

    // Sent in the URI label named "bucketName".
    @required
    @httpLabel
    bucketName: String,

    // Sent in the body
    @httpPayload
    @required
    payload: S3Object
}

structure S3Object {
  @required
  id: String,

  @required
  owner: String,

  @required
  attributes: Attributes,

  @required
  data: Blob
}

structure Attributes {
  @required
  user: String,

  @required
  public: Boolean,

  @required
  size: Long,

  @required
  @timestampFormat("epoch-seconds")
  creationDate: Timestamp,

  @required
  region: String,

  queryable: Boolean,
  @timestampFormat("epoch-seconds")
  queryableLastChange: Timestamp,
  blockPublicAccess: Boolean,
  permissions: ListPermissions,
  tags: ListTags,
  backedUp: Boolean,
  metadata: ListMetadata,
  encryption: Encryption
}

list ListTags {
  member: String
}

list ListPermissions {
  member: Permission
}

list ListMetadata {
  member: Metadata
}

structure Permission {
  read: Boolean,
  write: Boolean,
  directory: Boolean
}

structure Metadata {
  contentType: String,
  @timestampFormat("epoch-seconds")
  lastModified: Timestamp,
  checkSum: String,
  pendingDeletion: Boolean,
  etag: String
}

structure Encryption {
  user: String,
  @timestampFormat("epoch-seconds")
  date: Timestamp,
  metadata: EncryptionMetadata
}

structure EncryptionMetadata {
  system: String,
  credentials: Creds,
  partial: Boolean
}

structure Creds {
  user: String,
  key: String
}
