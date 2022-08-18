package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.common.TProtocolFactoryType
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSchemaCompatibility, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.flink.util.InstantiationUtil

case class ScroogeTypeSerializerSnapshot[T <: ThriftStruct](
  structClass: Class[T],
  protocolFactoryType: TProtocolFactoryType
  ) extends TypeSerializerSnapshot[T] {

  @transient private lazy val codec = ThriftStructCodec.forStructClass(structClass)

  override def getCurrentVersion: Int = 1

  override def writeSnapshot(out: DataOutputView): Unit = out.writeUTF(codec.metaData.structClassName)

  override def readSnapshot(readVersion: Int, in: DataInputView, userCodeClassLoader: ClassLoader): Unit = {
    val clazz = InstantiationUtil.resolveClassByName[T](in, userCodeClassLoader)
    require(clazz.getName == codec.metaData.structClassName)
  }

  override def restoreSerializer(): TypeSerializer[T] = ScroogeTypeSerializer(structClass, protocolFactoryType)

  override def resolveSchemaCompatibility(newSerializer: TypeSerializer[T]): TypeSerializerSchemaCompatibility[T] =
    TypeSerializerSchemaCompatibility.compatibleAsIs()
}
