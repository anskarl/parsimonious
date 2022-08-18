package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.TBaseType
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.TypeSerializer
import com.github.anskarl.parsimonious.common.{ParsimoniousConfig, TProtocolFactoryType}

case class ThriftTypeInformation[T <: TBaseType](
    tbaseClass: Class[T],
    protocolFactoryType: TProtocolFactoryType
  ) extends TypeInformation[T] {

  override def isBasicType: Boolean = false

  override def isTupleType: Boolean = false

  override def getArity: Int = 1

  override def getTotalFields: Int = 1

  override def getTypeClass: Class[T] = tbaseClass

  override def isKeyType: Boolean = false

  override def createSerializer(config: ExecutionConfig): TypeSerializer[T] = ThriftTypeSerializer(tbaseClass, protocolFactoryType)

  override def canEqual(obj: Any): Boolean = tbaseClass.isInstance(obj)
}

object ThriftTypeInformation {

  def apply[T <: TBaseType](tbaseClass: Class[T])(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): ThriftTypeInformation[T] =
    new ThriftTypeInformation(tbaseClass, parsimoniousConfig.protocolFactoryType)
}
