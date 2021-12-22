namespace smithy4s.example

@error("server")
@httpError(507)
structure NoMoreSpace {
  @required
  message: String,
  foo: Foo
}
