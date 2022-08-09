package com.github.anskarl.parsimonious.scrooge.json

import com.fasterxml.jackson.databind.JsonNode
import com.github.anskarl.parsimonious.scrooge.{ScroogeConfig, UnionBuilders}
import com.twitter.scrooge.ThriftStruct

import scala.reflect.runtime.{universe => ru}

object Converters {

  implicit class JsonStringScrooge(val src: String) extends AnyVal {

    def as[T <: ThriftStruct with Product: ru.TypeTag](clazz: Class[T])
      (implicit unionBuilders: UnionBuilders, scroogeConfig: ScroogeConfig = ScroogeConfig()): T = {
      val jsonNode = mapper.readTree(src)
      JsonScroogeConverter.convert(clazz, jsonNode)
    }

  }

  implicit class JsonNodeScrooge(val jsonNode: JsonNode) extends AnyVal {

    def as[T <: ThriftStruct with Product: ru.TypeTag](clazz: Class[T])
      (implicit unionBuilders: UnionBuilders, scroogeConfig: ScroogeConfig = ScroogeConfig()): T = {
      JsonScroogeConverter.convert(clazz, jsonNode)
    }

  }

  implicit class ScroogeJsonString[T <: ThriftStruct with Product](val instance: T) extends AnyVal {

    def toJsonString(implicit scroogeConfig: ScroogeConfig = ScroogeConfig()): String =
      ScroogeJsonConverter.convert(instance).toString

  }

  implicit class ScroogeJsonNode[T <: ThriftStruct with Product](val instance: T) extends AnyVal {

    def toJsonNode(implicit scroogeConfig: ScroogeConfig = ScroogeConfig()): JsonNode =
      ScroogeJsonConverter.convert(instance).jsonNode
  }

}
