$version: "2"

namespace smithy4s.guides.hello

use alloy#simpleRestJson

@simpleRestJson
@cors(origin: "http://mysite.com", additionalAllowedHeaders: ["Authorization"], additionalExposedHeaders: ["X-Smithy4s"])
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

