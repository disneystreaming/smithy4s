package smithy4s.compliancetests

import org.http4s.Uri
import cats.implicits._
import java.nio.charset.StandardCharsets

package object internals {
  def splitQuery(queryString: String): (String, String) = {
    queryString.split("=", 2) match {
      case Array(k, v) =>
        (
          k,
          Uri.decode(
            toDecode = v,
            charset = StandardCharsets.UTF_8,
            plusIsSpace = true
          )
        )
      case Array(k) => (k, "")
    }
  }

  def parseQueryParams(
      queryParams: Option[List[String]]
  ): Map[String, List[String]] = {
    queryParams.combineAll
      .map(splitQuery)
      .foldRight[Map[String, List[String]]](Map.empty) { case ((k, v), acc) =>
        acc.get(k) match {
          case Some(value) => acc + (k -> (v :: value))
          case None        => acc + (k -> List(v))
        }
      }
  }

}
