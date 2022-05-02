package com.github.anskarl.parsimonious

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule


package object scrooge {

  type ThriftStructWithProduct = Class[_ <: com.twitter.scrooge.ThriftStruct with Product]

  private[scrooge] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  object Constants {
    final val keyName = "key"
    final val valName = "value"
  }

}
