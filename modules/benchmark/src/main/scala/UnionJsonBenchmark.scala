package smithy4s.benchmark

import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Mode
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Scope
import smithy4s.json.internals.JsonPayloadCodecCompilerImpl
import smithy4s.Schema
import smithy4s.example.SampleOpenUnion
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole
import smithy4s.Blob

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(3)
class UnionJsonBenchmark {
  private val lenientCodec =
    JsonPayloadCodecCompilerImpl.defaultJsonPayloadCodecCompiler
      .configureJsoniterCodecCompiler(
        _.withLenientTaggedUnionDecoding
      )

  private val regularCodec =
    JsonPayloadCodecCompilerImpl.defaultJsonPayloadCodecCompiler

  private val lenientEncoder =
    lenientCodec.encoders.fromSchema(Schema[SampleOpenUnion])
  private val lenientDecoder =
    lenientCodec.decoders.fromSchema(Schema[SampleOpenUnion])

  private val regularEncoder =
    regularCodec.encoders.fromSchema(Schema[SampleOpenUnion])
  private val regularDecoder =
    regularCodec.decoders.fromSchema(Schema[SampleOpenUnion])

  val lenientStrJson =
    """
    {
        "str": "foo",
        "u": null
    }"""

  val lenientUnknownJson =
    """
    {
        "struct": null,
        "u": null,
        "foo": {"bar": "baz"}
    }"""

  val regularStrJson =
    """
    {
        "str": "foo"
    }"""

  val regularUnknownJson =
    """
    {
        "foo": {"bar": "baz"}
    }"""

  var str: String = "foo"

  @Benchmark
  def lenientRoundtrip(blackhole: Blackhole) = {
    blackhole.consume(roundtrip(SampleOpenUnion.str(str), isLenient = true))
  }

  @Benchmark
  def regularRoundtrip(blackhole: Blackhole) = {
    blackhole.consume(roundtrip(SampleOpenUnion.str(str), isLenient = false))
  }

  @Benchmark
  def lenientStrDecode(blackhole: Blackhole) = {
    blackhole.consume(
      assert(lenientDecoder.decode(Blob(lenientStrJson)).isRight)
    )
  }

  @Benchmark
  def regularStrDecode(blackhole: Blackhole) = {
    blackhole.consume(
      assert(regularDecoder.decode(Blob(regularStrJson)).isRight)
    )
  }

  @Benchmark
  def lenientUnknownDecode(blackhole: Blackhole) = {
    blackhole.consume(
      assert(lenientDecoder.decode(Blob(lenientUnknownJson)).isRight)
    )
  }

  @Benchmark
  def regularUnknownDecode(blackhole: Blackhole) = {
    blackhole.consume(
      assert(regularDecoder.decode(Blob(regularUnknownJson)).isRight)
    )
  }

  private def roundtrip(a: SampleOpenUnion, isLenient: Boolean) = {
    val (encoder, decoder) =
      if (isLenient) (lenientEncoder, lenientDecoder)
      else (regularEncoder, regularDecoder)
    val blob = encoder.encode(a)
    decoder.decode(blob) match {
      case Left(value)  => sys.error("Not matched")
      case Right(value) => assert(a == value)
    }
  }

}
