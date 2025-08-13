/*
 *  Copyright 2021-2025 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import smithy4s.Blob
import smithy4s.Schema
import smithy4s.example.SampleOpenUnion
import smithy4s.json.internals.JsonPayloadCodecCompilerImpl

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 20, time = 30, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 50, time = 30, timeUnit = TimeUnit.MILLISECONDS)
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
    """{
        "str": "foo",
        "u": null
    }"""

  val lenientUnknownJson =
    """{
        "struct": null,
        "u": null,
        "foo": {"bar": "baz"}
    }"""

  val regularStrJson =
    """{
        "str": "foo"
    }"""

  val regularUnknownJson =
    """{
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
