
import cats.Hash
import smithy4s.schema.Schema.{bigdecimal, bigint, boolean, byte, bytes, double, enumeration, float, indexedSeq, int, list, long, map, recursive, set, short, string, struct, timestamp, union, vector, StructSchema}
import smithy4s.schema.{Schema, SchemaVisitor}
import smithy4s.{ByteArray, Hints, ShapeId, Timestamp}
import smithy4s.cats.SchemaVisitorHash
import weaver.FunSuite

object HashVisitorSpec extends FunSuite{

    val visitor:SchemaVisitor[Hash] =  SchemaVisitorHash

    test("int") {
      val schema: Schema[Int] = int
      val intValue = 1
      val hashOutput = visitor(schema).hash(intValue)
      expect.eql(intValue.hashCode,hashOutput)
    }

    test("string") {
      val schema: Schema[String] = string
      val foo = "foo"
      val hashOutput = visitor(schema).hash(foo)
      expect.eql( "foo".hashCode,hashOutput)
    }

    test("boolean") {
      val schema: Schema[Boolean] = boolean
      val foo = true
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }

    test("long") {
      val schema: Schema[Long] = long
      val foo = 1L
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }

    test("short") {
      val schema: Schema[Short] = short
      val foo = 1.toShort
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }

    test("byte") {
      val schema: Schema[Byte] = byte
      val foo = 1.toByte
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )

    }

    test("double") {
      val schema: Schema[Double] = double
      val foo = 1.0
      val hashOutput = visitor(schema).hash(foo)
       expect.eql(foo.hashCode(),hashOutput )

    }

    test("float") {
      val schema: Schema[Float] = float
      val foo = 1.0f
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }

    test("bigint") {
      val schema: Schema[BigInt] = bigint
      val foo = BigInt(1)
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }

    test("bigdecimal") {
      val schema: Schema[BigDecimal] = bigdecimal
      val foo = BigDecimal(1)
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )

    }

    test("smithy4s ByteArray") {
      val schema: Schema[ByteArray] = bytes
      val fooBar = ByteArray("fooBar".getBytes)
      val hashOutput = visitor(schema).hash(fooBar)
      expect.eql(fooBar.array.hashCode(),hashOutput )
    }

    test("smithy4s timestamp") {
      val schema: Schema[Timestamp] = timestamp
      val now = java.time.Instant.now()
      val foo = Timestamp.fromEpochSecond(now.getEpochSecond)
      val hashOutput = visitor(schema).hash(foo)
      expect.eql(foo.epochSecond.hashCode(),hashOutput )
    }

    test("struct") {
      case class Foo(x: String, y: String)
      object Foo {
        val schema: Schema[Foo] = {
          StructSchema(
            ShapeId("", "Foo"),
            Hints.empty,
            Vector(
              string.required[Foo]("x", _.x),
              string.required[Foo]("y", _.y)
            ),
            arr =>
              Foo.apply(
                arr(0).asInstanceOf[String],
                arr(1).asInstanceOf[String]
              )
          )

        }
      }
      val foo = Foo("foo", "bar")
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }
    test("struct: empty optional") {
      case class Foo(x: String, y: Option[String])
      object Foo {
        val schema: Schema[Foo] = {
          StructSchema(
            ShapeId("", "Foo"),
            Hints.empty,
            Vector(
              string.required[Foo]("x", _.x),
              string.optional[Foo]("y", _.y)
            ),
            arr =>
              Foo.apply(
                arr(0).asInstanceOf[String],
                arr(1).asInstanceOf[Option[String]]
              )
          )

        }
      }

      val foo = Foo("foo", None)
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )
    }

    test("list") {
      case class Foo(foos: List[Int])
      object Foo {
        val schema: Schema[Foo] = {
          val foos = list(int)
            .required[Foo]("foos", _.foos)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
      }
      val foo = Foo(List(1, 2, 3))
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.foos.hashCode(),hashOutput )
    }

    test("set") {
      case class Foo(foos: Set[Int])
      object Foo {
        val schema: Schema[Foo] = {
          val foos = set(int)
            .required[Foo]("foos", _.foos)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
      }
      val foo = Foo(Set(1, 2, 3))
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.foos.hashCode(),hashOutput )
    }
    test("vector"){
      case class Foo(foos: Vector[Int])
      object Foo {
        val schema: Schema[Foo] = {
          val foos = vector(int)
            .required[Foo]("foos", _.foos)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
      }
      val foo = Foo(Vector(1, 2, 3))
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.foos.hashCode(),hashOutput )
    }

    test("indexedSeq"){
      case class Foo(foos: IndexedSeq[Int])
      object Foo {
        val schema: Schema[Foo] = {
          val foos = indexedSeq(int)
            .required[Foo]("foos", _.foos)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
      }
      val foo = Foo(IndexedSeq(1, 2, 3))
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.foos.hashCode(),hashOutput )
    }

    test("map"){
      case class Foo(foos: Map[String, Int])
      object Foo {
        val schema: Schema[Foo] = {
          val foos = map(string, int)
            .required[Foo]("foos", _.foos)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
      }
      val foo = Foo(Map("foo" -> 1, "bar" -> 2))
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.foos.hashCode(),hashOutput )
    }



    test("recursion") {
      case class Foo(foo: Option[Foo])
      object Foo {
        val schema: Schema[Foo] = recursive {
          val foos = schema.optional[Foo]("foo", _.foo)
          struct(foos)(Foo.apply)
        }.withId(ShapeId("", "Foo"))
      }

      val foo = Foo(Some(Foo(None)))
      val hashOutput = visitor(Foo.schema).hash(foo)
      expect.eql(foo.hashCode(),hashOutput )

    }

    test("union") {
      sealed trait IntOrString
      case class IntValue(value: Int) extends IntOrString
      case class StringValue(value: String) extends IntOrString
      val schema: Schema[IntOrString] = {
        val intValue = int.oneOf[IntOrString]("intValue", IntValue(_))
        val stringValue = string.oneOf[IntOrString]("stringValue", StringValue(_))
        union(intValue, stringValue) {
          case IntValue(int)     => intValue(int)
          case StringValue(string) => stringValue(string)
        }.withId(ShapeId("", "Foo"))
      }
      val foo0 =IntValue(1)
      val foo1 = StringValue("foo")
      val hashOutput0 = visitor(schema).hash(foo0)
      val hashOutput1 = visitor(schema).hash(foo1)
      expect.eql(foo0.hashCode(),hashOutput0 )
      expect.eql(foo1.hashCode(),hashOutput1 )
    }

    test("enumeration") {
      sealed abstract class FooBar(val stringValue: String, val intValue: Int)
        extends smithy4s.Enumeration.Value {
        val name = stringValue
        val value = stringValue
        val hints = Hints.empty
      }
      object FooBar {
        case object Foo extends FooBar("foo", 0)
        case object Bar extends FooBar("bar", 1)
        val schema: Schema[FooBar] =
          enumeration[FooBar](List(Foo, Bar))
      }
      val foo = FooBar.Foo
      val hashOutput = visitor(FooBar.schema).hash(foo)
      val  bar = FooBar.Bar
      val hashOutput1 = visitor(FooBar.schema).hash(bar)
      expect.eql(foo.hashCode(),hashOutput )
      expect.eql(bar.hashCode(),hashOutput1 )
    }

}

