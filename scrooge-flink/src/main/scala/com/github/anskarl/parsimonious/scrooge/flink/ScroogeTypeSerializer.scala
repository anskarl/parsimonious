package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.common.{ParsimoniousConfig, TProtocolFactoryType}
import com.github.anskarl.parsimonious.flink.common.ThriftFlinkSerdeHelpers
import com.github.anskarl.parsimonious.scrooge.{ByteArrayThriftDecoder, ByteArrayThriftEncoder}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}


case class ScroogeTypeSerializer[T <: ThriftStruct](
  structClass: Class[T],
  protocolFactoryType: TProtocolFactoryType
  ) extends TypeSerializer[T] {

  @transient private lazy val codec = ThriftStructCodec.forStructClass(structClass)
  @transient private implicit lazy val parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig(protocolFactoryType = protocolFactoryType)
  
  override def isImmutableType: Boolean = true
  override def duplicate(): TypeSerializer[T] = ScroogeTypeSerializer(structClass, protocolFactoryType)
  override def createInstance(): T = codec.metaData.structClass.getDeclaredConstructor().newInstance()

  override def copy(from: T): T = from
  override def copy(from: T, reuse: T): T = from

  override def getLength: Int = -1

  override def serialize(record: T, target: DataOutputView): Unit = {
    val recordSerializedBytes = ByteArrayThriftEncoder(record)
    ThriftFlinkSerdeHelpers.writeMessageFrame(target, recordSerializedBytes)
  }

  override def deserialize(source: DataInputView): T = {
    val recordSerializedBytes = ThriftFlinkSerdeHelpers.readMessage(source)
    ByteArrayThriftDecoder(codec, recordSerializedBytes)
  }

  override def deserialize(reuse: T, source: DataInputView): T = this.deserialize(source)

  override def copy(source: DataInputView, target: DataOutputView): Unit =
    this.serialize(deserialize(source), target)

  override def snapshotConfiguration(): TypeSerializerSnapshot[T] = ScroogeTypeSerializerSnapshot(structClass, protocolFactoryType)

}
