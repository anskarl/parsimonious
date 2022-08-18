package com.github.anskarl.parsimonious.flink

import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TJSONProtocol, TProtocolFactory}

sealed trait TProtocolFactoryType {
  def create(): TProtocolFactory
}

case object TCompactProtocolFactoryType extends TProtocolFactoryType {
  override def create(): TProtocolFactory = new TCompactProtocol.Factory()
}

case object TBinaryProtocolFactoryType extends TProtocolFactoryType{
  override def create(): TProtocolFactory = new TBinaryProtocol.Factory()
}

case object TJSONProtocolFactoryType extends TProtocolFactoryType{
  override def create(): TProtocolFactory = new TJSONProtocol.Factory()
}
