package com.example


case object Data1Request
case object Data2Request

case class AggregatedResponse(d1: Data1, d2: Data2)
case object AggregationRequested

sealed trait Data1
case object Data1Received extends Data1
case object IndeterminateData1 extends Data1

sealed trait Data2
case object Data2OK extends Data2
case object Data2Error extends Data2
case object IndeterminateData2 extends Data2

case object TimeOut

case object Dependency1Request
case object Dependency2Request
case class Dependency1Response(s: Data1)
case class Dependency2Response(f: Data2)