package com.github.anskarl.parsimonious.scrooge

import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.transport.{TByteBuffer, TMemoryInputTransport, TTransport}

import java.nio.ByteBuffer

trait ThriftDecoder {
  type Input
  type Transport <: TTransport
  type Protocol <: TProtocol

  def createTransport(input: Input): Transport

  def createProtocol[T <: ThriftStruct](codec: ThriftStructCodec[T], transport: Transport): Protocol

  def apply[T <: ThriftStruct](codec: ThriftStructCodec[T], input: Input): T
}

trait BaseThriftDecoder extends ThriftDecoder {
  
  type Protocol = TProtocol

  override def createProtocol[T <: ThriftStruct](codec: ThriftStructCodec[T], transport: Transport): Protocol = {
    Constants.DefaultProtocolFactory.getProtocol(transport)
  }

  override def apply[T <: ThriftStruct](codec: ThriftStructCodec[T], input: Input): T = {
    val transport = createTransport(input)
    val protocol = createProtocol(codec, transport)
    codec.decode(protocol)
  }
}

object ByteArrayThriftDecoder extends BaseThriftDecoder {

  type Input = Array[Byte]
  type Transport = TMemoryInputTransport

  override def createTransport(input: Array[Byte]): TMemoryInputTransport =
    new TMemoryInputTransport(input)
}

object ByteBufferThriftDecoder extends BaseThriftDecoder {

  type Input = ByteBuffer
  type Transport = TByteBuffer

  override def createTransport(input: ByteBuffer): TByteBuffer =
    new TByteBuffer(input)
}

