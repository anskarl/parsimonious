package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious._
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.apache.thrift.meta_data._
import org.apache.thrift.protocol.{TCompactProtocol, TType}
import org.apache.thrift.{TBase, TFieldIdEnum, TFieldRequirementType, TSerializer}

import java.nio.ByteBuffer
import scala.reflect.ClassTag
import scala.collection.JavaConverters._

object ThriftRowConverter {

  private val keyName = "key"
  private val valName = "value"

  //  private val thriftSerializer = new TSerializer(new TCompactProtocol.Factory())
  private val DefaultTCompactProtocolSerializer = new TSerializer(new TCompactProtocol.Factory())

  /**
    * Converts a [[TBaseType]] to a Spark SQL [[Row]]
    */
  def convert[F <: TFieldIdEnum : ClassTag](
    instance: TBase[_ <: TBase[_, _], F],
    thriftSerializer: TSerializer = DefaultTCompactProtocolSerializer
  ): Row = {
    val fieldMeta = FieldMetaData
      .getStructMetaDataMap(instance.getClass.asInstanceOf[Class[_ <: TBase[_, _]]])
      .asScala


    val elms: Seq[Any] = fieldMeta
      .map { entry: (TFieldIdEnum, FieldMetaData) =>
          val (tFieldIdEnum, metaData) = entry
          val field: F = instance.fieldForId(tFieldIdEnum.getThriftFieldId).asInstanceOf[F]

          if (instance.isSet(field))
            convertJavaElmToRowElm(instance.getFieldValue(field), metaData.valueMetaData, thriftSerializer)
          else null

      }
      .toSeq

    Row.fromSeq(elms)
  }

  /**
    * Extracts schema (i.e., Spark SQL [[StructType]]) from a class of [[TBaseType]]
    */
  def extractSchema(tbaseClass: ClassTBaseType): StructType = {

    val fieldMeta = FieldMetaData
      .getStructMetaDataMap(tbaseClass.asInstanceOf[Class[_ <: TBase[_, _]]])
      .asScala


    val fields: Seq[StructField] = fieldMeta
      .map { entry: (TFieldIdEnum, FieldMetaData) =>
        val (tFieldIdEnum, metaData) = entry
        StructField(
          name = tFieldIdEnum.getFieldName,
          dataType = convertThriftFieldToDataType(metaData.valueMetaData),
          nullable = metaData.requirementType != TFieldRequirementType.REQUIRED
        )
      }
      .toSeq

    StructType(fields)
  }

  /**
    * Converts a Java element to a [[Row]] element
    */
  private def convertJavaElmToRowElm(elm: Any, elmMeta: FieldValueMetaData, thriftSerializer: TSerializer): Any = {
    if (elmMeta.isBinary) elm match {
      case elmByteArray: Array[Byte] => elmByteArray
      case elmByteBuffer: ByteBuffer => elmByteBuffer.array()
    }
    else elmMeta.`type` match {
      // Recursive Cases
      case TType.LIST =>
        val seq = elm.asInstanceOf[java.util.List[Any]].asScala
        val innerElmMeta = elmMeta.asInstanceOf[ListMetaData].elemMetaData

        convertJavaElmSeqToRowElmSeq(seq.toSeq, innerElmMeta, thriftSerializer)

      case TType.SET =>
        val seq = elm.asInstanceOf[java.util.Set[Any]].asScala.toSeq
        val innerElmMeta = elmMeta.asInstanceOf[SetMetaData].elemMetaData

        convertJavaElmSeqToRowElmSeq(seq, innerElmMeta, thriftSerializer)

      case TType.MAP =>
        val map = elm.asInstanceOf[java.util.Map[Any, Any]].asScala
        val mapMeta = elmMeta.asInstanceOf[MapMetaData]

        val keys: Seq[Any] = convertJavaElmSeqToRowElmSeq(map.keys.toSeq, mapMeta.keyMetaData, thriftSerializer)
        val vals: Seq[Any] = convertJavaElmSeqToRowElmSeq(map.values.toSeq, mapMeta.valueMetaData, thriftSerializer)
        val keyVals = keys.zip(vals)

        // If the key is not primitive, we convert the map to a list of key/value struct
        if (isPrimitive(mapMeta.keyMetaData)) Map(keyVals: _*) else keyVals.map(tuple => Row(tuple._1, tuple._2))

      case TType.STRUCT => elmMeta match {
        case _: StructMetaData => convert(elm.asInstanceOf[TBaseType])
        // If we've recursed on a struct, thrift returns a TType.Struct with non StructMetaData. We serialize to bytes
        case _ => thriftSerializer.serialize(elm.asInstanceOf[TBase[_, _]])
      }
      // Base Cases
      case TType.ENUM => elm.toString

      case TType.BYTE => java.lang.Byte.valueOf(elm.asInstanceOf[java.lang.Number].byteValue())
      case TType.I16 => java.lang.Short.valueOf(elm.asInstanceOf[java.lang.Number].shortValue())
      case TType.I32 => java.lang.Integer.valueOf(elm.asInstanceOf[java.lang.Number].intValue())
      case TType.I64 => java.lang.Long.valueOf(elm.asInstanceOf[java.lang.Number].longValue())
      case TType.DOUBLE => java.lang.Double.valueOf(elm.asInstanceOf[java.lang.Number].doubleValue())
      case TType.BOOL => elm.asInstanceOf[java.lang.Boolean]
      case TType.STRING => elm.asInstanceOf[java.lang.String]
      case illegalType@_ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }

  /**
    * Converts a Thrift field to a Spark SQL [[DataType]].
    */
  private def convertThriftFieldToDataType(meta: FieldValueMetaData): DataType = {

    if (meta.isBinary) BinaryType else meta.`type` match {
      // Recursive Cases
      case TType.LIST =>

        val listMetaData = meta.asInstanceOf[ListMetaData]
        ArrayType(convertThriftFieldToDataType(listMetaData.elemMetaData))

      case TType.SET =>

        val setMetaData = meta.asInstanceOf[SetMetaData]
        ArrayType(convertThriftFieldToDataType(setMetaData.elemMetaData))

      case TType.MAP =>

        val mapMetaData = meta.asInstanceOf[MapMetaData]
        val keyDataType = convertThriftFieldToDataType(mapMetaData.keyMetaData)
        val valueDataType = convertThriftFieldToDataType(mapMetaData.valueMetaData)

        // If the key is not primitive, we convert the map to a list of key/value struct
        if (isPrimitive(mapMetaData.keyMetaData))
          MapType(keyDataType, valueDataType)
        else ArrayType(
          StructType(Seq(StructField(keyName, keyDataType), StructField(valName, valueDataType))),
          containsNull = false
        )

      case TType.STRUCT => meta match {
        case structMetaData: StructMetaData => extractSchema(structMetaData.structClass)
        // If we have preformed a recursion on a struct, thrift does not return StructMetaData.
        // We use StringType here for JSON
        case _ => BinaryType
      }
      // Base Cases
      case TType.BOOL => BooleanType
      case TType.BYTE => ByteType
      case TType.DOUBLE => DoubleType
      case TType.I16 => ShortType
      case TType.I32 => IntegerType
      case TType.I64 => LongType
      case TType.STRING | TType.ENUM => StringType
      case illegalType@_ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }

  @inline
  private def isPrimitive(meta: FieldValueMetaData): Boolean = !(meta.isContainer || meta.isStruct)

  @inline
  private def convertJavaElmSeqToRowElmSeq(seq: Seq[Any], innerElmMeta: FieldValueMetaData, thriftSerializer: TSerializer): Seq[Any] =
    seq.map(Option(_)).map(_.map(convertJavaElmToRowElm(_, innerElmMeta, thriftSerializer)).orNull)

}
