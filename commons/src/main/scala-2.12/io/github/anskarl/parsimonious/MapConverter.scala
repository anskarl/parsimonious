package io.github.anskarl.parsimonious

import org.apache.thrift.TFieldIdEnum
import org.apache.thrift.meta_data.FieldMetaData

import scala.collection.JavaConverters


object MapConverter {

  def convert(jmap: java.util.Map[TFieldIdEnum, FieldMetaData]): scala.collection.mutable.Map[TFieldIdEnum, FieldMetaData] =
    JavaConverters.mapAsScalaMap(jmap)


}
