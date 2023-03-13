$version: "2.0"

namespace service

service Foo {
  version: "1.0.0",
  operations: [Bar]
}

operation Bar {
  input: Input,
  output: Output,
}

structure Input {
  input: InputUnion
}

union InputUnion {
  foo: InputFoo,
  bar: InputBar,
}

structure InputFoo {
  foo: String
}

structure InputBar {
  bar: Integer
}

structure Output {
  foo: String,
  bar: Integer,
}
