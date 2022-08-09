package com.github.anskarl.parsimonious.scrooge

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

package object json {
  private[json] final val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
}
