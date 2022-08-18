package com.github.anskarl.parsimonious.scrooge.json

import com.fasterxml.jackson.core.Base64Variants
import com.fasterxml.jackson.databind.{JsonNode, node}
import com.github.anskarl.parsimonious.scrooge.{ScroogeHelpers, ThriftStructWithProduct, UnionBuilders}
import com.github.anskarl.parsimonious.common.ParsimoniousConfig
import com.twitter.scrooge._
import org.apache.thrift.protocol.TType

import scala.collection.JavaConverters._
import java.nio.ByteBuffer
import scala.reflect.runtime.{universe => ru}

object JsonScroogeConverter {

  def convert[T <: ThriftStruct with Product: ru.TypeTag](structClass: Class[T], jsonNode: JsonNode)(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): T ={
    val codec: ThriftStructCodec[T] = com.twitter.scrooge.ThriftStructCodec.forStructClass(structClass)
    val isUnion = codec.metaData.unionFields.nonEmpty

    if(isUnion) convertUnion(jsonNode, codec)
    else convertStruct(jsonNode, codec.asInstanceOf[ThriftStructCodec[T] with StructBuilderFactory[T]])

  }

  private def convertUnion[T <: ThriftStruct: ru.TypeTag](jsonNode: JsonNode, codec: ThriftStructCodec[T])(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): T ={
    val unionFields = codec.metaData.unionFields

    val fieldNames: Seq[String] = unionFields.map(_.structFieldInfo.tfield.name)

    val jsonFieldName = jsonNode.fieldNames().next()
    val index = fieldNames.indexOf(jsonFieldName)
    val thriftUnionFieldInfo = unionFields(index)

    val element = convertJsonElmToScroogeElm(jsonNode.get(jsonFieldName), thriftUnionFieldInfo.structFieldInfo.fieldInfo)

    unionBuilders.build[T](codec, jsonFieldName, element)
  }

  private def convertStruct[T <: ThriftStruct with Product](
    jsonNode: JsonNode,
    codecWithBuilder: ThriftStructCodec[T] with StructBuilderFactory[T])(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): T ={

    val fieldInfos: Seq[ThriftStructFieldInfo] = ScroogeHelpers.getFieldInfos(codecWithBuilder)
    val builder = codecWithBuilder.newBuilder()
    for{
      (fieldInfo, index) <- fieldInfos.zipWithIndex
      element = jsonNode.get(fieldInfo.tfield.name)
    }{
      if(element != null){
        val value = convertJsonElmToScroogeElm(element, fieldInfo)
        builder.setField(index, value)
      }
      else builder.setField(index, None)
    }

    builder.build()
  }


  def convertJsonElmToScroogeElm(elm: Any, fieldInfo: ThriftStructFieldInfo)(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): Any ={
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
        val keyThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)

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
              val keyNode = element.get(parsimoniousConfig.keyName)
              val valueNode = element.get(parsimoniousConfig.valName)

              val key = convertJsonElmToScroogeElm(keyNode,keyThriftStructFieldInfo)
              val value = convertJsonElmToScroogeElm(valueNode,valueThriftStructFieldInfo)

              key -> value
            }.toMap
          }

        fieldInfo.convert(value)

      case TType.LIST =>
        val seq = elm.asInstanceOf[node.ArrayNode]
        val thriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values",fieldInfo.valueManifest.get)
        val values = seq.iterator().asScala.map(node => convertJsonElmToScroogeElm(node, thriftStructFieldInfo))
        fieldInfo.convert(values.toList)

      case TType.SET =>
        val seq = elm.asInstanceOf[node.ArrayNode]
        val thriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values",fieldInfo.valueManifest.get)
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

  implicit class RichFieldInfo(val fieldInfo: ThriftStructFieldInfo) extends AnyVal{
    def convert(value: Any): Any =
      if(fieldInfo.isOptional) Option(value) else value
  }
}
