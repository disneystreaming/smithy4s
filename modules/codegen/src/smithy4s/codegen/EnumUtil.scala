package smithy4s.codegen

object EnumUtil {
  private def toCamelCase(value: String): String = {
    val (_, output) = value.foldLeft((false, "")) {
      case ((wasLastSkipped, str), c) =>
        if (c.isLetterOrDigit) {
          val newC =
            if (wasLastSkipped) c.toString.capitalize else c.toString
          (false, str + newC)
        } else {
          (true, str)
        }
    }
    output
  }

  def enumValueClassName(
      name: Option[String],
      value: String,
      ordinal: Int
  ) = {
    name.getOrElse {
      val camel = toCamelCase(value).capitalize
      if (camel.nonEmpty) camel else "Value" + ordinal
    }

  }

}
