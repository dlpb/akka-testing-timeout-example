package com.example

import akka.actor.{Actor, Props, ActorRef}
import akka.event.LoggingReceive


import scala.concurrent.duration.FiniteDuration

class SellerToolsAggregatorActor(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) extends Actor { import context.become

  override def preStart: Unit = {
    println(s"**************** preStart: $self")
  }

  override def postStop: Unit = {
    println(s"**************** postStop: $self")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println(s"**************** preRestart: $self, $reason, $message")
  }

  override def postRestart(reason: Throwable): Unit = {
    println(s"**************** postRestart: $self, $reason")
    super.postRestart(reason)
  }

  override def unhandled(msg: Any): Unit = {
    println(s"**************** unhandled: $self, $msg")
    super.unhandled(msg)
  }

  override def receive: Receive = LoggingReceive {
    case SellerToolsAggregationRequested =>
      val originalSender = sender()
      dependency1 ! Dependency1Request
      dependency2 ! Dependency2Request
      become(receivedNothing(originalSender))
  }

  case object ReplyToSender
  case object ChangeState
  def receivedNothing(sender: ActorRef): Receive = LoggingReceive {
    case r: Dependency1Response =>
      println(self)
      context.become(receivedStorePreferencesOnly(sender, r.s), true)
      self ! ChangeState
    case r: Dependency2Response =>
      context.become(receivedFileSubscriptionOnly(sender, r.f), true)
      self ! ChangeState
    case TimeOut =>
      println(self)
      context.become(timeoutWithNothing(sender), true)
      self ! ReplyToSender
  }

  def receivedStorePreferencesOnly(sender: ActorRef, s: StoreType): Receive = LoggingReceive.withLabel("receivedStorePreferences") {
    case r: Dependency2Response =>
      context.become(receivedStorePreferencesAndFileExchange(sender, s, r.f), true)
      self ! ReplyToSender
    case TimeOut =>
      context.become(timeoutWithStorePreferencesOnly(sender, s), true)
      self ! ReplyToSender
    case ChangeState =>
      println(self)

  }

  def receivedFileSubscriptionOnly(sender: ActorRef, f: SubscriptionStatus): Receive = LoggingReceive.withLabel("receivedSubscription") {
    case r: Dependency1Response =>
      context.become(receivedStorePreferencesAndFileExchange(sender, r.s, f), true)
      self ! ReplyToSender
    case TimeOut =>
      context.become(timeoutWithNothing(sender), true)
      self ! ReplyToSender
    case ChangeState =>
  }

  def receivedStorePreferencesAndFileExchange(sender: ActorRef, s: StoreType, f: SubscriptionStatus): Receive = LoggingReceive.withLabel("receivedBoth") {
    case ReplyToSender =>
      sender ! SellerToolsAggregatedResponse(s, f)
      context.stop(self)
  }

  def timeoutWithNothing(sender: ActorRef): Receive = LoggingReceive.withLabel("timeoutWithNothing") {
    case ReplyToSender =>
      sender ! TimeOut
      context.stop(self)
  }

  def timeoutWithStorePreferencesOnly(sender: ActorRef, s: StoreType): Receive = LoggingReceive.withLabel("timeoutWithStorePreferences") {
    case ReplyToSender =>
      sender ! SellerToolsAggregatedResponse(s, IndeterminateSubscriptionStatus)
      context.stop(self)
  }


  import context.dispatcher
  val timeoutMessager = context.system.scheduler.scheduleOnce(timeout, context.self, TimeOut)
}

object SellerToolsAggregatorActor {
  def props(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) =
    Props(classOf[SellerToolsAggregatorActor], dependency1, dependency2, timeout)
}
