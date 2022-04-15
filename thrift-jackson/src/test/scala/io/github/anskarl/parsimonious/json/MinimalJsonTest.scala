package io.github.anskarl.parsimonious.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.anskarl.parsimonious.{BasicDummy, EnumDummy, NestedDummy, PropertyValue}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.chaining.scalaUtilChainingOps
import scala.collection.JavaConverters._

class MinimalJsonTest extends AnyWordSpecLike  with Matchers {
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val sampleJson = s"""
      |{
      |  "reqStr" : "required 101",
      |  "basic" : {
      |    "reqStr" : "required 101",
      |    "str" : "optional 101",
      |    "int16" : 101,
      |    "int32" : 101,
      |    "int64" : 101,
      |    "dbl" : 101.101,
      |    "byt" : 8,
      |    "bl" : false,
      |    "bin" : "MTAx",
      |    "listNumbersI32" : [ 1, 2, 3 ],
      |    "listNumbersDouble" : [ 1.1, 2.2, 3.3 ],
      |    "setNumbersI32" : [ 1, 2, 3 ],
      |    "setNumbersDouble" : [ 1.1, 2.2, 3.3 ],
      |    "enm" : "MAYBE",
      |    "listStruct" : [ {
      |      "property" : "prop1",
      |      "value" : "val1"
      |    }, {
      |      "property" : "prop2",
      |      "value" : "val2"
      |    } ],
      |    "mapPrimitives":[{"key":1,"value":1.1},{"key":2,"value":2.2}],
      |    "mapStructKey" : [ {
      |      "key" : {
      |        "property" : "prop1",
      |        "value" : "val1"
      |      },
      |      "value" : 1.1
      |    }, {
      |      "key" : {
      |        "property" : "prop2",
      |        "value" : "val2"
      |      },
      |      "value" : 2.2
      |    } ],
      |    "mapPrimitivesStr": {"one": 1.0, "two": 2.0}
      |  }
      |}
      |""".stripMargin

  private val sampleJsonNode = mapper.readTree(sampleJson)

  private val sampleBasicDummy = new BasicDummy().tap{ bd =>
    bd.setReqStr("required 101")
    bd.setStr("optional 101")
    bd.setInt16(101.toShort)
    bd.setInt32(101)
    bd.setInt64(101L)
    bd.setDbl(101.101)
    bd.setByt(8.toByte)
    bd.setBl(false)
    bd.setBin("101".getBytes("UTF-8"))
    bd.setListNumbersI32(List(1,2,3).map(java.lang.Integer.valueOf).asJava)
    bd.setListNumbersDouble(List(1.1,2.2,3.3).map(java.lang.Double.valueOf).asJava)
    bd.setSetNumbersI32(Set(1,2,3).map(java.lang.Integer.valueOf).asJava)
    bd.setSetNumbersDouble(Set(1.1,2.2,3.3).map(java.lang.Double.valueOf).asJava)
    bd.setEnm(EnumDummy.MAYBE)
    bd.setListStruct(List(new PropertyValue("prop1", "val1"), new PropertyValue("prop2", "val2")).asJava)
    bd.setMapPrimitives(
      Map(
        java.lang.Integer.valueOf(1) -> java.lang.Double.valueOf(1.1),
        java.lang.Integer.valueOf(2) -> java.lang.Double.valueOf(2.2)
      ).asJava
    )
    bd.setMapStructKey(Map(
      new PropertyValue("prop1", "val1") -> java.lang.Double.valueOf(1.1),
      new PropertyValue("prop2", "val2") -> java.lang.Double.valueOf(2.2)
    ).asJava)
    bd.setMapPrimitivesStr(Map("one" -> java.lang.Double.valueOf(1.0), "two" -> java.lang.Double.valueOf(2.0)).asJava)
  }

  private val sampleNestedDummy = new NestedDummy().tap{ nd =>
    nd.setReqStr("required 101")
    nd.setBasic(sampleBasicDummy)
  }

  "Basic encode/decode functionality" should {
    "encode/decode Thrift class to Json" in {

      val encodedJson = ThriftJsonConverter.convert(sampleNestedDummy)
      val decodedNestedDummy = JsonThriftConverter.convert(classOf[NestedDummy], encodedJson)

      decodedNestedDummy.getReqStr mustEqual sampleNestedDummy.getReqStr
      decodedNestedDummy.getBasic.getReqStr mustEqual sampleNestedDummy.getBasic.getReqStr
      decodedNestedDummy.getBasic.getInt16 mustEqual sampleNestedDummy.getBasic.getInt16
      decodedNestedDummy.getBasic.getInt32 mustEqual sampleNestedDummy.getBasic.getInt32
      decodedNestedDummy.getBasic.getInt64 mustEqual sampleNestedDummy.getBasic.getInt64
      decodedNestedDummy.getBasic.getDbl mustEqual sampleNestedDummy.getBasic.getDbl
      decodedNestedDummy.getBasic.getByt mustEqual sampleNestedDummy.getBasic.getByt
      decodedNestedDummy.getBasic.isBl mustEqual sampleNestedDummy.getBasic.isBl
      decodedNestedDummy.getBasic.getBin mustEqual sampleNestedDummy.getBasic.getBin
      decodedNestedDummy.getBasic.getListNumbersI32 mustEqual sampleNestedDummy.getBasic.getListNumbersI32
      decodedNestedDummy.getBasic.getListNumbersDouble mustEqual sampleNestedDummy.getBasic.getListNumbersDouble
      decodedNestedDummy.getBasic.getSetNumbersI32 mustEqual sampleNestedDummy.getBasic.getSetNumbersI32
      decodedNestedDummy.getBasic.getSetNumbersDouble mustEqual sampleNestedDummy.getBasic.getSetNumbersDouble
      decodedNestedDummy.getBasic.getEnm mustEqual sampleNestedDummy.getBasic.getEnm
      decodedNestedDummy.getBasic.getListStruct mustEqual sampleNestedDummy.getBasic.getListStruct
      decodedNestedDummy.getBasic.getMapPrimitives mustEqual sampleNestedDummy.getBasic.getMapPrimitives
      decodedNestedDummy.getBasic.getMapStructKey mustEqual sampleNestedDummy.getBasic.getMapStructKey
      decodedNestedDummy.getBasic.getMapPrimitivesStr mustEqual sampleNestedDummy.getBasic.getMapPrimitivesStr
    }
    "decode Json to Thrift" in {
      val decodedNestedDummy = JsonThriftConverter.convert(classOf[NestedDummy], sampleJsonNode)

      decodedNestedDummy.getReqStr mustEqual sampleNestedDummy.getReqStr
      decodedNestedDummy.getBasic.getReqStr mustEqual sampleNestedDummy.getBasic.getReqStr
      decodedNestedDummy.getBasic.getInt16 mustEqual sampleNestedDummy.getBasic.getInt16
      decodedNestedDummy.getBasic.getInt32 mustEqual sampleNestedDummy.getBasic.getInt32
      decodedNestedDummy.getBasic.getInt64 mustEqual sampleNestedDummy.getBasic.getInt64
      decodedNestedDummy.getBasic.getDbl mustEqual sampleNestedDummy.getBasic.getDbl
      decodedNestedDummy.getBasic.getByt mustEqual sampleNestedDummy.getBasic.getByt
      decodedNestedDummy.getBasic.isBl mustEqual sampleNestedDummy.getBasic.isBl
      decodedNestedDummy.getBasic.getBin mustEqual sampleNestedDummy.getBasic.getBin
      decodedNestedDummy.getBasic.getListNumbersI32 mustEqual sampleNestedDummy.getBasic.getListNumbersI32
      decodedNestedDummy.getBasic.getListNumbersDouble mustEqual sampleNestedDummy.getBasic.getListNumbersDouble
      decodedNestedDummy.getBasic.getSetNumbersI32 mustEqual sampleNestedDummy.getBasic.getSetNumbersI32
      decodedNestedDummy.getBasic.getSetNumbersDouble mustEqual sampleNestedDummy.getBasic.getSetNumbersDouble
      decodedNestedDummy.getBasic.getEnm mustEqual sampleNestedDummy.getBasic.getEnm
      decodedNestedDummy.getBasic.getListStruct mustEqual sampleNestedDummy.getBasic.getListStruct
      decodedNestedDummy.getBasic.getMapPrimitives mustEqual sampleNestedDummy.getBasic.getMapPrimitives
      decodedNestedDummy.getBasic.getMapStructKey mustEqual sampleNestedDummy.getBasic.getMapStructKey
      decodedNestedDummy.getBasic.getMapPrimitivesStr mustEqual sampleNestedDummy.getBasic.getMapPrimitivesStr
    }
  }
}
