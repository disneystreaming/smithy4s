$version: "2.0"

namespace smithy4s.example

@documentation("FaceCard types")
intEnum FaceCard {
    @enumValue(1)
    JACK
    @enumValue(2)
    QUEEN
    @enumValue(3)
    KING
    @enumValue(4)
    ACE
    @enumValue(5)
    JOKER
}

enum Letters {
  @enumValue("a")
  A
  @enumValue("b")
  B
  @enumValue("c")
  C
}

enum SwitchState {
  ON
  OFF
}
