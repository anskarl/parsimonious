package com.github.anskarl.parsimonious.flink

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.core.memory.{DataInputViewStreamWrapper, DataOutputViewStreamWrapper}
import org.scalatest.{Assertion, Suite}
import org.scalatest.matchers.must.Matchers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}

/**
  * Modified version of https://github.com/findify/flink-protobuf/blob/master/src/test/scala/io/findify/flinkpb/SerializerTest.scala
  * Licence https://github.com/findify/flink-protobuf/blob/master/LICENSE
  */
trait SerializerTestHelpers { this: Suite with Matchers =>

  def roundtrip[T](ti: TypeInformation[T], value: T): Assertion = {
    val serializer = ti.createSerializer(new ExecutionConfig())
    val buffer     = new ByteArrayOutputStream()
    serializer.serialize(value, new DataOutputViewStreamWrapper(buffer))
    val decoded = serializer.deserialize(new DataInputViewStreamWrapper(new ByteArrayInputStream(buffer.toByteArray)))
    value mustBe decoded
  }

  def serializable[T](ti: TypeInformation[T]): Unit = {
    val stream = new ObjectOutputStream(new ByteArrayOutputStream())
    val ser    = ti.createSerializer(new ExecutionConfig())
    stream.writeObject(ser)
  }

  def snapshotSerializable[T](ti: TypeInformation[T]): Assertion = {
    val buffer     = new ByteArrayOutputStream()
    val serializer = ti.createSerializer(new ExecutionConfig())
    val conf       = serializer.snapshotConfiguration()
    conf.writeSnapshot(new DataOutputViewStreamWrapper(buffer))
    conf.readSnapshot(
      1,
      new DataInputViewStreamWrapper(new ByteArrayInputStream(buffer.toByteArray)),
      this.getClass.getClassLoader
    )
    val restored = conf.restoreSerializer()
    restored mustBe serializer
  }

}