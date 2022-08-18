package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious.common.ParsimoniousConfig
import com.github.anskarl.parsimonious.{ClassTBaseType, TBaseType, UnsafeThriftHelpers}
import org.apache.spark.sql.Row
import org.apache.thrift.meta_data._
import org.apache.thrift.protocol.TType
import org.apache.thrift.{TBase, TFieldIdEnum}

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.collection.mutable

object RowThriftConverter {

  /**
    * Converts a Spark SQL [[Row]] to a [[TBaseType]]
    */
  def convert[T <: TBaseType](
    tbaseClass: Class[T],
    row: Row
  )(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T =
    convertRowToThriftGeneric(
      tbaseClass = tbaseClass.asInstanceOf[ClassTBaseType],
      row = row
    ).asInstanceOf[T]

  private def convertRowToThriftGeneric[F <: TFieldIdEnum](
    tbaseClass: ClassTBaseType,
    row: Row,
    typeDefClasses: Map[String, ClassTBaseType] = Map.empty
  )(implicit parsimoniousConfig: ParsimoniousConfig): TBaseType = {

    val fieldMeta = UnsafeThriftHelpers
      .getStructMetaDataMap(tbaseClass)
      .asInstanceOf[mutable.Map[F, FieldMetaData]]

    val instance = tbaseClass.getConstructor().newInstance().asInstanceOf[TBase[_ <: TBase[_, _], F]]

    fieldMeta.zipWithIndex.foreach({
      case ((tFieldIdEnum: TFieldIdEnum, metaData: FieldMetaData), i: Int) =>
        if (!row.isNullAt(i)) {
          val field: F = tFieldIdEnum.asInstanceOf[F]
          val typeDefName: String = metaData.valueMetaData.getTypedefName

          val datum =
            convertRowElmToJavaElm(
              elm = row(i),
              meta = metaData.valueMetaData,
              typeDefClasses = typeDefClasses + (typeDefName -> tbaseClass)
            ).asInstanceOf[Object]

          instance.setFieldValue(field, datum)
        }
    })

    instance
  }

  @inline
  private def convertRowElmSeqToJavaElmSeq(
    seq: Seq[Any],
    innerElmMeta: FieldValueMetaData,
    typeDefClasses: Map[String, ClassTBaseType]
  )(implicit parsimoniousConfig: ParsimoniousConfig): Seq[Any] =
    seq.map(Option(_)).map(_.map(convertRowElmToJavaElm(_, innerElmMeta, typeDefClasses)).orNull)

  /**
    * Converts a [[Row]] element to a Java element
    */
  private def convertRowElmToJavaElm(
    elm: Any,
    meta: FieldValueMetaData,
    typeDefClasses: Map[String, ClassTBaseType]
  )(implicit parsimoniousConfig: ParsimoniousConfig): Any = {

    if (meta.isBinary) ByteBuffer.wrap(elm.asInstanceOf[Array[Byte]]) else meta.`type` match {
      // Recursive Cases
      case TType.STRUCT => meta match {
        case structMetaData: StructMetaData =>
          val structSafeClass = structMetaData.structClass
          convertRowToThriftGeneric(
            tbaseClass = structSafeClass,
            row = elm.asInstanceOf[Row],
            typeDefClasses = typeDefClasses
          )
        // This case implies recursion
        case _ =>
          val recursiveInstance = typeDefClasses(meta.getTypedefName).getConstructor().newInstance()
          parsimoniousConfig.protocolDeserializer.deserialize(recursiveInstance.asInstanceOf[TBase[_, _]], elm.asInstanceOf[Array[Byte]])
          recursiveInstance
      }

      case TType.MAP =>
        val mapMeta = meta.asInstanceOf[MapMetaData]

        val keys = elm match {
          case map: Map[_, _] => map.keys
          case mapRows: Iterable[_] => mapRows.map { case Row(k: Any, _) => k }
        }

        val vals = elm match {
          case map: Map[_, _] => map.values
          case mapRows: Iterable[_] => mapRows.map { case Row(_, v: Any) => v }
        }

        val keyVals =
          convertRowElmSeqToJavaElmSeq(keys.toSeq, mapMeta.keyMetaData, typeDefClasses)
            .zip(convertRowElmSeqToJavaElmSeq(vals.toSeq, mapMeta.valueMetaData, typeDefClasses))

        Map(keyVals: _*).asJava

      case TType.LIST =>
        val listMeta = meta.asInstanceOf[ListMetaData]
        // should keep WrappedArray to retain compatibility with Scala 2.12
        val arr = elm.asInstanceOf[scala.collection.Iterable[Any]]

        convertRowElmSeqToJavaElmSeq(
          arr.toSeq,
          listMeta.elemMetaData,
          typeDefClasses
        ).toList.asJava

      case TType.SET =>

        val setMeta = meta.asInstanceOf[SetMetaData]

        // should keep WrappedArray to retain compatibility with Scala 2.12
        val arr = elm.asInstanceOf[scala.collection.Iterable[Any]]

        convertRowElmSeqToJavaElmSeq(
          arr.toSeq,
          setMeta.elemMetaData,
          typeDefClasses
        ).toSet.asJava

      // Base Cases
      case TType.ENUM => UnsafeThriftHelpers.enumOf(meta, elm.asInstanceOf[String])
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
}
