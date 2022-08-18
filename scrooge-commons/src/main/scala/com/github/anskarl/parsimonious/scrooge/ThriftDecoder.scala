package com.github.anskarl.parsimonious.scrooge

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.transport.{TByteBuffer, TMemoryInputTransport, TTransport}
import com.github.anskarl.parsimonious.common.ParsimoniousConfig

import java.nio.ByteBuffer

trait ThriftDecoder {

  type Input
  type Transport <: TTransport
  type Protocol <: TProtocol

  def createTransport(input: Input)(implicit parsimoniousConfig: ParsimoniousConfig): Transport

  def createProtocol[T <: ThriftStruct](codec: ThriftStructCodec[T], transport: Transport)(implicit parsimoniousConfig: ParsimoniousConfig): Protocol

  def apply[T <: ThriftStruct](codec: ThriftStructCodec[T], input: Input)(implicit parsimoniousConfig: ParsimoniousConfig): T
}

trait BaseThriftDecoder extends ThriftDecoder {
  
  type Protocol = TProtocol

  override def createProtocol[T <: ThriftStruct](codec: ThriftStructCodec[T], transport: Transport)(implicit parsimoniousConfig: ParsimoniousConfig): Protocol = {
    parsimoniousConfig.protocolFactory.getProtocol(transport)
  }

  override def apply[T <: ThriftStruct](codec: ThriftStructCodec[T], input: Input)(implicit parsimoniousConfig: ParsimoniousConfig): T = {
    val transport = createTransport(input)
    val protocol = createProtocol(codec, transport)
    codec.decode(protocol)
  }
}

object ByteArrayThriftDecoder extends BaseThriftDecoder {

  type Input = Array[Byte]
  type Transport = TMemoryInputTransport

  override def createTransport(input: Array[Byte])
    (implicit parsimoniousConfig: ParsimoniousConfig): TMemoryInputTransport = new TMemoryInputTransport(input)
}

object ByteBufferThriftDecoder extends BaseThriftDecoder {

  type Input = ByteBuffer
  type Transport = TByteBuffer

  override def createTransport(input: ByteBuffer)
    (implicit parsimoniousConfig: ParsimoniousConfig): TByteBuffer = new TByteBuffer(input)
}

