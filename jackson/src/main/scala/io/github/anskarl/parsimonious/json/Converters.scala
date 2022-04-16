package io.github.anskarl.parsimonious.json

import com.fasterxml.jackson.databind.JsonNode
import io.github.anskarl.parsimonious.TBaseType
import org.apache.thrift.{TBase, TDeserializer, TSerializer}

object Converters {

  implicit class JsonStringThrift(val src: String) extends AnyVal {

    def as[T <: TBaseType](tBaseClass: Class[T]): T = {
      val jsonNode = mapper.readTree(src)
      JsonThriftConverter.convert(tBaseClass, jsonNode)
    }

    def as[T <: TBaseType](tBaseClass: Class[T], deserializer: TDeserializer): T = {
      val jsonNode = mapper.readTree(src)
      JsonThriftConverter.convert(tBaseClass, jsonNode, deserializer)
    }
  }

  implicit class JsonNodeThrift(val jsonNode: JsonNode) extends AnyVal {

    def as[T <: TBaseType](tBaseClass: Class[T]): T = {
      JsonThriftConverter.convert(tBaseClass, jsonNode)
    }

    def as[T <: TBaseType](tBaseClass: Class[T], deserializer: TDeserializer): T = {
      JsonThriftConverter.convert(tBaseClass, jsonNode, deserializer)
    }
  }

  implicit class ThriftJsonString[T <: TBase[_, _]](val instance: T) extends AnyVal {

    def toJsonString: String =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]]).toString

    def toJsonString(serializer: TSerializer): String =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]], serializer).toString
  }


  implicit class ThriftJsonNode[T <: TBase[_, _]](val instance: T) extends AnyVal {

    def toJsonNode: JsonNode =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]]).jsonNode

    def toJsonNode(serializer: TSerializer): JsonNode =
      ThriftJsonConverter.convert(instance.asInstanceOf[TBase[_,_]], serializer).jsonNode
  }

}
