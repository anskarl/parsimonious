package com.github.anskarl.parsimonious.scrooge.spark

import com.github.anskarl.parsimonious.common.ParsimoniousConfig
import com.github.anskarl.parsimonious.scrooge._
import com.twitter.scrooge._
import org.apache.spark.sql.Row
import org.apache.thrift.protocol.TType

import java.nio.ByteBuffer
import scala.reflect.runtime.{universe => ru}

object RowScroogeConverter {

  def convert[T <: ThriftStruct with Product: ru.TypeTag](
    structClass: Class[T],
    row: Row
  )(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T ={
    val codec = com.twitter.scrooge.ThriftStructCodec.forStructClass(structClass)
    convertWithCodec(codec,row)
  }

  def convertWithCodec[T <: ThriftStruct with Product: ru.TypeTag](
    codec: ThriftStructCodec[T],
    row: Row
  )(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T ={

    val isUnion = codec.metaData.unionFields.nonEmpty

    if(isUnion) convertUnion(row, codec)
    else convertStruct(row, codec.asInstanceOf[ThriftStructCodec[T] with StructBuilderFactory[T]])
  }

  private def convertUnion[T <: ThriftStruct: ru.TypeTag](row: Row, codec: ThriftStructCodec[T])
    (implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): T = {
    val unionFields = codec.metaData.unionFields

    val (fieldName, index) = row.schema.fieldNames.zipWithIndex
      .find{ case (_, index) => !row.isNullAt(index) }
      .get

    val value = row.get(index)
    val thriftUnionFieldInfo = unionFields(index)

    val element = convertRowElmToScroogeElm(value, thriftUnionFieldInfo.structFieldInfo.fieldInfo, codec)

    unionBuilders.build[T](codec, fieldName, element)
  }

  private def convertStruct[T <: ThriftStruct with Product](row: Row, codecWithBuilder: ThriftStructCodec[T] with StructBuilderFactory[T])
    (implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): T = {
    val fieldInfos: Seq[ThriftStructFieldInfo] = ScroogeHelpers.getFieldInfos(codecWithBuilder)
    val builder = codecWithBuilder.newBuilder()

    for ((fieldInfo, index) <- fieldInfos.zipWithIndex) {
      if(row.isNullAt(index)) builder.setField(index, None)
      else {
        val element = row(index)
        val value = convertRowElmToScroogeElm(element, fieldInfo, codecWithBuilder)
        builder.setField(index, value)
      }
    }

    builder.build()
  }

  private def convertRowElmToScroogeElm(elm: Any, fieldInfo: ThriftStructFieldInfo, codec: ThriftStructCodec[_])
    (implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): Any = {
    val fieldType = fieldInfo.tfield.`type`

    fieldType match {
      // primitives
      case TType.BOOL => fieldInfo.convert(elm.asInstanceOf[java.lang.Boolean].booleanValue())
      case TType.BYTE => fieldInfo.convert(elm.asInstanceOf[java.lang.Number].byteValue())
      case TType.DOUBLE => fieldInfo.convert(elm.asInstanceOf[java.lang.Number].doubleValue())
      case TType.I16 => fieldInfo.convert(elm.asInstanceOf[java.lang.Number].shortValue())
      case TType.I32 => fieldInfo.convert(elm.asInstanceOf[java.lang.Number].intValue())
      case TType.I64 => fieldInfo.convert(elm.asInstanceOf[java.lang.Number].longValue())

      case TType.STRING  =>
        if(fieldInfo.manifest.runtimeClass.equals(classOf[String]))
          fieldInfo.convert(elm.asInstanceOf[String])
        else // when is Binary
          fieldInfo.convert(ByteBuffer.wrap(elm.asInstanceOf[Array[Byte]]))

      // struct/union
      case TType.STRUCT =>
        if(fieldInfo.manifest.runtimeClass.getName == codec.metaData.structClassName) {
          val codecAny = codec.asInstanceOf[ThriftStructCodec[_ <: ThriftStruct]]

          if(fieldInfo.isOptional) elm.asInstanceOf[Option[Array[Byte]]].map(s => ByteArrayThriftDecoder(codecAny, s))
          else ByteArrayThriftDecoder(codecAny, elm.asInstanceOf[Array[Byte]])
        }
        else {
          val structClass = fieldInfo.manifest.runtimeClass.asInstanceOf[ThriftStructWithProduct]
          fieldInfo.convert(convert(structClass, elm.asInstanceOf[Row]))
        }

      // collections
      case TType.LIST =>
        val seq = elm.asInstanceOf[scala.collection.Iterable[Any]].toSeq
        val thriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values", fieldInfo.valueManifest.get)
        val values = convertRowElmSeqToScroogeElmSeq(seq,thriftStructFieldInfo, codec)

        fieldInfo.convert(values.toList)

      case TType.SET =>
        val seq = elm.asInstanceOf[scala.collection.Iterable[Any]].toSeq
        val thriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values", fieldInfo.valueManifest.get)
        val values = convertRowElmSeqToScroogeElmSeq(seq,thriftStructFieldInfo, codec)

        fieldInfo.convert(values.toSet)

      case TType.MAP =>
        val keyManifest = fieldInfo.keyManifest.get
        val keyThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)

        val keys = elm match {
          case map: Map[_, _] => map.keys
          case mapRows: Iterable[_] => mapRows.map { case Row(k: Any, _) => k }
        }

        val vals = elm match {
          case map: Map[_, _] => map.values
          case mapRows: Iterable[_] => mapRows.map { case Row(_, v: Any) => v }
        }

        val keyVals =
          convertRowElmSeqToScroogeElmSeq(keys.toSeq, keyThriftStructFieldInfo, codec)
            .zip(convertRowElmSeqToScroogeElmSeq(vals.toSeq, valueThriftStructFieldInfo, codec))
            .toMap

        fieldInfo.convert(keyVals)

      // enumeration
      case TType.ENUM =>
        val enumValue = elm.asInstanceOf[String]

        ThriftEnumObject
          .forEnumClass(fieldInfo.manifest.runtimeClass.asSubclass(classOf[com.twitter.scrooge.ThriftEnum]))
          .valueOf(enumValue)

      // otherwise fail by throwing IllegalArgumentException
      case illegalType @ _ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }

  @inline
  private def convertRowElmSeqToScroogeElmSeq(
    seq: Seq[Any],
    fieldInfo: ThriftStructFieldInfo,
    codec: ThriftStructCodec[_]
  )(implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig): Seq[Any] = seq.map(convertRowElmToScroogeElm(_, fieldInfo, codec))

  implicit class RichFieldInfo(val fieldInfo: ThriftStructFieldInfo) extends AnyVal{
    def convert(value: Any): Any =
      if(fieldInfo.isOptional) Option(value) else value
  }

}
