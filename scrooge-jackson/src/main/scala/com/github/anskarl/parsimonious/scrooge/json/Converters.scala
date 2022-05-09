package com.github.anskarl.parsimonious.scrooge.json

import com.fasterxml.jackson.databind.JsonNode
import com.github.anskarl.parsimonious.scrooge.{UnionBuilders, mapper}
import com.twitter.scrooge.ThriftStruct
import scala.reflect.runtime.{universe => ru}

object Converters {

  implicit class JsonStringScrooge(val src: String) extends AnyVal {

    def as[T <: ThriftStruct with Product: ru.TypeTag](structClass: Class[T], unionBuilders: UnionBuilders): T = {
      val jsonNode = mapper.readTree(src)
      implicit val ub = unionBuilders
      JsonScroogeConverter.convert(structClass, jsonNode)
    }

  }

  implicit class JsonNodeScrooge(val jsonNode: JsonNode) extends AnyVal {

    def as[T <: ThriftStruct with Product: ru.TypeTag](structClass: Class[T], unionBuilders: UnionBuilders): T = {
      implicit val ub = unionBuilders
      JsonScroogeConverter.convert(structClass, jsonNode)
    }
  }

  implicit class ScroogeJsonString[T <: ThriftStruct with Product](val instance: T) extends AnyVal {

    def toJsonString: String =
      ScroogeJsonConverter.convert(instance).toString

  }


  implicit class ScroogeJsonNode[T <: ThriftStruct with Product](val instance: T) extends AnyVal {

    def toJsonNode: JsonNode =
      ScroogeJsonConverter.convert(instance).jsonNode

  }

}
