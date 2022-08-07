package com.github.anskarl.parsimonious.scrooge.spark

import com.github.anskarl.parsimonious.scrooge.UnionBuilders
import com.twitter.scrooge.ThriftStruct
import org.apache.spark.sql.Row

object Converters {


//  implicit class RowThrift(val row: Row) extends AnyVal {
//    def as[T <: ThriftStruct with Product](tBaseClass: Class[T])
//      (implicit unionBuilders: UnionBuilders = UnionBuilders.create(classOf[T])): T =
//      RowScroogeConverter.convert(tBaseClass, row)
//  }
  implicit class ScroogeRow[T <: ThriftStruct with Product](val instance: T) extends AnyVal {
    def toRow: Row =
      ScroogeRowConverter.convert(instance.asInstanceOf[ThriftStruct with Product])
  }
}
