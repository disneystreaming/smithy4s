$version: "2.0"

namespace smithy4s.example

use alloy#openEnum

/// Just a dummy service to ensure that the rendered services compile
/// when testing core
service DummyService {
  version: "0.0",
  operations: [Dummy, DummyHostPrefix, DummyPath]
}

@http(method: "GET", uri: "/dummy")
@endpoint(hostPrefix: "foo.{label1}--abc{label2}.{label3}.secure.")
operation DummyHostPrefix {
    input: HostLabelInput
}

structure HostLabelInput {
    @required
    @hostLabel
    label1: String
    @required
    @hostLabel
    label2: String
    @required
    @hostLabel
    label3: HostLabelEnum
}

enum HostLabelEnum {
    THING1
    THING2
}

@http(method: "GET", uri: "/dummy")
@readonly
operation Dummy {
  input: Queries
}

@http(method: "GET", uri: "/dummy-path/{str}/{int}/{ts1}/{ts2}/{ts3}/{ts4}/{b}/{ie}?value=foo&baz=bar")
@readonly
operation DummyPath {
  input: PathParams
}

structure Queries {
  @httpQuery("str")
  str: String,
  @httpQuery("int")
  int: Integer,
  @httpQuery("ts1")
  ts1: Timestamp,
  @timestampFormat("date-time")
  @httpQuery("ts2")
  ts2: Timestamp,
  @httpQuery("ts3")
  @timestampFormat("epoch-seconds")
  ts3: Timestamp,
  @httpQuery("ts4")
  @timestampFormat("http-date")
  ts4: Timestamp,
  @httpQuery("b")
  b: Boolean,
  @httpQuery("sl")
  sl: StringList,
  @httpQuery("nums")
  ie: Numbers,
  @httpQuery("openNums")
  on: OpenNums
  @httpQuery("openNumsStr")
  ons: OpenNumsStr
  @httpQueryParams
  slm: StringMap
}

structure QueriesWithDefaults {
  @httpQuery("dflt")
  dflt: String = "test"
}

structure HeadersStruct {
  @httpHeader("str")
  str: String,
  @httpHeader("int")
  int: Integer,
  @httpHeader("ts1")
  ts1: Timestamp,
  @timestampFormat("date-time")
  @httpHeader("ts2")
  ts2: Timestamp,
  @httpHeader("ts3")
  @timestampFormat("epoch-seconds")
  ts3: Timestamp,
  @httpHeader("ts4")
  @timestampFormat("http-date")
  ts4: Timestamp,
  @httpHeader("b")
  b: Boolean,
  @httpHeader("sl")
  sl: StringList,
  @httpHeader("nums")
  ie: Numbers,
  @httpHeader("openNums")
  on: OpenNums
  @httpHeader("openNumsStr")
  ons: OpenNumsStr
  @httpPrefixHeaders("foo-")
  slm: StringMap
}

structure HeadersWithDefaults {
  @httpHeader("dflt")
  dflt: String = "test"
}


structure PathParams {
  @httpLabel
  @required
  str: String,
  @httpLabel
  @required
  int: Integer,
  @httpLabel
  @required
  ts1: Timestamp,
  @httpLabel
  @timestampFormat("date-time")
  @required
  ts2: Timestamp,
  @httpLabel
  @timestampFormat("epoch-seconds")
  @required
  ts3: Timestamp,
  @httpLabel
  @timestampFormat("http-date")
  @required
  ts4: Timestamp,
  @httpLabel
  @required
  b: Boolean,
  @httpLabel
  @required
  ie: Numbers
}

structure ValidationChecks {
  @httpQuery("str")
  @length(min: 1, max: 10)
  str: String,
  @httpQuery("lst")
  @length(min: 1, max: 10)
  lst: StringList,
  @httpQuery("int")
  @range(min: 1, max: 10)
  int: Integer
}

intEnum Numbers {
    @enumValue(1)
    ONE
    @enumValue(2)
    TWO
}

@openEnum
intEnum OpenNums {
  ONE = 1
  TWO = 2
}

@openEnum
enum OpenNumsStr {
  ONE
  TWO
}
