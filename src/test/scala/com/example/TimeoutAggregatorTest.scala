package com.example

import akka.actor.{ Terminated, Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestProbe, TestKit }
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually

import concurrent.duration._

class TimeoutAggregatorTest extends TestKit(ActorSystem()) with FlatSpecLike
    with Matchers
    with Eventually {

  val timeoutDuration = 5 seconds

  "The agregator actor" should "send the messages" in new Context {
     data1Probe.expectMsg(Dependency1Request)
     data2Probe.expectMsg(Dependency2Request)

  }
  it should "receive Data1 and Data2 responses, and send a response" in new Context {
    // when
    aggregator ! Dependency1Response(Data1Received)
    aggregator ! Dependency2Response(Data2OK)

    //then
    senderProbe.expectMsgType[ AggregatedResponse ]
  }

  it should "build up a response containing Data1 and Data2, and return a response" in new Context {

    aggregator ! Dependency1Response(Data1Received)
    aggregator ! Dependency2Response(Data2OK)

    //then
    senderProbe.expectMsgPF(10 seconds) {
      case AggregatedResponse(Data1Received, Data2OK) =>
    }
  }

  it should "return an IndeterminedData1 when we dont get a response within the timeout limit" in new Context {
    aggregator ! Dependency1Response(Data1Received)

    senderProbe.expectMsgPF(10 seconds) {
      case AggregatedResponse(Data1Received, IndeterminateData2) =>
    }
  }

  it should "return an IndeterminedData2 when we dont get a response within the timeout limit" in new Context {
    aggregator ! Dependency2Response(Data2OK)

    senderProbe.expectMsgPF(10 seconds) {
      case AggregatedResponse(IndeterminateData1, Data2OK) =>
    }
  }

  it should "time out when we dont get all responses within 3 seconds" in new Context {

    //then
    senderProbe.expectMsgPF(10 seconds) {
      case AggregatedResponse(IndeterminateData1, IndeterminateData2) =>
    }
  }

  it should "stop the actor once the message has been sent" in new Context {
    val watcher = TestProbe()
    watcher.watch(aggregator)

    aggregator ! Dependency1Response(Data1Received)
    aggregator ! Dependency2Response(Data2OK)

    //then
    senderProbe.expectMsgType[ AggregatedResponse ]
    watcher.expectMsgType[ Terminated ]
  }

  trait Context {
    val data1Probe = TestProbe()
    val data2Probe = TestProbe()

    val aggregator = system.actorOf(AggregatorActor.props(data1Probe.ref, data2Probe.ref, timeoutDuration))
    implicit val senderProbe = TestProbe()
    implicit val sender = senderProbe.ref

    //when
    aggregator ! AggregationRequested
  }

}
