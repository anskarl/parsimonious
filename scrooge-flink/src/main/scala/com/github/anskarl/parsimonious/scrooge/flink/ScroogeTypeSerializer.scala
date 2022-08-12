package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.scrooge.{ByteArrayThriftDecoder, ByteArrayThriftEncoder, ScroogeConfig}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSnapshot}
import org.apache.flink.api.java.typeutils.runtime.{DataInputViewStream, DataOutputViewStream}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.thrift.transport.{TFramedTransport, TTransportException}

import java.io.InputStream

// todo: add functionality to able to change the protocol like ThriftTypeSerializer
case class ScroogeTypeSerializer[T <: ThriftStruct](structClass: Class[T]) extends TypeSerializer[T] {

  @transient private lazy val codec = ThriftStructCodec.forStructClass(structClass)
  @transient private final val FrameMetaSizeBytes = 4
  
  override def isImmutableType: Boolean = true

  override def duplicate(): TypeSerializer[T] = ScroogeTypeSerializer(structClass)

  override def createInstance(): T = codec.metaData.structClass.getDeclaredConstructor().newInstance()

  override def copy(from: T): T = from

  override def copy(from: T, reuse: T): T = from

  override def getLength: Int = -1

  override def serialize(record: T, target: DataOutputView): Unit = {
    val outputStream = new DataOutputViewStream(target)

    // Serialize thrift record
    // todo: compared to ThriftTypeSerializer.serialize only th following line is different, should move to a common place
    val recordSerializedBytes = ByteArrayThriftEncoder(record)(ScroogeConfig()) //todo fix this, should not create a new ScroogeConfig each time

    val framedMessage = new Array[Byte](FrameMetaSizeBytes + recordSerializedBytes.length)

    // In the first 4 bytes we prepend the size of the serialised record
    TFramedTransport.encodeFrameSize(recordSerializedBytes.length, framedMessage)

    // Then append with the bytes of the serialized record
    System.arraycopy(recordSerializedBytes,0, framedMessage, FrameMetaSizeBytes, recordSerializedBytes.length)

    // write the resulting framed message to the target output stream
    outputStream.write(framedMessage)
  }

  override def deserialize(source: DataInputView): T = {
    val inputStream = new DataInputViewStream(source)
    val bytes = this.getRecordBytes(inputStream)
    // todo: compared to ThriftTypeSerializer.deserialize only th following line is different, should move to a common place
    ByteArrayThriftDecoder(codec,bytes)(ScroogeConfig()) // todo fix this, should not create a new ScroogeConfig each time
  }

  override def deserialize(reuse: T, source: DataInputView): T = this.deserialize(source)

  override def copy(source: DataInputView, target: DataOutputView): Unit =
    this.serialize(deserialize(source), target)

  override def snapshotConfiguration(): TypeSerializerSnapshot[T] = ScroogeTypeSerializerSnapshot(structClass)

  // todo: this is exactly the same with ThriftTypeSerializer::getRecordBytes, should move to a common trait/object
  private def getRecordBytes(inputStream: InputStream): Array[Byte] = {
    val frameSizeBuffer = inputStream.readNBytes(FrameMetaSizeBytes) // todo: compiles only from Java 11+, should replace this for maximum compatibility (e.g., java 8+)
    val size = TFramedTransport.decodeFrameSize(frameSizeBuffer)

    if (size < 0) throw new TTransportException(s"Read a negative frame size ($size)!")

    val buffer = new Array[Byte](size)
    inputStream.read(buffer)
    buffer
  }
}
