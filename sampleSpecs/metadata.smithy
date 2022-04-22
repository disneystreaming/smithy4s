$version: "2.0"

namespace smithy4s.example

/// Just a dummy service to ensure that the rendered services compile
/// when testing core
service DummyService {
  version: "0.0",
  operations: [Dummy, DummyPath]
}

@http(method: "GET", uri: "/dummy")
@readonly
operation Dummy {
  input: Queries
}

@http(method: "GET", uri: "/dummy-path/{str}/{int}/{ts1}/{ts2}/{ts3}/{ts4}/{b}/{ie}")
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
  ts2 : Timestamp,
  @httpQuery("ts3")
  @timestampFormat("epoch-seconds")
  ts3: Timestamp,
  @httpQuery("ts4")
  @timestampFormat("http-date")
  ts4: Timestamp,
  @httpQuery("b")
  b: Boolean,
  @httpQuery("sl")
  sl : StringList,
  @httpQuery("nums")
  ie: Numbers,
  @httpQueryParams
  slm: StringMap
}

structure Headers {
  @httpHeader("str")
  str: String,
  @httpHeader("int")
  int: Integer,
  @httpHeader("ts1")
  ts1: Timestamp,
  @timestampFormat("date-time")
  @httpHeader("ts2")
  ts2 : Timestamp,
  @httpHeader("ts3")
  @timestampFormat("epoch-seconds")
  ts3: Timestamp,
  @httpHeader("ts4")
  @timestampFormat("http-date")
  ts4: Timestamp,
  @httpHeader("b")
  b: Boolean,
  @httpHeader("sl")
  sl : StringList,
  @httpHeader("nums")
  ie: Numbers,
  @httpPrefixHeaders("foo-")
  slm: StringMap
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
  ts2 : Timestamp,
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

list StringList {
  member: String
}

map StringMap {
  key: String,
  value: String
}

intEnum Numbers {
    @enumValue(1)
    ONE
    @enumValue(2)
    TWO
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
