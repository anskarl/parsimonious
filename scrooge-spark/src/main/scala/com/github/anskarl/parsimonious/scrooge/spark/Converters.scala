package com.github.anskarl.parsimonious.scrooge.spark

import com.github.anskarl.parsimonious.common.ParsimoniousConfig
import com.github.anskarl.parsimonious.scrooge.{ScroogeHelpers, UnionBuilders}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec, ThriftStructFieldInfo}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

object Converters {

  implicit class RowScrooge(val row: Row) extends AnyVal {

    def as[T <: ThriftStruct with Product: ru.TypeTag](tBaseClass: Class[T])
      (implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): T =
      RowScroogeConverter.convert(tBaseClass, row)

  }

  implicit class ScroogeRow[T <: ThriftStruct with Product](val instance: T) extends AnyVal {

    def toRow(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): Row =
      ScroogeRowConverter.convert(instance.asInstanceOf[ThriftStruct with Product])

  }

  implicit class ScroogeDataFrame(val df: DataFrame) extends AnyVal {

    def toRDD[T <: ThriftStruct with Product: ClassTag: ru.TypeTag](clazz: Class[T])
      (implicit unionBuilders: UnionBuilders, parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): RDD[T] = {
      val codec: ThriftStructCodec[T] = com.twitter.scrooge.ThriftStructCodec.forStructClass(clazz)
      df.rdd.map(row => RowScroogeConverter.convertWithCodec(codec, row))
    }


  }

  implicit class ScroogeRDD[T <: ThriftStruct with Product](val rdd: RDD[T])  extends AnyVal {

    def toDF(clazz: Class[T])(implicit spark: SparkSession, ct: ClassTag[T], parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): DataFrame = {
      val codec = com.twitter.scrooge.ThriftStructCodec.forStructClass(clazz)
      val fieldInfos: Seq[ThriftStructFieldInfo] = ScroogeHelpers.getFieldInfos(codec)
      val schema = ScroogeRowConverter.extractSchema(clazz)
      val rddRow = rdd.map(instance => ScroogeRowConverter.convertWithCodec(codec, fieldInfos, instance))

      spark.createDataFrame(rddRow, schema)
    }
  }

  implicit class ScroogeSpark(val spark: SparkSession) extends AnyVal {

    def createDataFrame[T <: ThriftStruct with Product: ClassTag](
      clazz: Class[T],
      rdd: RDD[T]
    )(implicit parsimoniousConfig: ParsimoniousConfig = ParsimoniousConfig()): Unit ={
      val codec = com.twitter.scrooge.ThriftStructCodec.forStructClass(clazz)
      val fieldInfos: Seq[ThriftStructFieldInfo] = ScroogeHelpers.getFieldInfos(codec)
      val schema = ScroogeRowConverter.extractSchema(clazz)
      val rddRow = rdd.map(instance => ScroogeRowConverter.convertWithCodec(codec, fieldInfos, instance))

      spark.createDataFrame(rddRow, schema)
    }
  }
}
