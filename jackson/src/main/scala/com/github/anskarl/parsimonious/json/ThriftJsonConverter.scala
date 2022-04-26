package com.github.anskarl.parsimonious.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.anskarl.parsimonious.TBaseType
import org.apache.thrift.meta_data._
import org.apache.thrift.protocol.TType
import org.apache.thrift.{TBase, TFieldIdEnum, TSerializer}

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object ThriftJsonConverter {

  private final val nodeFactory = mapper.getNodeFactory

  /**
    * Converts a [[TBaseType]] to Jackson [[ObjectNode]]
    */
  def convert[F <: TFieldIdEnum: ClassTag](
      instance: TBase[_ <: TBase[_, _], F],
      thriftSerializer: TSerializer = DefaultTCompactProtocolSerializer
  ): ObjectNode = {

    val fieldMeta = FieldMetaData
      .getStructMetaDataMap(instance.getClass.asInstanceOf[Class[_ <: TBase[_, _]]])
      .asScala

    fieldMeta.foldLeft(mapper.createObjectNode()){
      case (node, (tFieldIdEnum: TFieldIdEnum, metaData: FieldMetaData)) =>
        val field: F = instance.fieldForId(tFieldIdEnum.getThriftFieldId)

        if (instance.isSet(field)) {
          val element = convertJavaElmToJSONElm(instance.getFieldValue(field), metaData.valueMetaData, thriftSerializer)
          val name = field.getFieldName
          node.replace(name, element)
        }

        node
    }

  }

  /**
    * Converts a Java element to JSON
    */
  private def convertJavaElmToJSONElm(elm: Any, elmMeta: FieldValueMetaData, thriftSerializer: TSerializer): JsonNode = {
    if (elmMeta.isBinary) {
      val bytes = elm match {
        case elmByteArray: Array[Byte] => elmByteArray
        case elmByteBuffer: ByteBuffer => elmByteBuffer.array()
      }
      nodeFactory.binaryNode(bytes)
    }
    else elmMeta.`type` match {
      // Recursive Cases
      case TType.LIST =>
        val seq = elm.asInstanceOf[java.util.List[Any]].asScala
        val innerElmMeta = elmMeta.asInstanceOf[ListMetaData].elemMetaData
        val elements = convertJavaElmSeqToJsonNodeElmSeq(seq.toSeq, innerElmMeta, thriftSerializer)
        nodeFactory.arrayNode().addAll(elements.asJava)

      case TType.SET =>
        val seq = elm.asInstanceOf[java.util.Set[Any]].asScala.toSeq
        val innerElmMeta = elmMeta.asInstanceOf[SetMetaData].elemMetaData

        val elements = convertJavaElmSeqToJsonNodeElmSeq(seq, innerElmMeta, thriftSerializer)
        nodeFactory.arrayNode().addAll(elements.asJava)

      case TType.MAP =>
        val map = elm.asInstanceOf[java.util.Map[Any, Any]].asScala
        val mapMeta = elmMeta.asInstanceOf[MapMetaData]

        val keys: Seq[JsonNode] = convertJavaElmSeqToJsonNodeElmSeq(map.keys.toSeq, mapMeta.keyMetaData, thriftSerializer)
        val vals: Seq[JsonNode] = convertJavaElmSeqToJsonNodeElmSeq(map.values.toSeq, mapMeta.valueMetaData, thriftSerializer)
        val keyVals = keys.zip(vals)


        // When the key is not STRING, we convert the map to a list of key/value struct
        if(mapMeta.keyMetaData.`type` == TType.STRING) {
          keyVals.foldLeft(nodeFactory.objectNode()){
            case (node, (k, v)) =>
              node.replace(k.asText(), v)
              node
          }
        }
        else {
          val elements = keyVals.map{case (k,v) =>
            val objNode = nodeFactory.objectNode()
            objNode.replace(keyName, k)
            objNode.replace(valName, v)
            objNode
          }
          nodeFactory.arrayNode().addAll(elements.asJava)
        }

      case TType.STRUCT =>
        convert(elm.asInstanceOf[TBaseType])

      // Base Cases
      case TType.ENUM      =>
        nodeFactory.textNode(elm.toString)
      case TType.BYTE      =>
        nodeFactory.numberNode(java.lang.Byte.valueOf(elm.asInstanceOf[java.lang.Number].byteValue()).intValue())
      case TType.I16       =>
        nodeFactory.numberNode(java.lang.Short.valueOf(elm.asInstanceOf[java.lang.Number].shortValue()))
      case TType.I32       =>
        nodeFactory.numberNode(java.lang.Integer.valueOf(elm.asInstanceOf[java.lang.Integer].toInt))
      case TType.I64       =>
        nodeFactory.numberNode(java.lang.Long.valueOf(elm.asInstanceOf[java.lang.Number].longValue()))
      case TType.DOUBLE    =>
        nodeFactory.numberNode(java.lang.Double.valueOf(elm.asInstanceOf[java.lang.Number].doubleValue()))
      case TType.BOOL      =>
        nodeFactory.booleanNode(elm.asInstanceOf[java.lang.Boolean])
      case TType.STRING    =>
        nodeFactory.textNode(elm.asInstanceOf[java.lang.String])
      case illegalType @ _ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }


  @inline
  private def convertJavaElmSeqToJsonNodeElmSeq(seq: Seq[Any], innerElmMeta: FieldValueMetaData, thriftSerializer: TSerializer): Seq[JsonNode] =
    seq.map(Option(_)).map(_.map(convertJavaElmToJSONElm(_, innerElmMeta, thriftSerializer)).orNull)

}
