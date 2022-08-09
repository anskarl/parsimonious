package com.github.anskarl

import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.{TBase, TDeserializer, TFieldIdEnum, TSerializer}

package object parsimonious {

  type TBaseType = TBase[_ <: TBase[_, _], _ <: TFieldIdEnum]

  type ClassTBaseType = Class[_ <: TBaseType]

  case class ThriftConfig(
    keyName: String = "key",
    valName: String = "value",
    protocolSerializer: TSerializer = new TSerializer(new TCompactProtocol.Factory()),
    protocolDeserializer: TDeserializer = new TDeserializer(new TCompactProtocol.Factory())
  )
}
