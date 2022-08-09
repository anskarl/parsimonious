package com.github.anskarl.parsimonious

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.thrift.protocol.{TCompactProtocol, TProtocolFactory}


package object scrooge {

  type ThriftStructWithProduct = Class[_ <: com.twitter.scrooge.ThriftStruct with Product]

  private[scrooge] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  case class ScroogeConfig(
    keyName: String = "key",
    valName: String = "value",
    sizeHint: Int = 512,
    protocolFactory: TProtocolFactory = new TCompactProtocol.Factory
  )

}
