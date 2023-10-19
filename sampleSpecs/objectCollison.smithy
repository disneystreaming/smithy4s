$version: "2"

namespace smithy4s.example

// Testing collision of rendered operations with https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html
service ObjectCollision {
    operations: [
        Clone
        Equals
        Finalize
        GetClass
        HashCode
        Notify
        NotifyAll
        ToString
        Wait
    ]
}

operation Clone {

}

operation Equals {

}

operation Finalize {

}

operation GetClass {

}

operation HashCode {

}

operation Notify {

}

operation NotifyAll {

}

operation ToString {

}

operation Wait {

}
