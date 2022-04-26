```
┌─┐┌─┐┬─┐┌─┐┬┌┬┐┌─┐┌┐┌┬┌─┐┬ ┬┌─┐
├─┘├─┤├┬┘└─┐│││││ ││││││ ││ │└─┐
┴  ┴ ┴┴└─└─┘┴┴ ┴└─┘┘└┘┴└─┘└─┘└─┘
```
_Parsimonious_ is a helper library for encoding/decoding Apache Thrift classes to Spark Dataframes and Jackson JSON. 
The implementation for Spark is based on https://github.com/airbnb/airbnb-spark-thrift/tree/nwparker/convV2.

Important features:

  - Supports all Thrift types, including unions and binary types. 
  - Supports nested and recursive structures. Please note, for Spark, recursive structures are being serialized to bytes.
  - For Json, when a Thrift map does not have string type as a key (e.g., a struct) then _Parsimonious_ will convert it to a sequence of key, value tuples (the opposite during decoding to Thrift is also supported).

## Example usage

More detailed examples can be found in unit tests (including nested and recursive structures).

Assume that we have an Apache Thrift struct named `BasicDummy`

```thrift
namespace java com.github.anskarl.parsimonious

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
```

### Apache Thrift interoperability with Apache Spark

Create a Spark Dataframe:

```scala
import org.apache.spark.sql.types.StructType
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import com.github.anskarl.parsimonious._
import com.github.anskarl.parsimonious.spark.ThriftRowConverter

// Assume that we have SparkSession initialized as `spark`
// To extract the schema (i.e., Apache Spark org.apache.spark.sql.types.StructType)
val sparkSchema: StructType = ThriftRowConverter.extractSchema(classOf[BasicDummy])

// Example collection of BasicDummy instances
val exampleData = 
    for (index <- 1 to 100) 
        yield new BasicDummy().setReqStr(s"index: ${index}").setInt32(index).setBl(index % 10 == 0)

// Convert BasicDummy to org.apache.spark.sql.Row
val rowSeq: Seq[Row] = exampleData.map(_.toRow)

// Create RDD[Row]
val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)

// Create the corresponding dataframe
val df: DataFrame = spark.createDataFrame(rowRDD, sparkSchema)
```

See the schema and preview the first 20 rows:

```scala
// Print dataframe schema
df.printSchema()

// Will print:
// root
// |-- reqStr: string (nullable = false)
// |-- str: string (nullable = true)
// |-- int16: short (nullable = true)
// |-- int32: integer (nullable = true)
// |-- int64: long (nullable = true)
// |-- dbl: double (nullable = true)
// |-- byt: byte (nullable = true)
// |-- bl: boolean (nullable = true)
// |-- bin: binary (nullable = true)

df.show(numRows = 20, truncate = false)
// +---------+----+-----+-----+-----+----+----+-----+----+
// |reqStr   |str |int16|int32|int64|dbl |byt |bl   |bin |
// +---------+----+-----+-----+-----+----+----+-----+----+
// |index: 1 |null|null |1    |null |null|null|false|null|
// |index: 2 |null|null |2    |null |null|null|false|null|
// |index: 3 |null|null |3    |null |null|null|false|null|
// |index: 4 |null|null |4    |null |null|null|false|null|
// |index: 5 |null|null |5    |null |null|null|false|null|
// |index: 6 |null|null |6    |null |null|null|false|null|
// |index: 7 |null|null |7    |null |null|null|false|null|
// |index: 8 |null|null |8    |null |null|null|false|null|
// |index: 9 |null|null |9    |null |null|null|false|null|
// |index: 10|null|null |10   |null |null|null|true |null|
// |index: 11|null|null |11   |null |null|null|false|null|
// |index: 12|null|null |12   |null |null|null|false|null|
// |index: 13|null|null |13   |null |null|null|false|null|
// |index: 14|null|null |14   |null |null|null|false|null|
// |index: 15|null|null |15   |null |null|null|false|null|
// |index: 16|null|null |16   |null |null|null|false|null|
// |index: 17|null|null |17   |null |null|null|false|null|
// |index: 18|null|null |18   |null |null|null|false|null|
// |index: 19|null|null |19   |null |null|null|false|null|
// |index: 20|null|null |20   |null |null|null|true |null|
// +---------+----+-----+-----+-----+----+----+-----+----+
```

Collect the rows and convert back to `BasicDummy`:

```scala
// Collect rows
val dfRows: Array[Row] = df.collect()

// Convert the collected rows back to BasicDummy instances
val decodedInputSeq: Seq[BasicDummy] = dfRows
    .map(row => row.as(classOf[BasicDummy])
    .toSeq

// Print the first 20 `BasicDummy`
decodedInputSeq.take(20).foreach(println)

// BasicDummy(reqStr:index: 1, int32:1, bl:false)
// BasicDummy(reqStr:index: 2, int32:2, bl:false)
// BasicDummy(reqStr:index: 3, int32:3, bl:false)
// BasicDummy(reqStr:index: 4, int32:4, bl:false)
// BasicDummy(reqStr:index: 5, int32:5, bl:false)
// BasicDummy(reqStr:index: 6, int32:6, bl:false)
// BasicDummy(reqStr:index: 7, int32:7, bl:false)
// BasicDummy(reqStr:index: 8, int32:8, bl:false)
// BasicDummy(reqStr:index: 9, int32:9, bl:false)
// BasicDummy(reqStr:index: 10, int32:10, bl:true)
// BasicDummy(reqStr:index: 11, int32:11, bl:false)
// BasicDummy(reqStr:index: 12, int32:12, bl:false)
// BasicDummy(reqStr:index: 13, int32:13, bl:false)
// BasicDummy(reqStr:index: 14, int32:14, bl:false)
// BasicDummy(reqStr:index: 15, int32:15, bl:false)
// BasicDummy(reqStr:index: 16, int32:16, bl:false)
// BasicDummy(reqStr:index: 17, int32:17, bl:false)
// BasicDummy(reqStr:index: 18, int32:18, bl:false)
// BasicDummy(reqStr:index: 19, int32:19, bl:false)
// BasicDummy(reqStr:index: 20, int32:20, bl:true)
```

### Apache Thrift interoperability with Jackson for JSON support

Encode/Decode Apache Thrift POJO class to/from Jackson node:

```scala
import com.github.anskarl.parsimonious._
import com.github.anskarl.parsimonious.json._
import scala.collection.JavaConverters._

// create POJO
val basicDummy = new BasicDummy()
  basicDummy.setReqStr("required 101")
  basicDummy.setStr("optional 101")
  basicDummy.setInt16(101.toShort)
  basicDummy.setInt32(101)
  basicDummy.setInt64(101L)
  basicDummy.setDbl(101.101)
  basicDummy.setByt(8.toByte)
  basicDummy.setBl(false)
  basicDummy.setBin("101".getBytes("UTF-8"))
  basicDummy.setListNumbersI32(List(1,2,3).map(java.lang.Integer.valueOf).asJava)
  basicDummy.setListNumbersDouble(List(1.1,2.2,3.3).map(java.lang.Double.valueOf).asJava)
  basicDummy.setSetNumbersI32(Set(1,2,3).map(java.lang.Integer.valueOf).asJava)
  basicDummy.setSetNumbersDouble(Set(1.1,2.2,3.3).map(java.lang.Double.valueOf).asJava)
  basicDummy.setEnm(EnumDummy.MAYBE)
  basicDummy.setListStruct(List(new PropertyValue("prop1", "val1"), new PropertyValue("prop2", "val2")).asJava)
  basicDummy.setMapPrimitives(
    Map(
      java.lang.Integer.valueOf(1) -> java.lang.Double.valueOf(1.1),
      java.lang.Integer.valueOf(2) -> java.lang.Double.valueOf(2.2)
    ).asJava
  )
basicDummy.setMapStructKey(Map(
    new PropertyValue("prop1", "val1") -> java.lang.Double.valueOf(1.1),
    new PropertyValue("prop2", "val2") -> java.lang.Double.valueOf(2.2)
  ).asJava)
basicDummy.setMapPrimitivesStr(Map("one" -> java.lang.Double.valueOf(1.0), "two" -> java.lang.Double.valueOf(2.0)).asJava)


// .. etc

// Encode to Json
val encoded: ObjectNode = ThriftJsonConverter.convert(basicDummy)
println(encoded.toPrettyString)
```
Will print the following:
```json
{
    "reqStr" : "required 101",
    "str" : "optional 101",
    "int16" : 101,
    "int32" : 101,
    "int64" : 101,
    "dbl" : 101.101,
    "byt" : 8,
    "bl" : false,
    "bin" : "MTAx",
    "listNumbersI32" : [ 1, 2, 3 ],
    "listNumbersDouble" : [ 1.1, 2.2, 3.3 ],
    "setNumbersI32" : [ 1, 2, 3 ],
    "setNumbersDouble" : [ 1.1, 2.2, 3.3 ],
    "enm" : "MAYBE",
    "listStruct" : [ {
      "property" : "prop1",
      "value" : "val1"
    }, {
      "property" : "prop2",
      "value" : "val2"
    } ],
    "mapPrimitives":[{"key":1,"value":1.1},{"key":2,"value":2.2}],
    "mapStructKey" : [ {
      "key" : {
        "property" : "prop1",
        "value" : "val1"
      },
      "value" : 1.1
    }, {
      "key" : {
        "property" : "prop2",
        "value" : "val2"
      },
      "value" : 2.2
    } ],
    "mapPrimitivesStr": {"one": 1.0, "two": 2.0}
}
```
Please note that the type of key in both `mapPrimitives` and `mapStructKey` is not string. 
In such cases _Parsimonious_ converts them to lists of structs with the pair of fields 
`"key"` and `"value"`. That convention, is being performed since JSON does not support 
maps having keys with type different of string.  

To decode from JSON back to Thrift POJO:

```scala
// Decode from Json
val decoded: BasicDummy = JsonThriftConverter.convert(classOf[BasicDummy], encoded)
```