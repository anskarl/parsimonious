package com.github.anskarl.parsimonious

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.thrift.TSerializer
import org.apache.thrift.protocol.TCompactProtocol

package object json {

  private[json] final val keyName = "key"
  private[json] final val valName = "value"


  private[json] final val DefaultTCompactProtocolSerializer = new TSerializer(new TCompactProtocol.Factory())

  private[json] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

}
