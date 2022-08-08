package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious.ClassTBaseType
import com.github.anskarl.parsimonious.pojo.models._
import com.github.anskarl.parsimonious.spark.Converters._
import org.apache.thrift.{TBase, TFieldIdEnum}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.nio.ByteBuffer
import scala.util.chaining.scalaUtilChainingOps
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class ThriftMinimalSparkTest extends AnyWordSpecLike with SparkSessionTestSuite with Matchers {

  val instanceBasic: BasicDummy = new BasicDummy().tap{ d =>
    d.setReqStr("required string")
    d.setStr("optional string")
    d.setInt16(16.toShort)
    d.setInt32(32)
    d.setInt64(64L)
    d.setDbl(1.1)
    d.setByt(8.toByte)
    d.setBl(true)
    d.setBin(ByteBuffer.wrap("string value encoded as binary".getBytes("UTF-8")))
    d.setListNumbersI32(Seq(1,2,3,4,5).map(java.lang.Integer.valueOf).asJava)
    d.setListNumbersDouble(Seq(1.1, 2.2, 3.3).map(java.lang.Double.valueOf).asJava)
    d.setSetNumbersI32(Set(1,1,2,2,3,3).map(java.lang.Integer.valueOf).asJava)
    d.setSetNumbersDouble(Set(1.1,1.1,2.2,2.2,3.3,3.3).map(java.lang.Double.valueOf).asJava)
    d.setEnm(EnumDummy.MAYBE)
    d.setListStruct(Seq(new PropertyValue("prop1", "val1"), new PropertyValue("prop2", "val2")).asJava)
    d.setMapPrimitives(Map(java.lang.Integer.valueOf(1) -> java.lang.Double.valueOf(1.1), java.lang.Integer.valueOf(2) -> java.lang.Double.valueOf(2.2)).asJava)
    d.setMapStructKey(Map(new PropertyValue("prop1", "val1") -> java.lang.Double.valueOf(1.1), new PropertyValue("prop2", "val2") -> java.lang.Double.valueOf(2.2)).asJava)
    d.setMapPrimitivesStr(Map("a" -> java.lang.Double.valueOf(1.1), "b" -> java.lang.Double.valueOf(2.2)).asJava)
  }

  val anotherInstanceBasic: BasicDummy = instanceBasic.deepCopy().setReqStr("another required string")
  val instanceNested: NestedDummy = new NestedDummy("nested struct", instanceBasic)

  val instanceUnionRecursive: UnionRecursiveDummy = new UnionRecursiveDummy().tap{
    _.setUr(new UnionRecursiveDummy().tap{
      _.setBl(java.lang.Boolean.valueOf(true))
    })
  }

  val instanceComplex: ComplexDummy = new ComplexDummy().tap{ d =>
    d.setBdList(List(instanceBasic, anotherInstanceBasic).asJava)
    d.setBdSet(Set(instanceBasic, anotherInstanceBasic).asJava)
    d.setStrToBdMap(Map("basic key" -> instanceBasic, "another basic key" -> anotherInstanceBasic).asJava)
    d.setBdToStrMap(Map(instanceBasic -> "basic value", anotherInstanceBasic -> "another basic value").asJava)
    d.setUnionDummy(new UnionDummy().tap(_.setDbl(2.0)))
    d.setUnionRecursiveDummy(instanceUnionRecursive)
  }

  private def checkFor[F <: TFieldIdEnum : ClassTag](clazz: ClassTBaseType, instance: TBase[_ <: TBase[_, _], F]): Assertion ={
    spark.sparkContext.setLogLevel("ERROR")

    val schema = ThriftRowConverter.extractSchema(clazz)

    val row = ThriftRowConverter.convert(instance)

    val rdd = spark.sparkContext.parallelize(Seq(row))
    val df = spark.createDataFrame(rdd, schema)

    // DECODE
    val decoded = df.head().as(clazz)

    assert(decoded.equals(instance))
  }

  "Thrift - Basic encode/decode functionality" should {
    "encode/decode Basic Thrift class to Spark Rows" in {
      checkFor(classOf[BasicDummy], instanceBasic)
    }

    "encode/decode Nested Thrift class to Spark Rows" in {
      checkFor(classOf[NestedDummy], instanceNested)
    }

    "encode/decode (recursive) Union Thrift class to Spark Rows" in {
      checkFor(classOf[ComplexDummy], instanceComplex)
    }
  }
}
