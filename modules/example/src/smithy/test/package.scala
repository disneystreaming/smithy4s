package smithy

package object test {

  type StringList = smithy.test.StringList.Type
  type NonEmptyString = smithy.test.NonEmptyString.Type
  /** Define how a malformed HTTP request is rejected by a server given a specific protocol */
  type HttpMalformedRequestTests = smithy.test.HttpMalformedRequestTests.Type
  type NonEmptyStringList = smithy.test.NonEmptyStringList.Type
  /** Define how an HTTP request is serialized given a specific protocol,
    * authentication scheme, and set of input parameters.
    */
  type HttpRequestTests = smithy.test.HttpRequestTests.Type
  type HttpMalformedRequestTestParametersDefinition = smithy.test.HttpMalformedRequestTestParametersDefinition.Type
  /** Define how an HTTP response is serialized given a specific protocol,
    * authentication scheme, and set of output or error parameters.
    */
  type HttpResponseTests = smithy.test.HttpResponseTests.Type
  type StringMap = smithy.test.StringMap.Type

}