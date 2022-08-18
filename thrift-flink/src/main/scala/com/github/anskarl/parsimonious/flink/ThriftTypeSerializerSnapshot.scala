package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.TBaseType
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSchemaCompatibility, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.flink.util.InstantiationUtil
import com.github.anskarl.parsimonious.common.TProtocolFactoryType

class ThriftTypeSerializerSnapshot[T <: TBaseType](
    tbaseClass: Class[T],
    protocolFactoryType: TProtocolFactoryType
  ) extends TypeSerializerSnapshot[T] {

  override def getCurrentVersion: Int = 1

  override def writeSnapshot(out: DataOutputView): Unit =
    out.writeUTF(tbaseClass.getName)

  override def readSnapshot(readVersion: Int, in: DataInputView, userCodeClassLoader: ClassLoader): Unit = {
    val clazz = InstantiationUtil.resolveClassByName[T](in, userCodeClassLoader)
    require(clazz.getName == tbaseClass.getName)
  }

  override def restoreSerializer(): TypeSerializer[T] = ThriftTypeSerializer(tbaseClass, protocolFactoryType)

  override def resolveSchemaCompatibility(newSerializer: TypeSerializer[T]): TypeSerializerSchemaCompatibility[T] =
    TypeSerializerSchemaCompatibility.compatibleAsIs()
}