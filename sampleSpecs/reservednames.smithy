$version: "2.0"

namespace smithy4s.example.collision

use alloy#simpleRestJson
use smithy4s.example.package#MyPackageString
use smithy4s.example.package#MyPackageStringTrait

@simpleRestJson
service ReservedNameService {
    version: "1.0.0"
    operations: [
        Set
        List
        Map
        Option
    ]
}

@http(method: "POST", uri: "/api/set/", code: 204)
operation Set {
    input := {
        @required
        set: MySet
    }
}

@uniqueItems
list MySet {
    member: String
}

@http(method: "POST", uri: "/api/list/", code: 204)
operation List {
    input := {
        @required
        list: MyList
    }
}

list MyList {
    member: String
}

@http(method: "POST", uri: "/api/map/", code: 204)
operation Map {
    input := {
        @required
        value: MyMap
    }
}

map MyMap {
    key: String
    value: String
}

@http(method: "POST", uri: "/api/option/", code: 204)
operation Option {
    input := {
        value: String
    }
}

string String

structure TestReservedNamespaceImport {
    package: MyPackageString
}

// Regression test for https://github.com/disneystreaming/smithy4s/issues/1601
@MyPackageStringTrait("test")
string TestReservedNamespaceTrait

// trait def
@trait
structure reservedKeywordStructTrait {
    @required
    implicit: String
    package: Packagee
}

// note: can't name this Package because of #1343
structure Packagee {
    class: Integer
}

// trait usages
@reservedKeywordStructTrait(implicit: "demo", package: {
    class: 42
})
structure ReservedKeywordTraitExampleStruct {
    @reservedKeywordStructTrait(implicit: "demo", package: {
        class: 42
    })
    member: String
}

@reservedKeywordStructTrait(implicit: "demo", package: {
    class: 42
})
union ReservedKeywordTraitExampleUnion {
    @reservedKeywordStructTrait(implicit: "demo", package: {
        class: 42
    })
    member: String
}

@reservedKeywordStructTrait(implicit: "demo", package: {
    class: 42
})
string ReservedKeywordTraitExamplePrimitive

@reservedKeywordStructTrait(implicit: "demo", package: {
    class: 42
})
list ReservedKeywordTraitExampleCollection {
    @reservedKeywordStructTrait(implicit: "demo", package: {
        class: 42
    })
    member: String
}

// trait def, as a union
@trait
union reservedKeywordUnionTrait {
    package: PackageUnion
}

union PackageUnion {
    class: Integer
}

// trait usages
@reservedKeywordUnionTrait(package: {
    class: 42
})
structure ReservedKeywordTraitExampleStruct {
    @reservedKeywordUnionTrait(package: {
        class: 42
    })
    member: String
}

enum KeywordEnum {
    implicit
    package = "class"
}

structure Scala3ReservedKeywords {
    export: String
    enum: String
}

@smithy4s.meta#adt
@trait
union class {
    package: AdtStruct
}

structure AdtStruct {}

// Regression test for https://github.com/disneystreaming/smithy4s/issues/1601
@class(package: {

})
string ReservedNameUnionTrait
