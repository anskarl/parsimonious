package com.github.anskarl.parsimonious

import org.apache.thrift.TSerializer
import org.apache.thrift.protocol.{TCompactProtocol, TField, TType}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftStructCodec, ThriftStructFieldInfo}

import java.nio.ByteBuffer


package object scrooge {

  type ThriftStructWithProduct = Class[_ <: com.twitter.scrooge.ThriftStruct with Product]

  private[scrooge] final val keyName = "key"
  private[scrooge] final val valName = "value"

  private[scrooge] final val DefaultTCompactProtocolSerializer = new TSerializer(new TCompactProtocol.Factory())

  private[scrooge] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)


  private[scrooge] def getFieldInfos[F <: ThriftStruct with Product](codec: ThriftStructCodec[F]): Seq[ThriftStructFieldInfo] = {
    val metadata = codec.metaData
    if(metadata.unionFields.nonEmpty && metadata.fieldInfos.isEmpty) metadata.unionFields.map(_.structFieldInfo)
    else metadata.fieldInfos
  }

  private[scrooge] def getThriftStructFieldInfo(name: String, typeManifest: Manifest[_]): ThriftStructFieldInfo ={
    val typeClass = typeManifest.runtimeClass

    val tField =
      if(typeClass == classOf[Boolean]) new TField(name, TType.BOOL, 0)
      else if (typeClass == classOf[Byte]) new TField(name, TType.BYTE, 0)
      else if (typeClass == classOf[Short]) new TField(name, TType.I16, 0)
      else if (typeClass == classOf[Int]) new TField(name, TType.I32, 0)
      else if (typeClass == classOf[Long]) new TField(name, TType.I64, 0)
      else if (typeClass == classOf[Double]) new TField(name, TType.DOUBLE, 0)
      else if (typeClass == classOf[String]) new TField(name, TType.STRING, 0)
      else if (typeClass == classOf[ByteBuffer]) new TField(name, TType.STRING, 0)
      else if (typeClass == classOf[Seq[_]]) new TField(name, TType.LIST, 0)
      else if (typeClass == classOf[Set[_]]) new TField(name, TType.SET, 0)
      else if (typeClass == classOf[Map[_,_]]) new TField(name, TType.MAP, 0)
      else if(classOf[ThriftEnum].isAssignableFrom(typeClass)) new TField(name, TType.ENUM, 0)
      else new TField(name, TType.STRUCT, 0)

    new ThriftStructFieldInfo(tField ,false, typeManifest, None, None)
  }

}
