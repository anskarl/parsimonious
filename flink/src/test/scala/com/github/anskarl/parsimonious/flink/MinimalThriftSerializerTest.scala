package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.pojo.models._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.JavaConverters._
import scala.util.chaining.scalaUtilChainingOps

class MinimalThriftSerializerTest extends AnyWordSpecLike
  with Matchers with FlinkEmbeddedClusterTestHelper with SerializerTestHelpers {

  val b1 = new BasicDummy().tap{ bd =>
    bd.setReqStr("required 101")
    bd.setStr("optional 101")
    bd.setInt16(101.toShort)
    bd.setInt32(101)
    bd.setInt64(101L)
    bd.setDbl(101.101)
    bd.setByt(8.toByte)
    bd.setBl(false)
    bd.setBin("101".getBytes("UTF-8"))
    bd.setListNumbersI32(List(1,2,3).map(java.lang.Integer.valueOf).asJava)
    bd.setListNumbersDouble(List(1.1,2.2,3.3).map(java.lang.Double.valueOf).asJava)
    bd.setSetNumbersI32(Set(1,2,3).map(java.lang.Integer.valueOf).asJava)
    bd.setSetNumbersDouble(Set(1.1,2.2,3.3).map(java.lang.Double.valueOf).asJava)
    bd.setEnm(EnumDummy.MAYBE)
    bd.setListStruct(List(new PropertyValue("prop1", "val1"), new PropertyValue("prop2", "val2")).asJava)
    bd.setMapPrimitives(
      Map(
        java.lang.Integer.valueOf(1) -> java.lang.Double.valueOf(1.1),
        java.lang.Integer.valueOf(2) -> java.lang.Double.valueOf(2.2)
      ).asJava
    )
    bd.setMapStructKey(Map(
      new PropertyValue("prop1", "val1") -> java.lang.Double.valueOf(1.1),
      new PropertyValue("prop2", "val2") -> java.lang.Double.valueOf(2.2)
    ).asJava)
    bd.setMapPrimitivesStr(Map("one" -> java.lang.Double.valueOf(1.0), "two" -> java.lang.Double.valueOf(2.0)).asJava)
  }

  val b2 = b1.deepCopy().setReqStr("required 202")

  val n1 = new NestedDummy().tap{ nd =>
    nd.setReqStr("required 101")
    nd.setBasic(b1)
  }

  val n2 = new NestedDummy().tap{ nd =>
    nd.setReqStr("required 202")
    nd.setBasic(b2)
  }

  val u1 = new UnionDummy().tap{ u =>
    u.setStr("string value")
  }

  val u2 = new UnionDummy().tap{ u =>
    u.setDbl(1.1)
  }

  val ur1 = new UnionRecursiveDummy().tap{ ur =>
    ur.setBl(true)
  }

  val ur2 = new UnionRecursiveDummy().tap{ ur =>
    ur.setUr(new UnionRecursiveDummy().tap(_.setUr(new UnionRecursiveDummy().tap(_.setBl(false)))))
  }

  "Thrift serialization" should{
    "serialize simple struct" in {
      val ti: ThriftTypeInformation[BasicDummy] = ThriftTypeInformation[BasicDummy](classOf[BasicDummy])
      roundtrip(ti, b1)
      serializable(ti)
      snapshotSerializable(ti)
    }

    "serialize simple union" in {
      val ti: ThriftTypeInformation[UnionDummy] = ThriftTypeInformation[UnionDummy](classOf[UnionDummy])
      roundtrip(ti, u1)
      serializable(ti)
      snapshotSerializable(ti)
    }

    "serialize simple nested struct" in {
      val ti: ThriftTypeInformation[NestedDummy] = ThriftTypeInformation[NestedDummy](classOf[NestedDummy])
      roundtrip(ti, n1)
      serializable(ti)
      snapshotSerializable(ti)
    }

    "serialize union with recursion" in {
      val ti: ThriftTypeInformation[UnionRecursiveDummy] = ThriftTypeInformation[UnionRecursiveDummy](classOf[UnionRecursiveDummy])
      roundtrip(ti, ur1)
      serializable(ti)
      snapshotSerializable(ti)
    }
  }


  "Thrift serialization support for Flink - simple examples" should {
    "use Thrift serialization in Flink for Thrift struct" in{
      implicit val ti: ThriftTypeInformation[BasicDummy] = ThriftTypeInformation[BasicDummy](classOf[BasicDummy])

      val result = env.fromElements(b1, b2).executeAndCollect().toList
      result mustBe List(b1, b2)
    }

    "use Thrift serialization in Flink for Thrift nested struct" in {
      implicit val ti: ThriftTypeInformation[NestedDummy] = ThriftTypeInformation[NestedDummy](classOf[NestedDummy])
      val result = env.fromElements(n1, n2).executeAndCollect().toList
      result mustBe List(n1, n2)
    }

    "use Thrift serialization in Flink for Thrift union" in {
      implicit val ti: ThriftTypeInformation[UnionDummy] = ThriftTypeInformation[UnionDummy](classOf[UnionDummy])
      val result = env.fromElements(u1, u2).executeAndCollect().toList
      result mustBe List(u1, u2)
    }

    "use Thrift serialization in Flink for Thrift union with recursion" in {
      implicit val ti: ThriftTypeInformation[UnionRecursiveDummy] = ThriftTypeInformation[UnionRecursiveDummy](classOf[UnionRecursiveDummy])
      val result = env.fromElements(ur1, ur2).executeAndCollect().toList
      result mustBe List(ur1, ur2)
    }

  }

}
