package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.common.TProtocolFactoryType
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSchemaCompatibility, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.flink.util.InstantiationUtil

class ScroogeTypeSerializerSnapshot[T <: ThriftStruct]() extends TypeSerializerSnapshot[T] {

  @transient private var _codec: ThriftStructCodec[T] = _
  @transient private var _structClass: Class[T] = _
  @transient private var _protocolFactoryType: TProtocolFactoryType = _

  def this(structClass: Class[T],
           protocolFactoryType: TProtocolFactoryType) ={
    this()
    _structClass = structClass
    _protocolFactoryType = protocolFactoryType
    _codec = ThriftStructCodec.forStructClass(structClass)
  }

  override def getCurrentVersion: Int = 1

  override def writeSnapshot(out: DataOutputView): Unit = out.writeUTF(_codec.metaData.structClassName)

  override def readSnapshot(readVersion: Int, in: DataInputView, userCodeClassLoader: ClassLoader): Unit = {
    val clazz = InstantiationUtil.resolveClassByName[T](in, userCodeClassLoader)
    require(clazz.getName == _codec.metaData.structClassName)
  }

  override def restoreSerializer(): TypeSerializer[T] = ScroogeTypeSerializer(_structClass, _protocolFactoryType)

  override def resolveSchemaCompatibility(newSerializer: TypeSerializer[T]): TypeSerializerSchemaCompatibility[T] =
    TypeSerializerSchemaCompatibility.compatibleAsIs()
}
