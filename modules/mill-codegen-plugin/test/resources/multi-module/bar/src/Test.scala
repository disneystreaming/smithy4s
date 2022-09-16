package bar

import foo._

object BarTest {

  def main(args: Array[String]): Unit = println(Bar(Some(Foo(Some(1)))))

}
