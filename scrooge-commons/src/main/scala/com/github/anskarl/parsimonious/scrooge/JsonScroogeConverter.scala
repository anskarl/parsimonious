package com.github.anskarl.parsimonious.scrooge

import com.fasterxml.jackson.core.Base64Variants
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, node}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.anskarl.parsimonious.scrooge.DeleteMeDec.getClass
import com.github.anskarl.parsimonious.{ComplexDummy, NestedDummy, UnionRecursiveDummy}
import com.twitter.scrooge.{StructBuilderFactory, ThriftEnumObject, ThriftStruct, ThriftStructCodec, ThriftStructFieldInfo, ThriftUnion, ThriftUnionFieldInfo}
import org.apache.thrift.protocol.TType

import scala.collection.JavaConverters._
import scala.collection.mutable
import java.nio.ByteBuffer
import scala.util.{Success, Try}
import scala.reflect.runtime.{universe => ru}
import com.twitter.util.reflect.Types._


object JsonScroogeConverter {
  private final val m = ru.runtimeMirror(getClass.getClassLoader)

  def convert[T <: ThriftStruct with Product: ru.TypeTag](structClass: Class[T], jsonNode: JsonNode): T ={
    val codec: ThriftStructCodec[T] = com.twitter.scrooge.ThriftStructCodec.forStructClass(structClass)
    val isUnion = codec.metaData.unionFields.nonEmpty

    if(isUnion) convertUnion(jsonNode, codec)
    else convertStruct(jsonNode, codec.asInstanceOf[ThriftStructCodec[T] with StructBuilderFactory[T]])

  }

  private def convertUnion[T <: ThriftStruct: ru.TypeTag](jsonNode: JsonNode, codec: ThriftStructCodec[T]): T ={
    val unionFields = codec.metaData.unionFields
    val fieldNames: Seq[String] = unionFields.map(_.structFieldInfo.tfield.name)

    val jsonFieldName = jsonNode.fieldNames().next()
    val index = fieldNames.indexOf(jsonFieldName)
    val thriftUnionFieldInfo = unionFields(index)
    val element = convertJsonElmToScroogeElm(jsonNode.get(jsonFieldName), thriftUnionFieldInfo.structFieldInfo.fieldInfo)

    val classUnion = ru.typeOf(asTypeTag(thriftUnionFieldInfo.fieldClassTag.runtimeClass)).typeSymbol.asClass
    val cu = m.reflectClass(classUnion)

    val ctor = ru.typeOf(asTypeTag(thriftUnionFieldInfo.fieldClassTag.runtimeClass)).decl(ru.termNames.CONSTRUCTOR).asMethod
    val ctorm = cu.reflectConstructor(ctor)
    val instance = ctorm(element)

    instance.asInstanceOf[T]
  }

  private def convertStruct[T <: ThriftStruct with Product](
    jsonNode: JsonNode,
    codecWithBuilder: ThriftStructCodec[T] with StructBuilderFactory[T]): T ={

    val fieldInfos: Seq[ThriftStructFieldInfo] = getFieldInfos(codecWithBuilder)
    val builder = codecWithBuilder.newBuilder()
    for{
      (fieldInfo, index) <- fieldInfos.zipWithIndex
      element = jsonNode.get(fieldInfo.tfield.name)
    }{
      if(element != null){
        val value = convertJsonElmToScroogeElm(element, fieldInfo)
//        if(fieldInfo.isOptional) builder.setField(index, Option(value))
//        else
        builder.setField(index, value)
      }
      else builder.setField(index, None)
    }

    builder.build()
  }

  implicit class RichFieldInfo(val fieldInfo: ThriftStructFieldInfo) extends AnyVal{
    def convert(value: Any): Any =
      if(fieldInfo.isOptional) Option(value) else value
  }

  def convertJsonElmToScroogeElm(elm: Any, fieldInfo: ThriftStructFieldInfo): Any ={
    val fieldType = fieldInfo.tfield.`type`

    fieldType match {
      case TType.BOOL =>
        fieldInfo.convert(elm.asInstanceOf[node.BooleanNode].asBoolean())
      case TType.BYTE =>
        fieldInfo.convert(elm.asInstanceOf[node.NumericNode].asInt().toByte)
      case TType.DOUBLE =>
        fieldInfo.convert(elm.asInstanceOf[node.NumericNode].asDouble)
      case TType.I16 =>
        fieldInfo.convert(elm.asInstanceOf[node.NumericNode].asInt.toShort)
      case TType.I32 =>
        fieldInfo.convert(elm.asInstanceOf[node.NumericNode].asInt)
      case TType.I64 =>
        fieldInfo.convert(elm.asInstanceOf[node.NumericNode].asLong)
      case TType.STRING =>
        if(!classOf[String].equals(fieldInfo.manifest.runtimeClass)){
          val decoded = Base64Variants
            .getDefaultVariant
            .decode(elm.asInstanceOf[node.ValueNode].asText())
          fieldInfo.convert(ByteBuffer.wrap(decoded))
        }
        else fieldInfo.convert(elm.asInstanceOf[node.TextNode].asText())
      case TType.STRUCT =>
        val structClass = fieldInfo.manifest.runtimeClass.asInstanceOf[ThriftStructWithProduct]
        val structJsonNode = elm.asInstanceOf[JsonNode]
        fieldInfo.convert(convert(structClass, structJsonNode))
      case TType.MAP =>
        val keyManifest = fieldInfo.keyManifest.get
        val keyThriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)

        val value =
          if(keyThriftStructFieldInfo.tfield.`type`== TType.STRING){
            val objNode = elm.asInstanceOf[node.ObjectNode]
            objNode.fields().asScala.map { entry =>
              entry.getKey -> convertJsonElmToScroogeElm(entry.getValue, valueThriftStructFieldInfo)
            }.toMap
          }
          else {
            val arrayNode = elm.asInstanceOf[node.ArrayNode]
            arrayNode.asScala.map{ element =>
              val keyNode = element.get(keyName)
              val valueNode = element.get(valName)

              val key = convertJsonElmToScroogeElm(keyNode,keyThriftStructFieldInfo)
              val value = convertJsonElmToScroogeElm(valueNode,valueThriftStructFieldInfo)

              key -> value
            }.toMap
          }

        Option(value)

      case TType.LIST =>
        val seq = elm.asInstanceOf[node.ArrayNode]
        val thriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_values",fieldInfo.valueManifest.get)
        val values = seq.iterator().asScala.map(node => convertJsonElmToScroogeElm(node, thriftStructFieldInfo))
        fieldInfo.convert(values.toList)

      case TType.SET =>
        val seq = elm.asInstanceOf[node.ArrayNode]
        val thriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_values",fieldInfo.valueManifest.get)
        val values = seq.iterator().asScala.map(node => convertJsonElmToScroogeElm(node, thriftStructFieldInfo))
        fieldInfo.convert(values.toSet)

      case TType.ENUM =>
        val enumValue = elm.asInstanceOf[node.TextNode].asText()

        ThriftEnumObject
          .forEnumClass(fieldInfo.manifest.runtimeClass.asSubclass(classOf[com.twitter.scrooge.ThriftEnum]))
          .valueOf(enumValue)

      case illegalType @ _ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")

    }

  }

}


object DeleteMeDec extends App {

//  val basicJson =
//    s"""
//     |{
//     |  "reqStr" : "required 101",
//     |  "basic" : {
//     |    "reqStr" : "required 101",
//     |    "str" : "optional 101",
//     |    "int16" : 101,
//     |    "int32" : 101,
//     |    "int64" : 101,
//     |    "dbl" : 101.101,
//     |    "byt" : 8,
//     |    "bl" : false,
//     |    "bin" : "MTAx",
//     |    "enm" : "MAYBE",
//     |    "listNumbersI32" : [ 1, 2, 3 ],
//     |    "listNumbersDouble" : [ 1.1, 2.2, 3.3 ],
//     |    "setNumbersI32" : [ 1, 2, 3, 4 ],
//     |    "listStruct" : [ {
//     |      "property" : "prop1",
//     |      "value" : "val1"
//     |    } ],
//     |    "mapPrimitives" : [ {
//     |      "key" : 1,
//     |      "value" : 1.1
//     |    }, {
//     |      "key" : 2,
//     |      "value" : 2.2
//     |    } ],
//     |    "mapStructKey" : [ {
//     |      "key" : {
//     |        "property" : "a",
//     |        "value" : "aa"
//     |      },
//     |      "value" : 1.0
//     |    }, {
//     |      "key" : {
//     |        "property" : "b",
//     |        "value" : "bb"
//     |      },
//     |      "value" : 2.0
//     |    } ],
//     |    "mapPrimitivesStr" : {
//     |      "a" : 1.1,
//     |      "b" : 2.2
//     |    }
//     |  }
//     |}
//     |""".stripMargin
//  val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
//  val jsonNode = mapper.readTree(basicJson)
//
//  val instance = JsonScroogeConverter.convert(classOf[NestedDummy], jsonNode)
//  println(instance)

  val complexDummyJson =
    s"""
       |{
       |  "bdList" : [ ],
       |  "bdSet" : [ ],
       |  "strToBdMap" : { },
       |  "bdToStrMap" : [ ],
       |  "enumDummy" : "Maybe",
       |  "unionDummy" : {
       |    "str" : "aaa"
       |  },
       |  "unionRecursiveDummy" : {
       |    "ur" : {
       |      "ur" : {
       |        "bl" : true
       |      }
       |    }
       |  }
       |}
       |""".stripMargin
//
//  val complexDummyJson =
//    s"""
//       |{
//       |  "bdList" : [ ],
//       |  "bdSet" : [ ],
//       |  "strToBdMap" : { },
//       |  "bdToStrMap" : [ ],
//       |  "enumDummy" : "Maybe",
//       |  "unionDummy" : {
//       |    "str" : "aaa"
//       |  }
//       |}
//       |""".stripMargin

  val cdJsonNode = mapper.readTree(complexDummyJson)
  val cd = JsonScroogeConverter.convert(classOf[ComplexDummy], cdJsonNode)
  println(cd)
//
  import scala.reflect.runtime.{universe => ru}
  val m = ru.runtimeMirror(getClass.getClassLoader)
  val classUnion = ru.typeOf[com.github.anskarl.parsimonious.UnionRecursiveDummy.Bl].typeSymbol.asClass
  val cu = m.reflectClass(classUnion)
  val ctor = ru.typeOf[com.github.anskarl.parsimonious.UnionRecursiveDummy.Bl].decl(ru.termNames.CONSTRUCTOR).asMethod
  val ctorm = cu.reflectConstructor(ctor)
  val instance = ctorm(true)
  println(instance)


}