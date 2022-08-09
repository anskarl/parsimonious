package com.github.anskarl.parsimonious

import org.apache.thrift.protocol.{TCompactProtocol, TProtocolFactory}

package object scrooge {

  type ThriftStructWithProduct = Class[_ <: com.twitter.scrooge.ThriftStruct with Product]

  case class ScroogeConfig(
    keyName: String = "key",
    valName: String = "value",
    sizeHint: Int = 512,
    protocolFactory: TProtocolFactory = new TCompactProtocol.Factory
  )

}
