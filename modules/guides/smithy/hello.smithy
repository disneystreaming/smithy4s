$version: "2"

namespace smithy4s.guides.hello

use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [SayWorld]
}


@readonly
@http(method: "GET", uri: "/hello", code: 200)
operation SayWorld {
  output: World
}

structure World {
  message: String = "World !"
}

