package smithy4s

object HintsEqualitySpec extends weaver.FunSuite {

  test("static equals static") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = alloy.DataExample.string("test")
    expect(one == two) && expect(two == one)
  }

  test("dynamic equals dynamic") {
    val one: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test")))
    )
    val two: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test")))
    )
    expect(one == two) && expect(two == one)
  }

  test("static equals dynamic") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test")))
    )
    expect(one == two) && expect(two == one)
  }

  test("static NOT equals dynamic") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = Hints.Binding.DynamicBinding(
      ShapeId(namespace = "alloy", name = "DataExample"),
      Document.DObject(Map("string" -> Document.fromString("test2")))
    )
    expect(one != two) && expect(two != one)
  }

  test("static equals static toDynamicBinding") {
    val one: Hint = alloy.DataExample.string("test")
    val two: Hint = Hints.Binding
      .StaticBinding(
        alloy.DataExample,
        alloy.DataExample.string("test")
      )
      .toDynamicBinding
    expect(one == two) && expect(two == one)
  }
}
