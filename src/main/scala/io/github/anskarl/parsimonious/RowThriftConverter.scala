package io.github.anskarl.parsimonious

import java.nio.ByteBuffer
import org.apache.spark.sql.Row
import org.apache.thrift.meta_data._
import org.apache.thrift.protocol.{ TCompactProtocol, TType }
import org.apache.thrift.{ TBase, TDeserializer, TFieldIdEnum }

import scala.collection.JavaConverters._
import scala.collection.mutable

object RowThriftConverter {

  private val DefaultTCompactProtocolDeserializer = new TDeserializer(new TCompactProtocol.Factory())

  /**
    * Converts a Spark SQL [[Row]] to a [[TBaseType]]
    */
  def convert[T <: TBaseType](
      tbaseClass: Class[T],
      row: Row,
      thriftDeserializer: TDeserializer = DefaultTCompactProtocolDeserializer
  ): T =
    convertRowToThriftGeneric(
      tbaseClass         = tbaseClass.asInstanceOf[ClassTBaseType],
      row                = row,
      thriftDeserializer = thriftDeserializer
    ).asInstanceOf[T]

  private def convertRowToThriftGeneric[F <: TFieldIdEnum](
      tbaseClass: ClassTBaseType,
      row: Row,
      typeDefClasses: Map[String, ClassTBaseType] = Map.empty,
      thriftDeserializer: TDeserializer
  ): TBaseType = {

    val fieldMeta = UnsafeThriftHelpers
      .getStructMetaDataMap(tbaseClass)
      .asInstanceOf[mutable.Map[F, FieldMetaData]]

    val instance = tbaseClass.newInstance().asInstanceOf[TBase[_ <: TBase[_, _], F]]

    fieldMeta.zipWithIndex.foreach({
      case ((tFieldIdEnum: TFieldIdEnum, metaData: FieldMetaData), i: Int) =>
        if (!row.isNullAt(i)) {
          // val tFieldIdEnum: TFieldIdEnum = instance.fieldForId(tFieldIdEnum.getThriftFieldId.toInt)
          val field: F = tFieldIdEnum.asInstanceOf[F]
          val typeDefName: String = metaData.valueMetaData.getTypedefName

          val datum =
            convertRowElmToJavaElm(
              elm                = row(i),
              meta               = metaData.valueMetaData,
              typeDefClses       = typeDefClasses + (typeDefName -> tbaseClass),
              thriftDeserializer = thriftDeserializer
            ).asInstanceOf[Object]

          instance.setFieldValue(field, datum) //todo
        }
    })

    instance
  }

  @inline
  private def convertRowElmSeqToJavaElmSeq(
      seq: Seq[Any],
      innerElmMeta: FieldValueMetaData,
      typeDefClasses: Map[String, ClassTBaseType],
      thriftDeserializer: TDeserializer
  ): Seq[Any] =
    seq.map(Option(_)).map(_.map(convertRowElmToJavaElm(_, innerElmMeta, typeDefClasses, thriftDeserializer)).orNull)

  /**
    * Converts a [[Row]] element to a Java element
    */
  private def convertRowElmToJavaElm(
      elm: Any,
      meta: FieldValueMetaData,
      typeDefClses: Map[String, ClassTBaseType],
      thriftDeserializer: TDeserializer
  ): Any = {

    if (meta.isBinary) ByteBuffer.wrap(elm.asInstanceOf[Array[Byte]]) else meta.`type` match {
      // Recursive Cases
      case TType.STRUCT => meta match {
        case structMetaData: StructMetaData =>
          val structSafeClass = structMetaData.structClass
          convertRowToThriftGeneric(
            tbaseClass         = structSafeClass,
            row                = elm.asInstanceOf[Row],
            typeDefClasses     = typeDefClses,
            thriftDeserializer = thriftDeserializer
          )
        // This case implies recursion
        case _ =>
          val recursiveInstance = typeDefClses(meta.getTypedefName).newInstance()
          thriftDeserializer.deserialize(recursiveInstance.asInstanceOf[TBase[_, _]], elm.asInstanceOf[Array[Byte]])
          recursiveInstance
      }

      case TType.MAP =>
        val mapMeta = meta.asInstanceOf[MapMetaData]

        val keys = elm match {
          case map: Map[_, _]  => map.keys
          case mapRows: Seq[_] => mapRows.map{ case Row(k: Any, _) => k }
        }

        val vals = elm match {
          case map: Map[_, _]  => map.values
          case mapRows: Seq[_] => mapRows.map{ case Row(_, v: Any) => v }
        }

        val keyVals =
          convertRowElmSeqToJavaElmSeq(keys.toSeq, mapMeta.keyMetaData, typeDefClses, thriftDeserializer)
            .zip(convertRowElmSeqToJavaElmSeq(vals.toSeq, mapMeta.valueMetaData, typeDefClses, thriftDeserializer))

        Map(keyVals: _*).asJava

      case TType.LIST =>
        val listMeta = meta.asInstanceOf[ListMetaData]

        convertRowElmSeqToJavaElmSeq(
          elm.asInstanceOf[Seq[Any]],
          listMeta.elemMetaData,
          typeDefClses,
          thriftDeserializer
        ).toList.asJava

      case TType.SET =>

        val setMeta = meta.asInstanceOf[SetMetaData]
        convertRowElmSeqToJavaElmSeq(
          elm.asInstanceOf[Seq[Any]],
          setMeta.elemMetaData,
          typeDefClses,
          thriftDeserializer
        ).toSet.asJava

      // Base Cases
      case TType.ENUM      => UnsafeThriftHelpers.enumOf(meta, elm.asInstanceOf[String])
      case TType.BYTE      => new java.lang.Byte(elm.asInstanceOf[java.lang.Number].byteValue())
      case TType.I16       => new java.lang.Short(elm.asInstanceOf[java.lang.Number].shortValue())
      case TType.I32       => new java.lang.Integer(elm.asInstanceOf[java.lang.Number].intValue())
      case TType.I64       => new java.lang.Long(elm.asInstanceOf[java.lang.Number].longValue())
      case TType.DOUBLE    => new java.lang.Double(elm.asInstanceOf[java.lang.Number].doubleValue())
      case TType.BOOL      => elm.asInstanceOf[java.lang.Boolean]
      case TType.STRING    => elm.asInstanceOf[java.lang.String]

      case illegalType @ _ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }
}
