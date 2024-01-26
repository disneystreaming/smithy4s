$version: "2"

namespace smithy4s.example

use alloy#simpleRestJson
use smithy4s.meta#packedInputs

@simpleRestJson
@packedInputs
service ServiceWithNullsAndDefaults {
  version: "1.0.0",
  operations: [Operation]
}


@http(method: "POST", uri: "/operation/{requiredLabel}")
operation Operation {
    input := {
      optional: String

      @default("optional-default")
      optionalWithDefault: String

      @httpLabel
      @required
      @default("required-label-with-default")
      requiredLabel: String
      
      @default("required-default")
      @required
      requiredWithDefault: String

      @httpHeader("optional-header")
      optionalHeader: String

      @httpHeader("optional-header-with-default")
      @default("optional-header-with-default")
      optionalHeaderWithDefault: String

      @httpHeader("required-header-with-default")
      @required
      @default("required-header-with-default")
      requiredHeaderWithDefault: String

      @httpQuery("optional-query")
      optionalQuery: String

      @httpQuery("optional-query-with-default")
      @default("optional-query-with-default")
      optionalQueryWithDefault: String

      @httpQuery("required-query-with-default")
      @default("required-query-with-default")
      requiredQueryWithDefault: String
    }

    output := {
      optional: String

      @default("optional-default")
      optionalWithDefault: String
      
      @default("required-default")
      @required
      requiredWithDefault: String
      
      @httpHeader("optional-header")
      optionalHeader: String

      @httpHeader("optional-header-with-default")
      @default("optional-header-with-default")
      optionalHeaderWithDefault: String

      @httpHeader("required-header-with-default")
      @required
      @default("required-header-with-default")
      requiredHeaderWithDefault: String
    }
}
