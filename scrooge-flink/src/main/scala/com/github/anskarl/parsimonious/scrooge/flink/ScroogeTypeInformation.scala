package com.github.anskarl.parsimonious.scrooge.flink

import com.twitter.scrooge.ThriftStruct
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.TypeSerializer

case class ScroogeTypeInformation[T <: ThriftStruct](structClass: Class[T]) extends TypeInformation[T] {

  override def isBasicType: Boolean = false

  override def isTupleType: Boolean = false

  override def getArity: Int = 1

  override def getTotalFields: Int = 1

  override def getTypeClass: Class[T] = structClass

  override def isKeyType: Boolean = false

  override def createSerializer(config: ExecutionConfig): TypeSerializer[T] = ScroogeTypeSerializer(structClass)

}
