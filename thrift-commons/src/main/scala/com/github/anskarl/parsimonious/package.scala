package com.github.anskarl

import org.apache.thrift.{TBase, TFieldIdEnum}

package object parsimonious {

  type TBaseType = TBase[_ <: TBase[_, _], _ <: TFieldIdEnum]

  type ClassTBaseType = Class[_ <: TBaseType]

}
