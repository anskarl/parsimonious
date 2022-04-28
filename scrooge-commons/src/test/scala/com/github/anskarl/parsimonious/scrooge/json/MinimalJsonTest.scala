package com.github.anskarl.parsimonious.scrooge.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.anskarl.parsimonious.{BasicDummy, EnumDummy, NestedDummy, PropertyValue}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.github.anskarl.parsimonious.scrooge._
import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.util.chaining.scalaUtilChainingOps

class MinimalJsonTest extends AnyWordSpecLike  with Matchers {
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val sampleJson = s"""
      |{
      |  "reqStr" : "required 101",
      |  "basic" : {
      |    "reqStr" : "required 101",
      |    "str" : "optional 101",
      |    "int16" : 16,
      |    "int32" : 32,
      |    "int64" : 64,
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

  private val sampleBasicDummy = BasicDummy(
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


  private val sampleNestedDummy = NestedDummy(reqStr = "required 101", basic = sampleBasicDummy)

  "Basic encode/decode functionality" should {
    "encode/decode Thrift class to Json" in {

      val encodedJson: ObjectNode = ScroogeJsonConverter.convert(sampleNestedDummy)
      val decodedNestedDummy: NestedDummy = JsonScroogeConverter.convert(classOf[NestedDummy], encodedJson)

      decodedNestedDummy.reqStr mustEqual sampleNestedDummy.reqStr
      decodedNestedDummy.basic.reqStr mustEqual sampleNestedDummy.basic.reqStr
      decodedNestedDummy.basic.int16 mustEqual sampleNestedDummy.basic.int16
      decodedNestedDummy.basic.int32 mustEqual sampleNestedDummy.basic.int32
      decodedNestedDummy.basic.int64 mustEqual sampleNestedDummy.basic.int64
      decodedNestedDummy.basic.dbl mustEqual sampleNestedDummy.basic.dbl
      decodedNestedDummy.basic.byt mustEqual sampleNestedDummy.basic.byt
      decodedNestedDummy.basic.bl mustEqual sampleNestedDummy.basic.bl
      decodedNestedDummy.basic.bin mustEqual sampleNestedDummy.basic.bin
      decodedNestedDummy.basic.listNumbersI32 mustEqual sampleNestedDummy.basic.listNumbersI32
      decodedNestedDummy.basic.listNumbersDouble mustEqual sampleNestedDummy.basic.listNumbersDouble
      decodedNestedDummy.basic.setNumbersI32 mustEqual sampleNestedDummy.basic.setNumbersI32
      decodedNestedDummy.basic.setNumbersDouble mustEqual sampleNestedDummy.basic.setNumbersDouble
      decodedNestedDummy.basic.enm mustEqual sampleNestedDummy.basic.enm
      decodedNestedDummy.basic.listStruct mustEqual sampleNestedDummy.basic.listStruct
      decodedNestedDummy.basic.mapPrimitives mustEqual sampleNestedDummy.basic.mapPrimitives
      decodedNestedDummy.basic.mapStructKey mustEqual sampleNestedDummy.basic.mapStructKey
      decodedNestedDummy.basic.mapPrimitivesStr mustEqual sampleNestedDummy.basic.mapPrimitivesStr
    }
    "decode Json to Thrift" in {
      val decodedNestedDummy = JsonScroogeConverter.convert(classOf[NestedDummy], sampleJsonNode)

      decodedNestedDummy.reqStr mustEqual sampleNestedDummy.reqStr
      decodedNestedDummy.basic.reqStr mustEqual sampleNestedDummy.basic.reqStr
      decodedNestedDummy.basic.int16 mustEqual sampleNestedDummy.basic.int16
      decodedNestedDummy.basic.int32 mustEqual sampleNestedDummy.basic.int32
      decodedNestedDummy.basic.int64 mustEqual sampleNestedDummy.basic.int64
      decodedNestedDummy.basic.dbl mustEqual sampleNestedDummy.basic.dbl
      decodedNestedDummy.basic.byt mustEqual sampleNestedDummy.basic.byt
      decodedNestedDummy.basic.bl mustEqual sampleNestedDummy.basic.bl
      decodedNestedDummy.basic.bin mustEqual sampleNestedDummy.basic.bin
      decodedNestedDummy.basic.listNumbersI32 mustEqual sampleNestedDummy.basic.listNumbersI32
      decodedNestedDummy.basic.listNumbersDouble mustEqual sampleNestedDummy.basic.listNumbersDouble
      decodedNestedDummy.basic.setNumbersI32 mustEqual sampleNestedDummy.basic.setNumbersI32
      decodedNestedDummy.basic.setNumbersDouble mustEqual sampleNestedDummy.basic.setNumbersDouble
      decodedNestedDummy.basic.enm mustEqual sampleNestedDummy.basic.enm
      decodedNestedDummy.basic.listStruct mustEqual sampleNestedDummy.basic.listStruct
      decodedNestedDummy.basic.mapPrimitives mustEqual sampleNestedDummy.basic.mapPrimitives
      decodedNestedDummy.basic.mapStructKey mustEqual sampleNestedDummy.basic.mapStructKey
      decodedNestedDummy.basic.mapPrimitivesStr mustEqual sampleNestedDummy.basic.mapPrimitivesStr
    }
  }
}
