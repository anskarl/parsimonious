package com.github.anskarl.parsimonious.scrooge

import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct}
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.transport.{TByteBuffer, TTransport}
import com.github.anskarl.parsimonious.common.ParsimoniousConfig

import java.nio.{ByteBuffer => NByteBuffer}

trait ThriftEncoder {
  type Output
  type Transport <: TTransport

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int)(implicit parsimoniousConfig: ParsimoniousConfig): Transport

  def createProtocol[T <: ThriftStruct](obj: T, transport: Transport)(implicit parsimoniousConfig: ParsimoniousConfig): TProtocol

  def getOutput(transport: Transport)(implicit parsimoniousConfig: ParsimoniousConfig): Output

  final def createTransport[T <: ThriftStruct](obj: T)(implicit parsimoniousConfig: ParsimoniousConfig): Transport =
    createTransport(obj, parsimoniousConfig.sizeHint)

  final def apply[T <: ThriftStruct](obj: T)(implicit parsimoniousConfig: ParsimoniousConfig): Output = {
    val transport = createTransport(obj, parsimoniousConfig.sizeHint)
    val protocol = createProtocol(obj, transport)
    obj.write(protocol)
    getOutput(transport)
  }
}

object ByteArrayThriftEncoder extends ThriftEncoder {
  type Output = Array[Byte]
  type Transport = TArrayByteTransport

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int)
    (implicit parsimoniousConfig: ParsimoniousConfig): TArrayByteTransport = new TArrayByteTransport(sizeHint)

  def createProtocol[T <: ThriftStruct](obj: T, transport: TArrayByteTransport)(implicit parsimoniousConfig: ParsimoniousConfig): TProtocol =
    parsimoniousConfig.protocolFactory.getProtocol(transport)

  def getOutput(transport: TArrayByteTransport)(implicit parsimoniousConfig: ParsimoniousConfig): Array[Byte] = transport.toByteArray
}

object ByteBufferThriftEncoder extends ThriftEncoder {
  type Output = NByteBuffer
  type Transport = TByteBuffer

  def createTransport[T <: ThriftStruct](obj: T, sizeHint: Int)(implicit parsimoniousConfig: ParsimoniousConfig): TByteBuffer =
    new TByteBuffer(NByteBuffer.allocate(sizeHint))

  def createProtocol[T <: ThriftStruct](obj: T, transport: TByteBuffer)(implicit parsimoniousConfig: ParsimoniousConfig): TProtocol =
    parsimoniousConfig.protocolFactory.getProtocol(transport)

  def getOutput(transport: TByteBuffer)(implicit parsimoniousConfig: ParsimoniousConfig): NByteBuffer = transport.getByteBuffer
}
