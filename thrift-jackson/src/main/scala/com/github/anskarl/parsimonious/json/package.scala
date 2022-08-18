package com.github.anskarl.parsimonious

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.thrift.TSerializer
import org.apache.thrift.protocol.TCompactProtocol

package object json {

  private[json] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

}
