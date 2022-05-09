package com.github.anskarl.parsimonious

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.thrift.protocol.TCompactProtocol


package object scrooge {

  type ThriftStructWithProduct = Class[_ <: com.twitter.scrooge.ThriftStruct with Product]

  private[scrooge] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  object Constants {
    final val keyName = "key"
    final val valName = "value"

    final val DefaultSizeHint = 512

    final val DefaultProtocolFactory = new TCompactProtocol.Factory
  }

}
