package com.example

import akka.actor.{Terminated, Actor, Props, ActorSystem}
import akka.testkit.{TestProbe, TestKit}
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually

import concurrent.duration._

class TimeoutAggregatorTest(_system: ActorSystem) extends TestKit(_system) with FlatSpecLike
    with Matchers
    with Eventually{


    val timeoutDuration = 5 seconds

      "Seller tools agregator actor"  should "receive an store prefences and file exchange subscription response, and send a response" in new Context {
        // when
        sellerToolsAggregatorActor ! Dependency1Response
        sellerToolsAggregatorActor ! Dependency2Response

        //then
        expectMsgType[SellerToolsAggregatedResponse]
      }

      it should "build up a response containing orders, cancellation, and return data" in new Context {
        
        sellerToolsAggregatorActor ! Dependency1Response
        sellerToolsAggregatorActor ! Dependency2Response

        //then
        val result = expectMsgType[SellerToolsAggregatedResponse]
        result.storeType should be(Some(NextGenStoreType))
        result.fileExchangeSubscriptionStatus should be(SubscribedToFileExchange)
      }

      it should "return an IndeterminedFileExchangeSubscription when we dont get a response within the timeout limit" in  new Context {
        sellerToolsAggregatorActor ! Dependency1Response
        sellerToolsAggregatorActor ! Dependency2Response

        expectMsgPF(10 seconds) {
          case SellerToolsAggregatedResponse(Some(NextGenStoreType), IndeterminateSubscriptionStatus) =>
        }
      }

      it should "time out when we dont get all responses within 3 seconds" in  new Context {
      
        //then
        within(250 milliseconds, 120 seconds) {
          expectMsg(TimeOut)
        }
      }

      it should "stop the actor once the message has been sent" in  new Context {
        
        var terminated = false

        system.actorOf(Props(new Actor {
          context.watch(sellerToolsAggregatorActor)

          override def receive: Receive = {
            case Terminated(_) => terminated = true
          }
        }))

        

        sellerToolsAggregatorActor ! Dependency1Response
        sellerToolsAggregatorActor ! Dependency2Response

        //then
        expectMsgType[SellerToolsAggregatedResponse]
        withClue("Expecting the actor to be terminated eventually") {
          eventually { terminated should be(true) }
        }

      }

      trait Context {
        val storeProbe = TestProbe()
        val fileProbe = TestProbe()

        val sellerToolsAggregatorActor = system.actorOf(SellerToolsAggregatorActor.props(storeProbe.ref, fileProbe.ref, timeoutDuration))

        //when
        sellerToolsAggregatorActor ! SellerToolsAggregationRequested
      }
}
