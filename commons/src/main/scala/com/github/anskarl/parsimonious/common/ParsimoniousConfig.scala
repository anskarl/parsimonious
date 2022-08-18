package com.github.anskarl.parsimonious.common

import org.apache.thrift.protocol.TProtocolFactory
import org.apache.thrift.{TDeserializer, TSerializer}

case class ParsimoniousConfig(
    keyName: String = "key",
    valName: String = "value",
    sizeHint: Int = 512,
    protocolFactoryType: TProtocolFactoryType = TCompactProtocolFactoryType
){
  @transient
  lazy val protocolFactory: TProtocolFactory = protocolFactoryType.create()

  @transient
  lazy val protocolSerializer: TSerializer = new TSerializer(protocolFactoryType.create())

  @transient
  lazy val protocolDeserializer: TDeserializer = new TDeserializer(protocolFactoryType.create())
}
