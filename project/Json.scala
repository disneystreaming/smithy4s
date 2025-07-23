import sjsonnew._
import BasicJsonProtocol._
import sbt.FileInfo
import sbt.HashFileInfo
import sbt.io.Hash
import scala.jdk.CollectionConverters._
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors

object Json {

  implicit val pathFormat: JsonFormat[File] =
    BasicJsonProtocol.projectFormat[File, HashFileInfo](
      p => {
        if (p.isFile()) FileInfo.hash(p)
        else
          // If the path is a directory, we get the hashes of all files
          // then hash the concatenation of the hash's bytes.
          FileInfo.hash(
            p,
            Hash(
              Files
                .walk(p.toPath(), 2)
                .collect(Collectors.toList())
                .asScala
                .map(_.toFile())
                .map(Hash(_))
                .foldLeft(Array.emptyByteArray)(_ ++ _)
            )
          )
      },
      hash => hash.file
    )
}
