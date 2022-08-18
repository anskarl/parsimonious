package com.github.anskarl.parsimonious.json

import com.fasterxml.jackson.databind.JsonNode
import com.github.anskarl.parsimonious.TBaseType
import com.github.anskarl.parsimonious.common.ParsimoniousConfig
import org.apache.thrift.TBase

object Converters {

  implicit class JsonStringThrift(val src: String) extends AnyVal {

    def as[T <: TBaseType](tBaseClass: Class[T])
      (implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T = {
      val jsonNode = mapper.readTree(src)
      JsonThriftConverter.convert(tBaseClass, jsonNode)
    }

  }

  implicit class JsonNodeThrift(val jsonNode: JsonNode) extends AnyVal {

    def as[T <: TBaseType](tBaseClass: Class[T])
      (implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T = {
      JsonThriftConverter.convert(tBaseClass, jsonNode)
    }

  }

  implicit class ThriftJsonString[T <: TBase[_, _]](val instance: T) extends AnyVal {

    def toJsonString(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): String =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]]).toString

  }


  implicit class ThriftJsonNode[T <: TBase[_, _]](val instance: T) extends AnyVal {

    def toJsonNode(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): JsonNode =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]]).jsonNode

  }

}
