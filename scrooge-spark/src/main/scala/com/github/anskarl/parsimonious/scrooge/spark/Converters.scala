package com.github.anskarl.parsimonious.scrooge.spark

import com.twitter.scrooge.ThriftStruct
import org.apache.spark.sql.Row

object Converters {


  implicit class ScroogeRow[T <: ThriftStruct with Product](val instance: T) extends AnyVal {
    def toRow: Row =
      ScroogeRowConverter.convert(instance.asInstanceOf[ThriftStruct with Product])
  }
}
