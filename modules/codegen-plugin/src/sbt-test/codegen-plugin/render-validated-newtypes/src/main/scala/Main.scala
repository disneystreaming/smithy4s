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

package newtypes.validated

import newtypes.validated._

object Main extends App {
  try {
    val cityOrError: Either[String, ValidatedCity] = ValidatedCity("test-city")
    val nameOrError: Either[String, ValidatedName] = ValidatedName("test-name")
    val country: String = "test-country"

    println(
      (nameOrError, cityOrError) match {
        case (Right(name), Right(city)) => s"Success: ${Person(name, Some(city), Some(country))}"
        case _ => s"Error"
      }
    )
  } catch {
    case _: java.lang.ExceptionInInitializerError =>
      println("failed")
      sys.exit(1)
  }
}
