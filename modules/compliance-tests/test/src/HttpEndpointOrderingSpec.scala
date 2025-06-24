package smithy4s.compliancetests

import weaver._
import smithy4s.http.HttpEndpoint
import smithy4s.http.PathSegment
import smithy4s.http.HttpMethod
import scala.util.Random

/**
 * The following algorithm is used to compare two paths
 * 
  * Given two ambiguous URI patterns A and B with segments [A0, …, An] and [B0, …, Bm] with query string literals [AQ0, …, AQp] and [BQ0, …, BQq]
  * (with both p and q possibly zero, i.e., without query string literals), the following steps are taken to compare them, 
  * for each index x from 0 to min(n, m)
  * 
  * If A[x] and B[x] are both literals then continue (the literal values have to be equal otherwise the patterns are not ambiguous)
  * If A[x] is a literal and B[x] is a label then A is more specific than B,
  * If A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B
  * If n > m then A is more specific than B
  * If p > q then A is more specific than B
  */
object HttpEndpointOrderingSpec extends SimpleIOSuite {

    final case class HttpEndpointDummy(
        path: List[PathSegment], 
        staticQueryParams: Map[String,Seq[String]] = Map.empty
    ) extends HttpEndpoint[Unit] {
      override def path(input: Unit): List[String] = throw new NotImplementedError("HttpEndpointOrderingSpec.HttpEndpointDummy.path")
      override def method: HttpMethod = throw new NotImplementedError("HttpEndpointOrderingSpec.HttpEndpointDummy.method")
      override def code: Int = throw new NotImplementedError("HttpEndpointOrderingSpec.HttpEndpointDummy.code")
    }

    pureTest("static > label > greedy") {
        val a = HttpEndpointDummy(path = List(PathSegment.static("abc"), PathSegment.static("bcd"), PathSegment.label("xyz")))
        val b = HttpEndpointDummy(path = List(PathSegment.static("abc"), PathSegment.label("xyz"), PathSegment.static("cde")))
        val c = HttpEndpointDummy(path = List(PathSegment.greedy("xyz"), PathSegment.static("bcd"), PathSegment.static("cde")))
        val d = HttpEndpointDummy(path = List(PathSegment.greedy("xyz"), PathSegment.label("bcd"), PathSegment.static("cde")))

        val expectedOrder = List[HttpEndpoint[_]](a, b, c, d)
        val shuffleOrder = Random.shuffle(expectedOrder)

        expect(shuffleOrder.sorted == expectedOrder)
    }

    pureTest("A[x] and B[x] are both literals then continue") {
        val a = HttpEndpointDummy(path = List(PathSegment.static("abc")))
        val b = HttpEndpointDummy(path = List(PathSegment.static("bcd")))

        expect(HttpEndpoint.ordering.compare(a, b) == 0)
    }

    pureTest("A[x] is a literal and B[x] is a label then A is more specific than B") {
        val a = HttpEndpointDummy(path = List(PathSegment.static("abc"), PathSegment.static("abc")))
        val b = HttpEndpointDummy(path = List(PathSegment.static("bcd"), PathSegment.label("xyz")))

        expect(HttpEndpoint.ordering.compare(a, b) == -1)
    }

    pureTest("A[x] is a non-greedy label and B[x] is a greedy label then A is more specific than B") {
        val a = HttpEndpointDummy(path = List(PathSegment.static("abc"), PathSegment.label("abc")))
        val b = HttpEndpointDummy(path = List(PathSegment.static("bcd"), PathSegment.greedy("xyz")))

        expect(HttpEndpoint.ordering.compare(a, b) == -1)
    }

    pureTest("n > m then A is more specific than B") {
        val a = HttpEndpointDummy(path = List(PathSegment.static("abc"), PathSegment.static("bcd")))
        val b = HttpEndpointDummy(path = List(PathSegment.static("abc")))

        expect(HttpEndpoint.ordering.compare(a, b) == -1)
    }

    pureTest("p > q then A is more specific than B") {
        val a = HttpEndpointDummy(path = List(PathSegment.static("abc")), staticQueryParams = Map("a" -> Seq.empty, "b" -> Seq.empty))
        val b = HttpEndpointDummy(path = List(PathSegment.static("abc")), staticQueryParams = Map("a" -> Seq.empty))

        expect(HttpEndpoint.ordering.compare(a, b) == -1)
    }
}
