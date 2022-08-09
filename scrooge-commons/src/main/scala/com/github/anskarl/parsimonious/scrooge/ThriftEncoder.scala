package com.github.anskarl.parsimonious.scrooge

import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct}
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.transport.{TByteBuffer, TTransport}

import java.nio.{ByteBuffer => NByteBuffer}

trait ThriftEncoder {
  type Output
  type Transport <: TTransport

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int)(implicit scroogeConfig: ScroogeConfig): Transport

  def createProtocol[T <: ThriftStruct](obj: T, transport: Transport)(implicit scroogeConfig: ScroogeConfig): TProtocol

  def getOutput(transport: Transport)(implicit scroogeConfig: ScroogeConfig): Output

  final def createTransport[T <: ThriftStruct](obj: T)(implicit scroogeConfig: ScroogeConfig): Transport =
    createTransport(obj, scroogeConfig.sizeHint)

  final def apply[T <: ThriftStruct](obj: T)(implicit scroogeConfig: ScroogeConfig): Output = {
    val transport = createTransport(obj, scroogeConfig.sizeHint)
    val protocol = createProtocol(obj, transport)
    obj.write(protocol)
    getOutput(transport)
  }
}

object ByteArrayThriftEncoder extends ThriftEncoder {
  type Output = Array[Byte]
  type Transport = TArrayByteTransport

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int)
    (implicit scroogeConfig: ScroogeConfig): TArrayByteTransport = new TArrayByteTransport(sizeHint)

  def createProtocol[T <: ThriftStruct](obj: T, transport: TArrayByteTransport)(implicit scroogeConfig: ScroogeConfig): TProtocol =
    scroogeConfig.protocolFactory.getProtocol(transport)

  def getOutput(transport: TArrayByteTransport)(implicit scroogeConfig: ScroogeConfig): Array[Byte] = transport.toByteArray
}

object ByteBufferThriftEncoder extends ThriftEncoder {
  type Output = NByteBuffer
  type Transport = TByteBuffer

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int)(implicit scroogeConfig: ScroogeConfig): TByteBuffer =
    new TByteBuffer(NByteBuffer.allocate(sizeHint))

  def createProtocol[T <: ThriftStruct](obj: T, transport: TByteBuffer)(implicit scroogeConfig: ScroogeConfig): TProtocol =
    scroogeConfig.protocolFactory.getProtocol(transport)

  def getOutput(transport: TByteBuffer)(implicit scroogeConfig: ScroogeConfig): NByteBuffer = transport.getByteBuffer
}
