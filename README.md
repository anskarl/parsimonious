```
┌─┐┌─┐┬─┐┌─┐┬┌┬┐┌─┐┌┐┌┬┌─┐┬ ┬┌─┐
├─┘├─┤├┬┘└─┐│││││ ││││││ ││ │└─┐
┴  ┴ ┴┴└─└─┘┴┴ ┴└─┘┘└┘┴└─┘└─┘└─┘
```
_Parsimonious_ is a helper library for encoding/decoding Apache Thrift and Twitter Scrooge classes to Spark Dataframes, 
Jackson JSON and Apache Flink TypeSerializer. 

  - The implementation for Spark is based on [airbnb-spark-thrift](https://github.com/airbnb/airbnb-spark-thrift/tree/nwparker/convV2).
  - The implementation for Flink support is inspired by [findify flink-protobuf](https://github.com/findify/flink-protobuf).

Important features:

  - Supports all Thrift types, including unions and binary types. 
  - Supports both Twitter Scrooge and Apache Thrift generated classes.
  - Supports nested and recursive structures. Please note, for Spark, recursive structures are being serialized to bytes.
  - For JSON, when a Thrift map does not have string type as a key (e.g., a struct) then _Parsimonious_ will convert it to a sequence of key, value tuples (the opposite during decoding to Thrift is also supported).
  
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

See the schema and preview the first 5 rows:

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

df.show(numRows = 5, truncate = false)
// +---------+----+-----+-----+-----+----+----+-----+----+
// |reqStr   |str |int16|int32|int64|dbl |byt |bl   |bin |
// +---------+----+-----+-----+-----+----+----+-----+----+
// |index: 1 |null|null |1    |null |null|null|false|null|
// |index: 2 |null|null |2    |null |null|null|false|null|
// |index: 3 |null|null |3    |null |null|null|false|null|
// |index: 4 |null|null |4    |null |null|null|false|null|
// |index: 5 |null|null |5    |null |null|null|false|null|
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

// Print the first 5 `BasicDummy`
decodedInputSeq.take(5).foreach(println)

// BasicDummy(reqStr:index: 1, int32:1, bl:false)
// BasicDummy(reqStr:index: 2, int32:2, bl:false)
// BasicDummy(reqStr:index: 3, int32:3, bl:false)
// BasicDummy(reqStr:index: 4, int32:4, bl:false)
// BasicDummy(reqStr:index: 5, int32:5, bl:false)
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

### Scrooge Thrift interoperability with Apache Spark

Create a Spark Dataframe:

```scala
import org.apache.spark.sql.types.StructType
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import com.github.anskarl.parsimonious.scrooge._
import com.github.anskarl.parsimonious.scrooge.spark._

// Assume that we have SparkSession initialized as `spark`
// To extract the schema (i.e., Apache Spark org.apache.spark.sql.types.StructType)
val sparkSchema: StructType = ScroogeRowConverter.extractSchema(classOf[BasicDummy])

// Need to create once union builders, helper class to extract the schema and 
// create builder for Scrooge Union (com.twitter.scrooge.ThriftUnion)
implicit val unionBuilders = UnionBuilders.create(classOf[BasicDummy])


// Example collection of BasicDummy instances
val exampleData = for (index <- 1 to 100) 
  yield new BasicDummy(reqStr = s"index: ${index}", int32 = index, bl = index % 10 == 0)

// Convert BasicDummy to org.apache.spark.sql.Row
val rowSeq: Seq[Row] = exampleData.map(_.toRow)

// Create RDD[Row]
val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)

// Create the corresponding dataframe
val df: DataFrame = spark.createDataFrame(rowRDD, sparkSchema)
```

See the schema and preview the first 5 rows:
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

df.show(numRows = 5, truncate = false)
// +---------+----+-----+-----+-----+----+----+-----+----+
// |reqStr   |str |int16|int32|int64|dbl |byt |bl   |bin |
// +---------+----+-----+-----+-----+----+----+-----+----+
// |index: 1 |null|null |1    |null |null|null|false|null|
// |index: 2 |null|null |2    |null |null|null|false|null|
// |index: 3 |null|null |3    |null |null|null|false|null|
// |index: 4 |null|null |4    |null |null|null|false|null|
// |index: 5 |null|null |5    |null |null|null|false|null|
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
```

### Scrooge Thrift interoperability with Jackson for JSON support

Encode/Decode Scrooge generated classes to/from Jackson node:

```scala
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.anskarl.parsimonious.{BasicDummy, EnumDummy, NestedDummy, PropertyValue}
import com.github.anskarl.parsimonious.scrooge._
import java.nio.ByteBuffer

val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

val sampleBasicDummy = BasicDummy(
    reqStr = "required 101",
    str = Option("optional 101"),
    int16 = Option(16.toShort),
    int32 = Option(32),
    int64 = Option(64L),
    dbl = Option(101.101),
    byt = Option(8.toByte),
    bl = Option(false),
    bin = Option(ByteBuffer.wrap("101".getBytes("UTF-8"))),
    listNumbersI32 = Option(List(1,2,3)),
    listNumbersDouble = Option(List(1.1,2.2,3.3)),
    setNumbersI32 = Option(Set(1,2,3)),
    setNumbersDouble = Option(Set(1.1,2.2,3.3)),
    enm = Option(EnumDummy.Maybe),
    listStruct = Option(List(PropertyValue("prop1", "val1"),PropertyValue("prop2", "val2"))),
    mapPrimitives = Option(Map(1 -> 1.1, 2 -> 2.2)),
    mapStructKey = Option(Map(PropertyValue("prop1", "val1") -> 1.1, PropertyValue("prop2", "val2") -> 2.2)),
    mapPrimitivesStr = Option(Map("one" -> 1.0, "two" -> 2.0))
  )

// Encode to JSON
val encoded = ScroogeJsonConverter.convert(sampleBasicDummy)
println(encoded.toPrettyString)
```

Will print the following:
```json
{
  "reqStr" : "required 101",
  "str" : "optional 101",
  "int16" : 16,
  "int32" : 32,
  "int64" : 64,
  "dbl" : 101.101,
  "byt" : 8,
  "bl" : false,
  "bin" : "MTAx",
  "listNumbersI32" : [ 1, 2, 3 ],
  "listNumbersDouble" : [ 1.1, 2.2, 3.3 ],
  "setNumbersI32" : [ 1, 2, 3 ],
  "setNumbersDouble" : [ 1.1, 2.2, 3.3 ],
  "enm" : "Maybe",
  "listStruct" : [ {
    "property" : "prop1",
    "value" : "val1"
  }, {
    "property" : "prop2",
    "value" : "val2"
  } ],
  "mapPrimitives" : [ {
    "key" : 1,
    "value" : 1.1
  }, {
    "key" : 2,
    "value" : 2.2
  } ],
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
  "mapPrimitivesStr" : {
    "one" : 1.0,
    "two" : 2.0
  }
}
```
Recall to Apache Thrift to JSON conversion, the type of key in both `mapPrimitives` and `mapStructKey` is not string.
In such cases _Parsimonious_ converts them to lists of structs with the pair of fields
`"key"` and `"value"`. That convention, is being performed since JSON does not support
maps having keys with type different of string.

To decode from JSON back to Scrooge class:
```scala

// Need to create once union builders, helper class to extract the schema and 
// create builder for Scrooge Union (com.twitter.scrooge.ThriftUnion)
implicit val unionBuilders = UnionBuilders.create(classOf[BasicDummy])

// Decode to Scrooge class
val decoded: BasicDummy = JsonScroogeConverter.convert(classOf[BasicDummy], encodedJson)
```

## Dependencies

Version variants published in `oss.sonatype.org`
 - scala_version: `2.12`, `2.13`
 - thrift_version: `thrift_0.10`, `thrift_0.13`
 - spark_version: `spark2` (i.e., 2.4.x), `spark3` (i.e., 3.1.x)
 - parsimonious_version: e.g., `0.4.0`

#### Maven

***parsimonious-commons***:
```
<dependency>
  <groupId>com.github.anskarl</groupId>
  <artifactId>parsimonious-commons_[scala_version]</artifactId>
  <version>[thrift_version]-[parsimonious_version]</version>
</dependency>
```

***parsimonious-jackson***:
```
<dependency>
  <groupId>com.github.anskarl</groupId>
  <artifactId>parsimonious-jackson_[scala_version]</artifactId>
  <version>[thrift_version]-[parsimonious_version]</version>
</dependency>
```

***parsimonious-spark***:
```
<dependency>
  <groupId>com.github.anskarl</groupId>
  <artifactId>parsimonious-spark_[scala_version]</artifactId>
  <version>[thrift_version]_[spark_version]-[parsimonious_version]</version>
</dependency>
```

***parsimonious-scrooge-commons***:
```
<dependency>
  <groupId>com.github.anskarl</groupId>
  <artifactId>parsimonious-scrooge-commons_[scala_version]</artifactId>
  <version>[thrift_version]-[parsimonious_version]</version>
</dependency>
```


***parsimonious-scrooge-jackson***:
```
<dependency>
  <groupId>com.github.anskarl</groupId>
  <artifactId>parsimonious-scrooge-jackson_[scala_version]</artifactId>
  <version>[thrift_version]-[parsimonious_version]</version>
</dependency>
```

***parsimonious-scrooge-spark***:
```
<dependency>
  <groupId>com.github.anskarl</groupId>
  <artifactId>parsimonious-scrooge-spark_[scala_version]</artifactId>
  <version>[thrift_version]_[spark_major_version]-[parsimonious_version]</version>
</dependency>
```

#### SBT

See the [official docs](https://www.scala-sbt.org/1.x/docs/Resolvers.html) to configure Sonatype (snapshots) 
resolver in your SBT project. For example:
```
resolvers += Resolver.sonatypeRepo("public") //  (or “snapshots”, “staging”, “releases”) 
```

***parsimonious-commons***:
```
"com.github.anskarl" %% "parsimonious-commons" % "[thrift_version]-[parsimonious_version]"
```

***parsimonious-jackson***:
```
"com.github.anskarl" %% "parsimonious-jackson" % "[thrift_version]-[parsimonious_version]"
```

***parsimonious-spark***:
```
"com.github.anskarl" %% "parsimonious-spark" % "[thrift_version]_[spark_version]-[parsimonious_version]"
```

***parsimonious-scrooge-commons***:
```
"com.github.anskarl" %% "parsimonious-scrooge-commons" % "[thrift_version]-[parsimonious_version]"
```


***parsimonious-scrooge-jackson***:
```
"com.github.anskarl" %% "parsimonious-scrooge-jackson" % "[thrift_version]-[parsimonious_version]"
```

***parsimonious-scrooge-spark***:
```
"com.github.anskarl" %% "parsimonious-scrooge-spark" % "[thrift_version]_[spark_version]-[parsimonious_version]"
```

## Build from sources

To build parsimonious from sources you will need an SBT version 1.6+. The build can be parameterized for the following environment variables:
  - `THRIFT_VERSION`: e.g., 0.13.0. Default is `0.10.0`. Please note that for Scrooge modules is always version `0.10.0`.
  - `SPARK_PROFILE`: can be either `spark2` or `spark3` (default is `spark3`).
    * In `spark2` parsimonious is build for Spark v2.4.6, Hadoop v2.10.0 and Parquet v1.10.1.
    * In `spark3` parsimonious is build for Spark v3.2.0, Hadoop v3.3.1 and Parquet v1.12.2.

For all variants of `THRIFT_VERSION` and `SPARK_PROFILE`, parsimonious can be cross-build for Scala 2.12 and 2.13.

There are several sbt command aliases to build any of the parsimonious modules separately or all together. 
For a complete list see the `.sbtrc` file. 

##### All modules using default settings

The following command will build all modules, using default version of Thrift (0.10) and Spark (3.2).

```shell
sbt build-all
```

##### All modules with custom Thrift and Spark version

The following command will build all modules for Thrift version 0.13.0 and Spark 2 (2.4.x).

```shell
THRIFT_VERSION=0.13.0 SPARK_PROFILE=spark2 sbt build-all
```


##### Specific module using default settings

The following command will build build-scrooge-jackson module (translates Scrooge classes to/from JSON),
using default version of Thrift (0.10) and Spark (3.2).

```shell
sbt build-scrooge-jackson
```

##### Specific module custom Thrift and Spark version

The following command will build build-spark module (translates Apache Thrift classes to/from Spark Dataframe),
using Thrift version 0.13 and Spark 2 (2.4.x).

```shell
THRIFT_VERSION=0.13.0 SPARK_PROFILE=spark2 sbt build-spark
```