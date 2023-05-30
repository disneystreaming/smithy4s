$version: "2"

namespace smithy4s.example

structure StringBody {
  @httpPayload
  @required
  str: String
}

structure StringEnumBody {
    @httpPayload
    @required
    str: StringEnum
}

structure AudioEnumBody {
    @httpPayload
    @required
    str: AudioEnum
}

structure BlobBody {
  @httpPayload
  @required
  blob: Blob
}

structure CSVBody {
  @httpPayload
  @required
  csv: CSV
}

structure PNGBody {
  @httpPayload
  @required
  png: PNG
}

@mediaType("text/csv")
string CSV

@mediaType("image/png")
blob PNG


enum StringEnum {
    STRING = "string"
    INTERESTING = "interesting"
}

@mediaType("audio/mpeg3")
enum AudioEnum {
   GUITAR = "guitar"
   BASS = "bass"
}