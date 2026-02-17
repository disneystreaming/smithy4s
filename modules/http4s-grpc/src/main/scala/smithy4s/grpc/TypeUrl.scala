package smithy4s.grpc

import smithy4s.ShapeId

// Identifies the type of the serialized Protobuf message with a URI reference
// consisting of a prefix ending in a slash and the fully-qualified type name.
//
// Example: type.googleapis.com/google.protobuf.StringValue
//
// This string must contain at least one `/` character, and the content after
// the last `/` must be the fully-qualified name of the type in canonical
// form, without a leading dot. Do not write a scheme on these URI references
// so that clients do not attempt to contact them.
//
// The prefix is arbitrary and Protobuf implementations are expected to
// simply strip off everything up to and including the last `/` to identify
// the type. `type.googleapis.com/` is a common default prefix that some
// legacy implementations require. This prefix does not indicate the origin of
// the type, and URIs containing it are not expected to respond to any
// requests.
//
// All type URL strings must be legal URI references with the additional
// restriction (for the text format) that the content of the reference
// must consist only of alphanumeric characters, percent-encoded escapes, and
// characters in the following set (not including the outer backticks):
// `/-.~_!$&()*+,;=`. Despite our allowing percent encodings, implementations
// should not unescape them to prevent confusion with existing parsers. For
// example, `type.googleapis.com%2FFoo` should be rejected.
//
// In the original design of `Any`, the possibility of launching a type
// resolution service at these type URLs was considered but Protobuf never
// implemented one and considers contacting these URLs to be problematic and
// a potential security issue. Do not attempt to contact type URLs.
object TypeUrl {

  def fromShapeId(shapeId: ShapeId): String = {
    s"type.googleapis.com/${shapeId.namespace}.${shapeId.name}"
  }

  def extractShapeId(s: String): Option[ShapeId] = {
    val fqn = s.replaceAll(".*/", "")
    val lastDotIndex = fqn.lastIndexOf('.')
    if(lastDotIndex > 0) {
      val namespace = fqn.substring(0, lastDotIndex)
      val name = fqn.substring(lastDotIndex + 1)
      Option(ShapeId(namespace, name))
    } else {
      Option.empty
    }
  }
}
