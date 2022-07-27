package smithy4s.example

import smithy4s.schema.Schema._

object `$Age` {
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy4s.example.AgeFormat(),
  )
  implicit val schema : smithy4s.Schema[smithy4s.example.refined.Age] = int.refined(smithy4s.example.refined.Age.provider)
}