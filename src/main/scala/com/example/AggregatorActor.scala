package com.example

import akka.actor.{Props, ActorRef}

import scala.concurrent.duration.FiniteDuration

class SellerToolsAggregatorActor(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) {
  
}

object SellerToolsAggregatorActor {
  def props(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) =
    Props(classOf[SellerToolsAggregatorActor], dependency1, dependency2, timeout)
}
