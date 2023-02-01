package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.TBaseType
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSchemaCompatibility, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.flink.util.InstantiationUtil

class ThriftTypeSerializerSnapshot[T <: TBaseType]() extends TypeSerializerSnapshot[T] {

  @transient private var tbaseClass: Class[T] = _

  def this(tbaseClass: Class[T]) ={
    this()
    this.tbaseClass = tbaseClass
  }

  override def getCurrentVersion: Int = 1

  override def writeSnapshot(out: DataOutputView): Unit =
    out.writeUTF(tbaseClass.getName)

  override def readSnapshot(readVersion: Int, in: DataInputView, userCodeClassLoader: ClassLoader): Unit = {
    tbaseClass = InstantiationUtil.resolveClassByName[T](in, userCodeClassLoader)
  }

  override def restoreSerializer(): TypeSerializer[T] = ThriftTypeSerializer(tbaseClass)

  override def resolveSchemaCompatibility(newSerializer: TypeSerializer[T]): TypeSerializerSchemaCompatibility[T] =
    TypeSerializerSchemaCompatibility.compatibleAsIs()
}