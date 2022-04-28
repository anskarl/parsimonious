namespace java com.github.anskarl.parsimonious

enum EnumDummy {
    YES
    NO
    MAYBE
}

struct PropertyValue{
    1: required string property
    2: required string value
}

struct BasicDummy {
    1: required string reqStr
    2: optional string str
    3: optional i16 int16
    4: optional i32 int32
    5: optional i64 int64
    6: optional double dbl
    7: optional i8 byt
    8: optional bool bl
    9: optional binary bin
    10: optional list<i32> listNumbersI32
    11: optional list<double> listNumbersDouble
    12: optional set<i32> setNumbersI32
    13: optional set<double> setNumbersDouble
    14: optional EnumDummy enm
    15: optional list<PropertyValue> listStruct
    16: optional map<i32, double> mapPrimitives
    17: optional map<PropertyValue, double> mapStructKey
    18: optional map<string, double> mapPrimitivesStr
}

struct NestedDummy {
    1: required string reqStr
    2: required BasicDummy basic
    3: optional BasicDummy basicOpt
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