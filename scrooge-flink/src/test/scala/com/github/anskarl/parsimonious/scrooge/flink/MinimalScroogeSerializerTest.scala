package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.flink.{FlinkEmbeddedClusterTestHelper, SerializerTestHelpers}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.github.anskarl.parsimonious.scrooge.models._

import java.nio.ByteBuffer

class MinimalScroogeSerializerTest extends AnyWordSpecLike
  with Matchers with FlinkEmbeddedClusterTestHelper with SerializerTestHelpers {

  val b1: BasicDummy = BasicDummy(
    reqStr = "required string",
    str = Option("optional string"),
    int16 = Option(16.toShort),
    int32 = Option(32),
    int64 = Option(64L),
    dbl = Option(1.1),
    byt = Option(8.toByte),
    bl = Option(true),
    bin = Option(ByteBuffer.wrap("string value encoded as binary".getBytes("UTF-8"))),
    listNumbersI32 = Option(Seq(1,2,3,4,5)),
    listNumbersDouble = Option(Seq(1.1, 2.2, 3.3)),
    setNumbersI32 = Option(Set(1,1,2,2,3,3)),
    setNumbersDouble = Option(Set(1.1,1.1,2.2,2.2,3.3,3.3)),
    enm = Option(EnumDummy.Maybe),
    listStruct = Option(Seq(PropertyValue("prop1", "val1"), PropertyValue("prop2", "val2"))),
    mapPrimitives = Option(Map(1 -> 1.1, 2 -> 2.2)),
    mapStructKey = Option(Map(PropertyValue("prop1", "val1") -> 1.1, PropertyValue("prop2", "val2") -> 2.2)),
    mapPrimitivesStr = Option(Map("a" -> 1.1, "b" -> 2.2)),
  )

  val b2 = b1.copy(reqStr = "required 202")

  val n1 = NestedDummy(
    reqStr = "required 101",
    basic = b1
  )

  val n2 = NestedDummy(
    reqStr = "required 202",
    basic = b2
  )

  val u1: UnionDummy = UnionDummy.Str("string value")

  val u2: UnionDummy = UnionDummy.Dbl(1.1)


  val ur1: UnionRecursiveDummy = UnionRecursiveDummy.Bl(true)

  val ur2: UnionRecursiveDummy = UnionRecursiveDummy.Ur(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Bl(false)))


  "Scrooge serialization" should {
    "serialize simple struct" in {
      val ti = ScroogeTypeInformation(classOf[BasicDummy])
      roundtrip(ti, b1)
      serializable(ti)
      snapshotSerializable(ti)
    }

    "serialize simple union" in {
      val ti = ScroogeTypeInformation(classOf[UnionDummy])
      roundtrip(ti, u1)
      serializable(ti)
      snapshotSerializable(ti)
    }

    "serialize simple nested struct" in {
      val ti = ScroogeTypeInformation(classOf[NestedDummy])
      roundtrip(ti, n1)
      serializable(ti)
      snapshotSerializable(ti)
    }

    "serialize union with recursion" in {
      val ti = ScroogeTypeInformation(classOf[UnionRecursiveDummy])
      roundtrip(ti, ur1)
      serializable(ti)
      snapshotSerializable(ti)
    }
  }

  "Scrooge serialization support for Flink - simple examples" should {
    "use Scrooge serialization in Flink for Thrift struct" in {
      implicit val ti = ScroogeTypeInformation(classOf[BasicDummy])

      val result = env.fromElements(b1, b2).executeAndCollect().toList
      result mustBe List(b1, b2)
    }

    "use Scrooge serialization in Flink for Thrift nested struct" in {
      implicit val ti = ScroogeTypeInformation(classOf[NestedDummy])
      val result = env.fromElements(n1, n2).executeAndCollect().toList
      result mustBe List(n1, n2)
    }

    "use Scrooge serialization in Flink for Thrift union" in {
      implicit val ti = ScroogeTypeInformation(classOf[UnionDummy])
      val result = env.fromElements(u1, u2).executeAndCollect().toList
      result mustBe List(u1, u2)
    }

    "use Scrooge serialization in Flink for Thrift union with recursion" in {
      implicit val ti = ScroogeTypeInformation(classOf[UnionRecursiveDummy])
      val result = env.fromElements(ur1, ur2).executeAndCollect().toList
      result mustBe List(ur1, ur2)
    }
  }

}
