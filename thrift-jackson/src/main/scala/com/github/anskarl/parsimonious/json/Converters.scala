package com.github.anskarl.parsimonious.json

import com.fasterxml.jackson.databind.JsonNode
import com.github.anskarl.parsimonious.{TBaseType, ThriftConfig}
import org.apache.thrift.{TBase, TDeserializer, TSerializer}

object Converters {

  implicit class JsonStringThrift(val src: String) extends AnyVal {

    def as[T <: TBaseType](tBaseClass: Class[T])
      (implicit thriftConfig: ThriftConfig = ThriftConfig()): T = {
      val jsonNode = mapper.readTree(src)
      JsonThriftConverter.convert(tBaseClass, jsonNode)
    }

  }

  implicit class JsonNodeThrift(val jsonNode: JsonNode) extends AnyVal {

    def as[T <: TBaseType](tBaseClass: Class[T])
      (implicit thriftConfig: ThriftConfig = ThriftConfig()): T = {
      JsonThriftConverter.convert(tBaseClass, jsonNode)
    }

  }

  implicit class ThriftJsonString[T <: TBase[_, _]](val instance: T) extends AnyVal {

    def toJsonString(implicit thriftConfig: ThriftConfig = ThriftConfig()): String =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]]).toString

  }


  implicit class ThriftJsonNode[T <: TBase[_, _]](val instance: T) extends AnyVal {

    def toJsonNode(implicit thriftConfig: ThriftConfig = ThriftConfig()): JsonNode =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]]).jsonNode

  }

}
