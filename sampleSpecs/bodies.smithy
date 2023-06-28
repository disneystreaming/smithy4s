$version: "2"

namespace smithy4s.example

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
