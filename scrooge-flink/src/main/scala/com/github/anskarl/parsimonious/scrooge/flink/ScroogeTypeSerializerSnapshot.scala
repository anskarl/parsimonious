package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.common.{TCompactProtocolFactoryType, TProtocolFactoryType}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSchemaCompatibility, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.flink.util.InstantiationUtil

class ScroogeTypeSerializerSnapshot[T <: ThriftStruct]() extends TypeSerializerSnapshot[T] {

  @transient private var _codec: ThriftStructCodec[T] = _
  @transient private var _structClass: Class[T] = _

  //todo: either use a global config or switch to a default protocol factory
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
    val _structClass = InstantiationUtil.resolveClassByName[T](in, userCodeClassLoader)
    _codec = ThriftStructCodec.forStructClass(_structClass)

    _protocolFactoryType = TCompactProtocolFactoryType
  }

  override def restoreSerializer(): TypeSerializer[T] = ScroogeTypeSerializer(_structClass, _protocolFactoryType)

  override def resolveSchemaCompatibility(newSerializer: TypeSerializer[T]): TypeSerializerSchemaCompatibility[T] =
    TypeSerializerSchemaCompatibility.compatibleAsIs()
}
