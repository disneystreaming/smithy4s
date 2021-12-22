package smithy4s
package scalacheck

import org.scalacheck.Gen

object SchematicGen
    extends Schematic[org.scalacheck.Gen]
    with schematic.scalacheck.SchematicGen {
  def withHints[A](fa: Gen[A], hints: Hints): Gen[A] = fa
  def timestamp: Gen[Timestamp] = Smithy4sGen.genTimestamp
  def document: Gen[Document] = Smithy4sGen.genDocument(1)
}
