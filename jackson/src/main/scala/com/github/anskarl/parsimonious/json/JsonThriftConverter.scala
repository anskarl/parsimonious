package com.github.anskarl.parsimonious.json

import com.fasterxml.jackson.core.Base64Variants
import com.github.anskarl.parsimonious.{ClassTBaseType, TBaseType, UnsafeThriftHelpers}
import org.apache.thrift.{TBase, TDeserializer, TFieldIdEnum}
import org.apache.thrift.meta_data.{FieldMetaData, FieldValueMetaData, ListMetaData, MapMetaData, SetMetaData, StructMetaData}
import org.apache.thrift.protocol.{TCompactProtocol, TType}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.collection.mutable

object JsonThriftConverter {

  private val DefaultTCompactProtocolDeserializer = new TDeserializer(new TCompactProtocol.Factory())
  /**
    * Converts a Jackson JsonNode, to a [[TBaseType]]
    */
  def convert[T <: TBaseType](
    tbaseClass: Class[T],
    jsonNode: JsonNode,
    thriftDeserializer: TDeserializer = DefaultTCompactProtocolDeserializer
  ): T =
    convertJsonNodeToThriftGeneric(
      tbaseClass         = tbaseClass.asInstanceOf[ClassTBaseType],
      jsonNode                = jsonNode,
      thriftDeserializer = thriftDeserializer
    ).asInstanceOf[T]

  private def convertJsonNodeToThriftGeneric[F <: TFieldIdEnum](
    tbaseClass: ClassTBaseType,
    jsonNode: JsonNode,
    typeDefClasses: Map[String, ClassTBaseType] = Map.empty,
    thriftDeserializer: TDeserializer
  ): TBaseType = {

    val fieldMeta = UnsafeThriftHelpers
      .getStructMetaDataMap(tbaseClass)
      .asInstanceOf[mutable.Map[F, FieldMetaData]]

    val instance = tbaseClass.getDeclaredConstructor().newInstance().asInstanceOf[TBase[_ <: TBase[_, _], F]]

    for{
      (tFieldIdEnum: TFieldIdEnum, metaData: FieldMetaData) <- fieldMeta
      element = jsonNode.get(metaData.fieldName)
      if element != null
    } {
      val field: F = tFieldIdEnum.asInstanceOf[F]
      val typeDefName: String = metaData.valueMetaData.getTypedefName

      val datum =
        convertJsonElmToJavaElm(
          elm                = element,
          meta               = metaData.valueMetaData,
          typeDefClses       = typeDefClasses + (typeDefName -> tbaseClass),
          thriftDeserializer = thriftDeserializer
        ).asInstanceOf[Object]

      instance.setFieldValue(field, datum)
    }
    instance
  }

  @inline
  private def convertJsonNodeElmSeqToJavaElmSeq(
    seq: node.ArrayNode,
    innerElmMeta: FieldValueMetaData,
    typeDefClasses: Map[String, ClassTBaseType],
    thriftDeserializer: TDeserializer
  ): Seq[Any] = {
    seq.iterator().asScala.map(Option(_)).map(_.map(convertJsonElmToJavaElm(_, innerElmMeta, typeDefClasses, thriftDeserializer)).orNull).toSeq
  }


  /**
    * Converts a json element to a Java element
    */
  private def convertJsonElmToJavaElm(
    elm: Any,
    meta: FieldValueMetaData,
    typeDefClses: Map[String, ClassTBaseType],
    thriftDeserializer: TDeserializer
  ): Any = {

    if (meta.isBinary) {
      val decoded = Base64Variants
        .getDefaultVariant
        .decode(elm.asInstanceOf[node.ValueNode].asText())

      ByteBuffer.wrap(decoded)
    }
    else meta.`type` match {
      // Recursive Cases
      case TType.STRUCT =>
        meta match {
          case structMetaData: StructMetaData =>
            val structSafeClass = structMetaData.structClass
            convertJsonNodeToThriftGeneric(
              tbaseClass         = structSafeClass,
              jsonNode           = elm.asInstanceOf[JsonNode],
              typeDefClasses     = typeDefClses,
              thriftDeserializer = thriftDeserializer
            )
          // This case implies recursion
          case _ =>
            val tBaseType: TBaseType = typeDefClses(meta.getTypedefName).getDeclaredConstructor().newInstance()
            convertJsonNodeToThriftGeneric(
              tbaseClass         = tBaseType.getClass,
              jsonNode           = elm.asInstanceOf[JsonNode],
              typeDefClasses     = typeDefClses,
              thriftDeserializer = thriftDeserializer
            )
        }


      case TType.MAP =>
        val mapMeta = meta.asInstanceOf[MapMetaData]

          // When the key is not STRING, we convert the map to a list of key/value struct
          if(mapMeta.keyMetaData.`type` == TType.STRING) {
            val objNode = elm.asInstanceOf[node.ObjectNode]

            objNode.fields().asScala.map{ entry =>
            entry.getKey -> convertJsonElmToJavaElm(entry.getValue, mapMeta.valueMetaData, typeDefClses,thriftDeserializer)
          }.toMap.asJava
        }
        else {
          val arrayNode = elm.asInstanceOf[node.ArrayNode]

          arrayNode.asScala.map{ element =>
            val keyNode = element.get(keyName)
            val valueNode = element.get(valName)

            val key = convertJsonElmToJavaElm(keyNode,mapMeta.keyMetaData,typeDefClses,thriftDeserializer)
            val value = convertJsonElmToJavaElm(valueNode,mapMeta.valueMetaData,typeDefClses,thriftDeserializer)

            key -> value
          }.toMap.asJava

        }

      case TType.LIST =>
        val listMeta = meta.asInstanceOf[ListMetaData]

        convertJsonNodeElmSeqToJavaElmSeq(
          elm.asInstanceOf[node.ArrayNode],
          listMeta.elemMetaData,
          typeDefClses,
          thriftDeserializer
        ).toList.asJava

      case TType.SET =>

        val setMeta = meta.asInstanceOf[SetMetaData]
        convertJsonNodeElmSeqToJavaElmSeq(
          elm.asInstanceOf[node.ArrayNode],
          setMeta.elemMetaData,
          typeDefClses,
          thriftDeserializer
        ).toSet.asJava

      // Base Cases
      case TType.ENUM      => UnsafeThriftHelpers.enumOf(meta, elm.asInstanceOf[node.TextNode].asText())
      case TType.BYTE      => java.lang.Byte.valueOf(elm.asInstanceOf[node.NumericNode].asInt().byteValue())
      case TType.I16       => java.lang.Short.valueOf(elm.asInstanceOf[node.NumericNode].shortValue())
      case TType.I32       => java.lang.Integer.valueOf(elm.asInstanceOf[node.NumericNode].intValue())
      case TType.I64       => java.lang.Long.valueOf(elm.asInstanceOf[node.NumericNode].longValue())
      case TType.DOUBLE    => java.lang.Double.valueOf(elm.asInstanceOf[node.NumericNode].doubleValue())
      case TType.BOOL      => elm.asInstanceOf[node.BooleanNode].asBoolean()
      case TType.STRING    => elm.asInstanceOf[node.TextNode].asText()

      case illegalType @ _ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }

}
