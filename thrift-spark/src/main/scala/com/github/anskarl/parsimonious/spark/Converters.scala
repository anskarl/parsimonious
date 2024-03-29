package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious.common.ParsimoniousConfig
import com.github.anskarl.parsimonious.{ClassTBaseType, TBaseType}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.thrift.TBase

import scala.reflect.ClassTag

object Converters {

  implicit class RowThrift(val row: Row) extends AnyVal {
    def as[T <: TBaseType](tBaseClass: Class[T])(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T =
      RowThriftConverter.convert(tBaseClass, row)
  }

  implicit class ThriftRow[T <: TBase[_, _]](val instance: T) extends AnyVal {
    def toRow(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): Row =
      ThriftRowConverter.convert(instance.asInstanceOf[TBase[_, _]])
  }

  implicit class ThriftDataFrame(val df: DataFrame) extends AnyVal {

    def toRDD[T <: TBaseType : ClassTag](tBaseClass: Class[T])(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): RDD[T] =
      df.rdd.map(row => RowThriftConverter.convert(tBaseClass, row))
  }

  implicit class ThriftRDD[T <: TBaseType](val rdd: RDD[T]) extends AnyVal {

    def toDF(tbaseClass: ClassTBaseType)(implicit spark: SparkSession, parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): DataFrame = {

      val schema = ThriftRowConverter.extractSchema(tbaseClass)
      val rddRow = rdd.map(instance => ThriftRowConverter.convert(instance.asInstanceOf[TBase[_, _]]))

      spark.createDataFrame(rddRow, schema)
    }
  }

}
