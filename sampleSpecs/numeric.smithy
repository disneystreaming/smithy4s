$version: "2"

namespace smithy4s.example

structure Numeric {
    i: Integer = 1
    f: Float = 1.0
    d: Double = 1.0
    s: Short = 1
    l: Long = 9999999999
    bi: BigInteger = 1
    bd: BigDecimal = 1
}

structure BigNumeric {
    bi: BigInteger = 4294967296
    bd: BigDecimal = 9007199254740993
    @range(min: -9007199254740993, max: 9007199254740993)
    l: Long
    doc: Document = 18446744073709551616
}
