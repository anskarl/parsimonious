package com.github.anskarl.parsimonious.scrooge.spark

import com.github.anskarl.parsimonious.scrooge.{ByteArrayThriftEncoder, Constants, ScroogeHelpers}
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftStructCodec, ThriftStructFieldInfo, ThriftUnion}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.apache.thrift.protocol.TType
import java.nio.ByteBuffer
import scala.reflect.ClassTag

trait ScroogeRowConverterSchema {

  def extractSchema[T <: ThriftStruct with Product: ClassTag](structClass: Class[T]): StructType ={
    val codec = com.twitter.scrooge.ThriftStructCodec.forStructClass(structClass)
    val fieldInfos: Seq[ThriftStructFieldInfo] = ScroogeHelpers.getFieldInfos(codec)

    val fields: Seq[StructField] = fieldInfos
      .map { entry: ThriftStructFieldInfo =>
        StructField(
          name = entry.tfield.name,
          dataType =
            if(entry.manifest.runtimeClass.getName == codec.metaData.structClassName) BinaryType
            else convertThriftFieldToDataType(entry),
          nullable = !entry.isRequired
        )
      }

    StructType(fields)
  }

  protected def convertThriftFieldToDataType(fieldInfo: ThriftStructFieldInfo): DataType = {
    fieldInfo.tfield.`type` match {
      // Recursive Cases
      case TType.LIST | TType.SET =>
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values", valueManifest)
        ArrayType(convertThriftFieldToDataType(valueThriftStructFieldInfo))

      case TType.MAP =>
        val keyManifest = fieldInfo.keyManifest.get
        val keyThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)

        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)

        val keyDataType = convertThriftFieldToDataType(keyThriftStructFieldInfo)
        val valueDataType = convertThriftFieldToDataType(valueThriftStructFieldInfo)

        // If the key is not primitive, we convert the map to a list of key/value struct
        if (ScroogeHelpers.isPrimitive(keyThriftStructFieldInfo))
          MapType(keyDataType, valueDataType)
        else ArrayType(
          StructType(Seq(StructField(Constants.keyName, keyDataType), StructField(Constants.valName, valueDataType))),
          containsNull = false
        )

      case TType.STRUCT =>
        extractSchema(fieldInfo.manifest.runtimeClass.asInstanceOf[Class[_ <: ThriftStruct with Product]])

      // Base Cases
      case TType.BOOL => BooleanType
      case TType.BYTE => ByteType
      case TType.DOUBLE => DoubleType
      case TType.I16 => ShortType
      case TType.I32 => IntegerType
      case TType.I64 => LongType
      case TType.ENUM => StringType
      case TType.STRING =>
        if(!classOf[String].equals(fieldInfo.manifest.runtimeClass)) BinaryType
        else StringType
      case illegalType@_ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }
}

object ScroogeRowConverter extends ScroogeRowConverterSchema {

  def convert[T <: ThriftStruct with Product](instance: T): Row = {
    val codec = com.twitter.scrooge.ThriftStructCodec.forStructClass(instance.getClass)
    val fieldInfos: Seq[ThriftStructFieldInfo] = ScroogeHelpers.getFieldInfos(codec)

    if(codec.metaData.unionFields.isEmpty)
      convertStruct(instance, fieldInfos,  codec.asInstanceOf[ThriftStructCodec[ThriftStruct]])
    else
      convertUnion(instance.asInstanceOf[ThriftUnion], fieldInfos, codec.asInstanceOf[ThriftStructCodec[ThriftStruct]])
  }

  def convertStruct[T <: ThriftStruct with Product](
    instance: T,
    fieldInfos: Seq[ThriftStructFieldInfo],
    codec: ThriftStructCodec[ThriftStruct]): Row ={

    val elms = fieldInfos.zipWithIndex.map{ entry =>
      val (fieldInfo: ThriftStructFieldInfo, index: Int) = entry
      val elm = instance.productElement(index)
      convertElmToRowElm(elm, fieldInfo,  codec)
    }

    Row.fromSeq(elms)
  }

  private def convertUnion[T <: ThriftUnion](instance: T, fieldInfos: Seq[ThriftStructFieldInfo],  codec: ThriftStructCodec[ThriftStruct]): Row = {
    val instanceFieldInfo = instance.unionStructFieldInfo.get
    val instanceFieldName = instanceFieldInfo.tfield.name

    val elms = fieldInfos.map{ info =>
      val name = info.tfield.name

      if(name == instanceFieldName)
        convertElmToRowElm(instance.containedValue(), info, codec)
      else null
    }
    Row.fromSeq(elms)
  }

  private def decodeSingle[T](elm: Any, fieldInfo: ThriftStructFieldInfo): Option[T] =
    if(fieldInfo.isOptional) elm.asInstanceOf[Option[T]] else Option(elm.asInstanceOf[T])

  private def convertElmSeqToRowElmSeq(seq: Seq[Any], fieldInfo: ThriftStructFieldInfo, codec: ThriftStructCodec[ThriftStruct]): Seq[Any] =
    seq.map(Option(_)).map(_.map(convertElmToRowElm(_, fieldInfo, codec)).orNull)

  def extractIterElements(elm: Any, isOptional: Boolean, fieldInfo: ThriftStructFieldInfo, codec: ThriftStructCodec[ThriftStruct]): Option[Iterable[Any]] = {
    if (isOptional)
      elm.asInstanceOf[Option[Iterable[Any]]].map(_.map(e => convertElmToRowElm(e, fieldInfo, codec)))
    else
      Option(elm.asInstanceOf[Iterable[Any]].map(e => convertElmToRowElm(e, fieldInfo, codec)))
  }

  def convertElmToRowElm(elm: Any, fieldInfo: ThriftStructFieldInfo, codec: ThriftStructCodec[ThriftStruct]): Any ={
    fieldInfo.tfield.`type` match {
      // Recursive Cases
      case TType.LIST | TType.SET=>
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values", valueManifest)
        val elementsOpt = extractIterElements(elm,fieldInfo.isOptional,valueThriftStructFieldInfo,codec)
        elementsOpt.map(_.toSeq).orNull


      case TType.MAP =>
        val keyManifest = fieldInfo.keyManifest.get
        val keyThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)

        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)

        val mapOpt = if(fieldInfo.isOptional) elm.asInstanceOf[Option[Map[Any, Any]]] else Option(elm.asInstanceOf[Map[Any, Any]])
        mapOpt.map{ map =>
          val keys: Seq[Any] = convertElmSeqToRowElmSeq(map.keys.toSeq, keyThriftStructFieldInfo, codec)
          val vals: Seq[Any] = convertElmSeqToRowElmSeq(map.values.toSeq, valueThriftStructFieldInfo, codec)
          val keyVals = keys.zip(vals)

          // If the key is not primitive, we convert the map to a list of key/value struct
          if (ScroogeHelpers.isPrimitive(keyThriftStructFieldInfo)) Map(keyVals: _*)
          else keyVals.map(tuple => Row(tuple._1, tuple._2))
        }.orNull


      case TType.STRUCT =>
        if(fieldInfo.manifest.runtimeClass.getName == codec.metaData.structClassName){
          if(fieldInfo.isOptional) elm.asInstanceOf[Option[ThriftStruct]].map(s => ByteArrayThriftEncoder(s))
          else ByteArrayThriftEncoder(elm.asInstanceOf[ThriftStruct])
        }
        else {
          if(fieldInfo.isOptional) elm.asInstanceOf[Option[ThriftStruct with Product]].map(value => convert(value)).orNull
          else convert(elm.asInstanceOf[ThriftStruct with Product])
        }


      // Base Cases
      case TType.BOOL => decodeSingle[Boolean](elm, fieldInfo).orNull
      case TType.BYTE => decodeSingle[Byte](elm, fieldInfo).orNull
      case TType.DOUBLE => decodeSingle[Double](elm, fieldInfo).orNull
      case TType.I16 => decodeSingle[Short](elm, fieldInfo).orNull
      case TType.I32 => decodeSingle[Int](elm, fieldInfo).orNull
      case TType.I64 => decodeSingle[Long](elm, fieldInfo).orNull
      case TType.ENUM => decodeSingle[ThriftEnum](elm, fieldInfo).map(_.name).orNull
      case TType.STRING =>
        if(!classOf[String].equals(fieldInfo.manifest.runtimeClass)) decodeSingle[ByteBuffer](elm, fieldInfo).map(_.array()).orNull
        else decodeSingle[String](elm, fieldInfo).orNull
      case illegalType@_ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")
    }
  }


}
