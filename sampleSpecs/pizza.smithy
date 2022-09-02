$version: "2"

namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service PizzaAdminService {
  version: "1.0.0",
  errors: [GenericServerError, GenericClientError],
  operations: [AddMenuItem, GetMenu, Version, Health, HeaderEndpoint, RoundTrip, GetEnum, GetIntEnum, CustomCode]
}

@http(method: "POST", uri: "/restaurant/{restaurant}/menu/item", code: 201)
operation AddMenuItem {
  input: AddMenuItemRequest,
  errors: [PriceError],
  output: AddMenuItemResult
}

@http(method: "POST", uri: "/headers/", code: 200)
operation HeaderEndpoint {
  input: HeaderEndpointData,
  output: HeaderEndpointData
}

@http(method: "POST", uri: "/roundTrip/{label}", code: 200)
operation RoundTrip {
  input: RoundTripData,
  output: RoundTripData
}

structure RoundTripData {
  @httpLabel
  @required
  label: String,
  @httpHeader("HEADER")
  header: String,
  @httpQuery("query")
  query: String,
  body: String
}

structure HeaderEndpointData {
  @httpHeader("X-UPPERCASE-HEADER")
  uppercaseHeader: String,
  @httpHeader("X-Capitalized-Header")
  capitalizedHeader: String,
  @httpHeader("x-lowercase-header")
  lowercaseHeader: String,
  @httpHeader("x-MiXeD-hEaDEr")
  mixedHeader: String,
}

structure AddMenuItemResult {
  @httpPayload
  @required
  itemId: String,
  @timestampFormat("epoch-seconds")
  @httpHeader("X-ADDED-AT")
  @required
  added: Timestamp
}

@readonly
@http(method: "GET", uri: "/version", code: 200)
operation Version {
  output: VersionOutput
}


structure VersionOutput {
  @httpPayload
  @required
  version: String
}

@error("client")
structure PriceError {
  @required
  message: String,
  @required
  @httpHeader("X-CODE")
  code: Integer
}

@http(method: "GET", uri: "/restaurant/{restaurant}/menu", code: 200)
operation GetMenu {
  input: GetMenuRequest,
  errors: [NotFoundError, FallbackError],
  output: GetMenuResult
}

structure GetMenuRequest {
  @httpLabel
  @required
  restaurant: String
}

structure GetMenuResult {
  @required
  @httpPayload
  menu: Menu
}

@error("client")
@httpError(404)
structure NotFoundError {
  @required
  name: String
}

@error("client")
structure FallbackError {
  @required
  error: String
}

map Menu {
  key: String,
  value: MenuItem
}

structure AddMenuItemRequest {
  @httpLabel
  @required
  restaurant: String,
  @httpPayload
  @required
  menuItem: MenuItem
}

structure MenuItem {
  @required
  food: Food,
  @required
  price: Float
}

union Food {
  pizza: Pizza,
  salad: Salad
}

structure Salad {
  @required
  name: String,
  @required
  ingredients: Ingredients
}

structure Pizza {
  @required
  name: String,
  @required
  base: PizzaBase,
  @required
  toppings: Ingredients
}

@enum([{name: "CREAM", value: "C"}, {name: "TOMATO", value: "T"}])
string PizzaBase

@enum([{value: "Mushroom"}, {value: "Cheese"}, {value: "Salad"}, {value: "Tomato"}])
string Ingredient

list Ingredients {
  member: Ingredient
}

@error("server")
@httpError(502)
structure GenericServerError {
  @required
  message: String
}

@error("client")
@httpError(418)
structure GenericClientError {
  @required
  message: String
}

@readonly
@http(method: "GET", uri: "/health", code: 200)
operation Health {
  input: HealthRequest,
  output: HealthResponse,
  errors: [ UnknownServerError ]
}

structure HealthRequest {
  @httpQuery("query")
  @length(min: 0, max: 5)
  query: String
}

@freeForm(i : 1, a: 2)
structure HealthResponse {
  @required
  status: String
}

// This error indicates a fatal, unexpected error has occurred. Fallback strategies ought to be triggered by this
// error.
@error("server")
@httpError(500)
structure UnknownServerError {
  @required
  errorCode: UnknownServerErrorCode,

  description: String,

  stateHash: String
}

// Define the singular error code that can be returned for an UnknownServerError
@enum([
 {
    value: "server.error",
    name: "ERROR_CODE"
 }
])
string UnknownServerErrorCode


@trait
document freeForm

@readonly
@http(method: "GET", uri: "/get-enum/{aa}", code: 200)
operation GetEnum {
  input: GetEnumInput,
  output: GetEnumOutput,
  errors: [ UnknownServerError ]
}

structure GetEnumInput {
  @required
  @httpLabel
  aa: TheEnum
}

structure GetEnumOutput {
  result: String
}

enum TheEnum {
  V1 = "v1"
  V2 = "v2"
}

@readonly
@http(method: "GET", uri: "/get-int-enum/{aa}", code: 200)
operation GetIntEnum {
  input := {
    @required
    @httpLabel
    aa: EnumResult
  }
  output := {
    @required
    result: EnumResult
  }
  errors: [ UnknownServerError ]
}

intEnum EnumResult {
    FIRST = 1
    SECOND = 2
}

@readonly
@http(method: "GET", uri: "/custom-code/{code}", code: 200)
operation CustomCode {
  input: CustomCodeInput,
  output: CustomCodeOutput,
  errors: [ UnknownServerError ]
}

structure CustomCodeInput {
  @httpLabel
  @required
  code: Integer
}

structure CustomCodeOutput {
  @httpResponseCode
  code: Integer
}