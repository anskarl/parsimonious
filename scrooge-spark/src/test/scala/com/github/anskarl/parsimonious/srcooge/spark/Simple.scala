package com.github.anskarl.parsimonious.srcooge.spark

import com.github.anskarl.parsimonious._
import com.github.anskarl.parsimonious.scrooge.spark.ScroogeRowConverter
import com.github.anskarl.parsimonious.spark.SparkSessionTestSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.nio.ByteBuffer

class Simple extends AnyWordSpecLike with SparkSessionTestSuite with Matchers {
//    val schema = ScroogeRowConverter.extractSchema(classOf[ComplexDummy])
//    println(schema.treeString)

    "AAA Basic encode/decode functionality" should {
        "encode/decode Thrift class to Spark Rows" in {
            spark.sparkContext.setLogLevel("ERROR")
            val instanceBd = BasicDummy(
                reqStr = "req straaaa",
                str = Option("foo"),
                int16 = Option(16.toShort),
                int32 = Option(32),
                int64 = Option(64L),
                dbl = Option(1.1),
                byt = Option(8.toByte),
                bl = Option(true),
                bin = Option(ByteBuffer.wrap("foo-bar".getBytes("UTF-8"))),
                listNumbersI32 = Option(Seq(1,2,3,4,5)),
                listNumbersDouble = Option(Seq(1.1, 2.2, 3.3)),
                setNumbersI32 = Option(Set(1,1,2,2,3,3)),
                setNumbersDouble = Option(Set(1.1,1.1,2.2,2.2,3.3,3.3)),
                enm = Option(EnumDummy.Maybe),
                listStruct = Option(Seq(PropertyValue("p1", "v1"), PropertyValue("p2", "v2"))),
                mapPrimitives = Option(Map(1->1.1, 2->2.2)),
                mapStructKey = Option(Map(PropertyValue("p1", "v1") -> 1.1, PropertyValue("p2", "v2") -> 2.2)),
                mapPrimitivesStr = Option(Map("a"->1.1, "b"->2.2)),

            )
            val instanceNested = NestedDummy("aaaa", instanceBd)
            val instance = ComplexDummy(unionDummy = Some(UnionDummy.Dbl(2.0)))
            val schema = ScroogeRowConverter.extractSchema(classOf[ComplexDummy])
            val row = ScroogeRowConverter.convert(instance)

            val rdd = spark.sparkContext.parallelize(Seq(row))
            val df = spark.createDataFrame(rdd, schema)
            df.show()
            assert(true)
        }
    }

}
