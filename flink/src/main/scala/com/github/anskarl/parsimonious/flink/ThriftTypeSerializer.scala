package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.TBaseType
import org.apache.flink.api.common.typeutils.{TypeSerializer, TypeSerializerSnapshot}
import org.apache.flink.api.java.typeutils.runtime.{DataInputViewStream, DataOutputViewStream}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.thrift.{TBase, TDeserializer, TSerializer}
import org.apache.thrift.transport.{TFramedTransport, TTransportException}

import java.io.InputStream

case class ThriftTypeSerializer[T <: TBaseType](
    tbaseClass: Class[T],
    protocolFactoryType: TProtocolFactoryType
  ) extends TypeSerializer[T] {

  @transient private lazy val protocolSerializer: TSerializer = new TSerializer(protocolFactoryType.create())
  @transient private lazy val protocolDeserializer: TDeserializer = new TDeserializer(protocolFactoryType.create())
  @transient private final val FrameMetaSizeBytes = 4

  override def isImmutableType: Boolean = false
  override def duplicate(): TypeSerializer[T] = ThriftTypeSerializer(tbaseClass, protocolFactoryType)
  override def createInstance(): T = tbaseClass.getConstructor().newInstance()

  override def copy(from: T): T = from.deepCopy().asInstanceOf[T]
  override def copy(from: T, reuse: T): T = from.deepCopy().asInstanceOf[T]
  override def getLength: Int = -1

  override def serialize(record: T, target: DataOutputView): Unit = {

    val outputStream = new DataOutputViewStream(target)

    // Serialize thrift record
    val recordSerializedBytes = protocolSerializer.serialize(record.asInstanceOf[TBase[_,_]])

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
    val instance = this.createInstance()
    protocolDeserializer.deserialize(instance.asInstanceOf[TBase[_,_]], bytes)

    instance
  }

  override def deserialize(reuse: T, source: DataInputView): T = this.deserialize(source)

  override def copy(source: DataInputView, target: DataOutputView): Unit = this.serialize(deserialize(source), target)

  override def snapshotConfiguration(): TypeSerializerSnapshot[T] = new ThriftTypeSerializerSnapshot(tbaseClass, protocolFactoryType)

  /**
    * Helper function to extract the bytes of a thrift serialized record that is
    * contained in the thrift framed message (see TFramedTransport)
    *
    * In each frame, the first 4 bytes contain the size of the serialized record.
    * Therefore, this function reads the first 4 bytes to determine the number of bytes
    * that will read from the given inputStream in order to return the bytes of the
    * record
    *
    * @param inputStream the input stream to read
    *
    * @return an array of bytes that represent a single thrift serialized record (i.e., struct or union)
    */
  private def getRecordBytes(inputStream: InputStream): Array[Byte] = {
    val frameSizeBuffer = inputStream.readNBytes(FrameMetaSizeBytes) // todo: compiles only from Java 11+, should replace this for maximum compatibility (e.g., java 8+)
    val size = TFramedTransport.decodeFrameSize(frameSizeBuffer)

    if (size < 0) throw new TTransportException(s"Read a negative frame size ($size)!")

    val buffer = new Array[Byte](size)
    inputStream.read(buffer)
    buffer
  }
}