package com.github.anskarl.parsimonious.srcooge.spark

import com.github.anskarl.parsimonious.scrooge.UnionBuilders
import com.github.anskarl.parsimonious.scrooge.models._
import com.github.anskarl.parsimonious.scrooge.spark.{RowScroogeConverter, ScroogeRowConverter}
import com.github.anskarl.parsimonious.spark.SparkSessionTestSuite
import com.twitter.scrooge.ThriftStruct
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.reflect.runtime.{universe => ru}
import java.nio.ByteBuffer
import scala.reflect.ClassTag

class Simple extends AnyWordSpecLike with SparkSessionTestSuite with Matchers {


  val instanceBasic: BasicDummy = BasicDummy(
    reqStr = "required string",
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

  val instanceBasic2: BasicDummy = instanceBasic.copy(reqStr = "required string 2")

  val instanceNested: NestedDummy = NestedDummy("aaaa", instanceBasic)

//  val ur0 = Some(UnionRecursiveDummy.Bl(true))
//  val ur1 = Some(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Bl(true)))
  val ur2: Option[UnionRecursiveDummy.Ur] = Some(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Bl(true))))

  val instanceComplex: ComplexDummy = ComplexDummy(
    bdList = Some(List(instanceBasic, instanceBasic2)),
    bdSet = Some(Set(instanceBasic, instanceBasic2)),
    strToBdMap = Some(Map("a" -> instanceBasic, "b" -> instanceBasic2 )),
    bdToStrMap = Some(Map(instanceBasic -> "a", instanceBasic2 -> "b")),
    unionDummy = Some(UnionDummy.Dbl(2.0)),
    unionRecursiveDummy = ur2
  )

  private def checkFor[T <: ThriftStruct with Product: ClassTag: ru.TypeTag](clazz: Class[T], instance: T): Assertion ={
    spark.sparkContext.setLogLevel("ERROR")

    val schema = ScroogeRowConverter.extractSchema(clazz)

    val row = ScroogeRowConverter.convert(instance)

    val rdd = spark.sparkContext.parallelize(Seq(row))
    val df = spark.createDataFrame(rdd, schema)

    // DECODE
    implicit val unionBuilders: UnionBuilders = UnionBuilders.create(clazz)
    val decoded = RowScroogeConverter.convert(clazz,df.head())
    decoded mustEqual instance
  }

  "Scrooge - Basic encode/decode functionality" should {
    "encode/decode Basic Scrooge class to Spark Rows" in {
      checkFor(classOf[BasicDummy], instanceBasic)
    }

    "encode/decode Nested Scrooge class to Spark Rows" in {
      checkFor(classOf[NestedDummy], instanceNested)
    }

    "encode/decode (recursive) Union Scrooge class to Spark Rows" in {
      checkFor(classOf[ComplexDummy], instanceComplex)
    }
  }

}
