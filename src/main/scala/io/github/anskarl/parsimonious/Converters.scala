package io.github.anskarl.parsimonious

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ DataFrame, Row, SparkSession }
import org.apache.thrift.{ TBase, TDeserializer, TSerializer }

import scala.reflect.ClassTag

object Converters {

  implicit class RowThrift(val row: Row) extends AnyVal {
    def as[T <: TBaseType](tBaseClass: Class[T]): T =
      RowThriftConverter.convert(tBaseClass, row)

    def as[T <: TBaseType](tBaseClass: Class[T], deserializer: TDeserializer): T =
      RowThriftConverter.convert(tBaseClass, row, deserializer)
  }

  implicit class ThriftRow[T <: TBase[_, _]](val instance: T) extends AnyVal {
    def toRow: Row =
      ThriftRowConverter.convert(instance.asInstanceOf[TBase[_, _]])

    def toRow(serializer: TSerializer): Row =
      ThriftRowConverter.convert(instance.asInstanceOf[TBase[_, _]], serializer)
  }

  implicit class ThriftDataFrame(val df: DataFrame) extends AnyVal {

    def toRDD[T <: TBaseType: ClassTag](tBaseClass: Class[T]): RDD[T] = df.rdd
      .map(row => RowThriftConverter.convert(tBaseClass, row))

    def toRDD[T <: TBaseType: ClassTag](tBaseClass: Class[T], deserializerBuilder: () => TDeserializer): RDD[T] = df.rdd
      .mapPartitions{ rows =>
        val deserializer = deserializerBuilder()
        rows.map(row => RowThriftConverter.convert(tBaseClass, row, deserializer))
      }
  }

  implicit class ThriftRDD[T <: TBaseType](val rdd: RDD[T]) extends AnyVal {

    def toDF(tbaseClass: ClassTBaseType)(implicit spark: SparkSession): DataFrame = {

      val schema = ThriftRowConverter.extractSchema(tbaseClass)
      val rddRow = rdd.map(instance => ThriftRowConverter.convert(instance.asInstanceOf[TBase[_, _]]))

      spark.createDataFrame(rddRow, schema)
    }

    def toDF(tbaseClass: ClassTBaseType, serializerBuilder: () => TSerializer)(implicit spark: SparkSession): DataFrame = {
      val schema = ThriftRowConverter.extractSchema(tbaseClass)

      val rddRow = rdd.mapPartitions { instances =>
        val serializer = serializerBuilder()
        instances.map(instance => ThriftRowConverter.convert(instance.asInstanceOf[TBase[_, _]], serializer))
      }

      spark.createDataFrame(rddRow, schema)
    }
  }

}
