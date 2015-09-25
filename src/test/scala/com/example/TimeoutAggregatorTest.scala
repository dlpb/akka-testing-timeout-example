package com.example

import akka.actor.{ Terminated, Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestProbe, TestKit }
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually

import concurrent.duration._

class TimeoutAggregatorTest extends TestKit(ActorSystem()) with FlatSpecLike
    with Matchers
    with ImplicitSender
    with Eventually {

  val timeoutDuration = 5 seconds

  "The agregator actor" should "receive Data1 and Data2 responses, and send a response" in new Context {
    // when
    aggregator ! Dependency1Response(Data1Received)
    aggregator ! Dependency2Response(Data2OK)

    //then
    expectMsgType[ AggregatedResponse ]
  }

  it should "build up a response containing Data1 and Data2, and return a response" in new Context {

    aggregator ! Dependency1Response(Data1Received)
    aggregator ! Dependency2Response(Data2OK)

    //then
    expectMsgPF(10 seconds) {
      case AggregatedResponse(Data1Received, Data2OK) =>
    }
  }

  it should "return an IndeterminedData1 when we dont get a response within the timeout limit" in new Context {
    aggregator ! Dependency1Response(Data1Received)

    expectMsgPF(10 seconds) {
      case AggregatedResponse(Data1Received, IndeterminateData2) =>
    }
  }

  it should "return an IndeterminedData2 when we dont get a response within the timeout limit" in new Context {
    aggregator ! Dependency2Response(Data2OK)

    expectMsgPF(10 seconds) {
      case AggregatedResponse(IndeterminateData1, Data2OK) =>
    }
  }

  it should "time out when we dont get all responses within 3 seconds" in new Context {

    //then
    expectMsgPF(10 seconds) {
      case AggregatedResponse(IndeterminateData1, IndeterminateData2) =>
    }
  }

  it should "stop the actor once the message has been sent" in new Context {

    var terminated = false

    val watcher = TestProbe()
    watcher.watch(aggregator)

    aggregator ! Dependency1Response(Data1Received)
    aggregator ! Dependency2Response(Data2OK)

    //then
    expectMsgType[ AggregatedResponse ]
    watcher.expectMsgType[ Terminated ]
  }

  trait Context {
    val storeProbe = TestProbe()
    val fileProbe = TestProbe()

    val aggregator = system.actorOf(AggregatorActor.props(storeProbe.ref, fileProbe.ref, timeoutDuration))

    //when
    aggregator ! AggregationRequested
  }

}
