package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.TBaseType
import com.github.anskarl.parsimonious.common.{TCompactProtocolFactoryType, TProtocolFactoryType}
import com.github.anskarl.parsimonious.flink.common.ThriftFlinkSerdeHelpers
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSnapshot}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.thrift.{TBase, TDeserializer, TSerializer}

case class ThriftTypeSerializer[T <: TBaseType](
    tbaseClass: Class[T],
  ) extends TypeSerializer[T] {

  @transient private lazy val protocolSerializer: TSerializer = new TSerializer(TCompactProtocolFactoryType.create())
  @transient private lazy val protocolDeserializer: TDeserializer = new TDeserializer(TCompactProtocolFactoryType.create())

  override def isImmutableType: Boolean = false
  override def duplicate(): TypeSerializer[T] = ThriftTypeSerializer(tbaseClass)
  override def createInstance(): T = tbaseClass.getConstructor().newInstance()

  override def copy(from: T): T = from.deepCopy().asInstanceOf[T]
  override def copy(from: T, reuse: T): T = from.deepCopy().asInstanceOf[T]
  override def getLength: Int = -1

  override def serialize(record: T, target: DataOutputView): Unit = {
    val recordSerializedBytes = protocolSerializer.serialize(record.asInstanceOf[TBase[_,_]])
    ThriftFlinkSerdeHelpers.writeMessageFrame(target, recordSerializedBytes)
  }

  override def deserialize(source: DataInputView): T = {
    val recordSerializedBytes = ThriftFlinkSerdeHelpers.readMessage(source)
    val instance = this.createInstance()
    protocolDeserializer.deserialize(instance.asInstanceOf[TBase[_,_]], recordSerializedBytes)
    instance
  }

  override def deserialize(reuse: T, source: DataInputView): T = this.deserialize(source)

  override def copy(source: DataInputView, target: DataOutputView): Unit = this.serialize(deserialize(source), target)

  override def snapshotConfiguration(): TypeSerializerSnapshot[T] = new ThriftTypeSerializerSnapshot(tbaseClass)

}