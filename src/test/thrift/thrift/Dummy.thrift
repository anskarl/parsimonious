namespace java io.github.anskarl.parsimonious

enum EnumDummy {
    YES
    NO
    MAYBE
}

struct BasicDummy {
    1: required string reqStr
    2: optional string str
    3: optional i16 int16
    4: optional i32 int32
    5: optional i64 int64
    6: optional double dbl
    7: optional byte byt
    8: optional bool bl
    9: optional binary bin
}

union UnionDummy {
    1: string str
    2: double dbl
}

// IMPORTANT: Please note that Schema in Spark Dataframes/Datasets cannot support recursive structures.
// A recursively defined struct/union is converted to bytes (binary)

union UnionRecursiveDummy {
    1: bool bl
    2: UnionRecursiveDummy ur
}

struct ComplexDummy {
    1: optional list<BasicDummy> bdList
    2: optional set<BasicDummy> bdSet
    3: optional map<string, BasicDummy> strToBdMap
    4: optional map<BasicDummy, string> bdToStrMap
    5: optional EnumDummy enumDummy
    6: optional UnionDummy unionDummy
    7: optional UnionRecursiveDummy unionRecursiveDummy
}