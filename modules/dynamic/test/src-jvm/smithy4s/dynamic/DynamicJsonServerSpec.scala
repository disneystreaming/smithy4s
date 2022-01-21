package smithy4s.dynamic

object DynamicJsonServerSpec {

  val modelString = """|namespace foo
                       |
                       |service KVStore {
                       |  operations[Get, Set, Delete]
                       |}
                       |
                       |operation Set {
                       |  input: KeyValue
                       |}
                       |
                       |operation Get {
                       |  input: Key,
                       |  output: Value
                       |}
                       |
                       |operation Delete {
                       |  output:
                       |}
                       |
                       |structure Key {
                       |  @required
                       |  key: String
                       |}
                       |
                       |structure KeyValue {
                       |  @required
                       |  key: String,
                       |  @required
                       |  value: String
                       |}
                       |
                       |structure Value {
                       |  @required
                       |  value: String
                       |}
                       |""".stripMargin

}
