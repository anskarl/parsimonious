package com.github.anskarl.parsimonious.flink.common

import org.apache.flink.api.java.typeutils.runtime.{DataInputViewStream, DataOutputViewStream}
import org.apache.flink.core.memory.{DataInputView, DataOutputView}
import org.apache.thrift.transport.{TFramedTransport, TTransportException}

object ThriftFlinkSerdeHelpers {

  final val FrameMetaSizeBytes = 4

  def writeMessageFrame(target: DataOutputView, recordSerializedBytes: Array[Byte]): Unit ={
    val outputStream = new DataOutputViewStream(target)
    val framedMessage = new Array[Byte](FrameMetaSizeBytes + recordSerializedBytes.length)

    // In the first 4 bytes we prepend the size of the serialised record
    TFramedTransport.encodeFrameSize(recordSerializedBytes.length, framedMessage)

    // Then append with the bytes of the serialized record
    System.arraycopy(recordSerializedBytes,0, framedMessage, FrameMetaSizeBytes, recordSerializedBytes.length)

    // write the resulting framed message to the target output stream
    outputStream.write(framedMessage)
  }

  /**
   * Helper function to extract the bytes of a thrift serialized record that is
   * contained in the thrift framed message (see TFramedTransport)
   *
   * In each frame, the first 4 bytes contain the size of the serialized record.
   * Therefore, this function reads the first 4 bytes to determine the number of bytes
   * that will read from the given inputStream in order to return the bytes of the
   * record
   *
   * @param source: the input stream to read
   *
   * @return an array of bytes that represent a single thrift serialized record (i.e., struct or union)
   */
  def readMessage(source: DataInputView): Array[Byte] = {
    val inputStream = new DataInputViewStream(source)
    val frameSizeBuffer = new Array[Byte](FrameMetaSizeBytes)
    inputStream.read(frameSizeBuffer,0,FrameMetaSizeBytes)
    val size = TFramedTransport.decodeFrameSize(frameSizeBuffer)

    if (size < 0) throw new TTransportException(s"Read a negative frame size ($size)!")

    val buffer = new Array[Byte](size)
    inputStream.read(buffer)
    buffer
  }

}
