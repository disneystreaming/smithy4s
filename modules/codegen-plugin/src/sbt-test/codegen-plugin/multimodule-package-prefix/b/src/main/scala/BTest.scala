package example

import com.example.second.Second
import gen.com.example.first.MyString

object BTest {
  def main(args: Array[String]): Unit = {
    println(Second(MyString("hello")))
  }
}
