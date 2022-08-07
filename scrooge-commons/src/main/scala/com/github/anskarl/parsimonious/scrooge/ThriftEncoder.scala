package com.github.anskarl.parsimonious.scrooge

import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct}
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.transport.{TByteBuffer, TTransport}

import java.nio.{ByteBuffer => NByteBuffer}

trait ThriftEncoder {
  type Output
  type Transport <: TTransport

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int): Transport

  def createProtocol[T <: ThriftStruct](obj: T, transport: Transport): TProtocol

  def getOutput(transport: Transport): Output

  final def createTransport[T <: ThriftStruct](obj: T): Transport = createTransport(obj, Constants.DefaultSizeHint)

  final def apply[T <: ThriftStruct](obj: T, sizeHint: Int): Output = {

    val transport = createTransport(obj, sizeHint)
    val protocol = createProtocol(obj, transport)
    obj.write(protocol)
    getOutput(transport)

  }

  final def apply[T <: ThriftStruct](obj: T): Output = apply(obj, Constants.DefaultSizeHint)
}

object ByteArrayThriftEncoder extends ThriftEncoder {
  type Output = Array[Byte]
  type Transport = TArrayByteTransport

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int): TArrayByteTransport =
    new TArrayByteTransport(sizeHint)

  def createProtocol[T <: ThriftStruct](obj: T, transport: TArrayByteTransport): TProtocol =
    Constants.DefaultProtocolFactory.getProtocol(transport)

  def getOutput(transport: TArrayByteTransport): Array[Byte] = transport.toByteArray
}

object ByteBufferThriftEncoder extends ThriftEncoder {
  type Output = NByteBuffer
  type Transport = TByteBuffer

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int): TByteBuffer =
    new TByteBuffer(NByteBuffer.allocate(sizeHint))

  def createProtocol[T <: ThriftStruct](obj: T, transport: TByteBuffer): TProtocol =
    Constants.DefaultProtocolFactory.getProtocol(transport)

  def getOutput(transport: TByteBuffer): NByteBuffer = transport.getByteBuffer
}
