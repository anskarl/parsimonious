package com.github.anskarl.parsimonious.scrooge.flink


import com.twitter.scrooge.ThriftStruct
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.TypeSerializer
import com.github.anskarl.parsimonious.common.{ParsimoniousConfig, TProtocolFactoryType}


case class ScroogeTypeInformation[T <: ThriftStruct](
    structClass: Class[T],
    protocolFactoryType: TProtocolFactoryType
  ) extends TypeInformation[T] {

  override def isBasicType: Boolean = false

  override def isTupleType: Boolean = false

  override def getArity: Int = 1

  override def getTotalFields: Int = 1

  override def getTypeClass: Class[T] = structClass

  override def isKeyType: Boolean = false

  override def createSerializer(config: ExecutionConfig): TypeSerializer[T] = ScroogeTypeSerializer(structClass, protocolFactoryType)

}

object ScroogeTypeInformation {
  def apply[T <: ThriftStruct](structClass: Class[T])(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): ScroogeTypeInformation[T] = {
    new ScroogeTypeInformation[T](structClass, parsimoniousConfig.protocolFactoryType)
  }
}