$version: "2"

namespace smithy4s.example.error

@error("client")
@httpError(404)
structure NotFoundError {
  error: String,
}
