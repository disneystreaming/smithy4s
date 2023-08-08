$version: "2.0"

namespace smithy4s.example

intEnum oneTwo {
    ONE = 1
    TWO = 2
}

enum leftRight {
    LEFT = "left"
    RIGHT = "right"
}

@enum([{
    name: "LEFT"
    value: "left"
}, {
    name: "RIGHT"
    value: "right"
}])
string oldStyleLeftRight

@oneTwo(1)
@leftRight("left")
@oldStyleLeftRight("right")
string StringWithEnumTraits
