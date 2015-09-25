package com.example

import akka.actor.{Actor, Props, ActorRef}
import akka.event.LoggingReceive


import scala.concurrent.duration.FiniteDuration

class SellerToolsAggregatorActor(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) extends Actor {

  import context.become

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
      become(waitForResponse(originalSender))

  }

  private var storePreferences: Option[StoreType] = None
  private var fileExchangeSubscription: Option[SubscriptionStatus] = None

  def waitForResponse(sender: ActorRef): Receive = {
    case sp: Dependency1Response =>
      storePreferences = Some(sp.s)
      checkForAllData(sender, false)
    case fes: Dependency2Response =>
      fileExchangeSubscription = Some(fes.f)
      checkForAllData(sender, false)
    case TimeOut =>
      checkForAllData(sender, true)
    case x =>
      throw new RuntimeException( s"""received "${x }" unexpectedly""")
  }

  def checkForAllData(sender: ActorRef) = {}

  def checkForAllData(sender: ActorRef, timedOut: Boolean): Unit = {
    val (store, file, timeout) = (storePreferences, fileExchangeSubscription, timedOut)
    (store, file, timeout) match {
      case (Some(sp), Some(fes), false) =>
        sender ! SellerToolsAggregatedResponse(sp, fes)
        timeoutMessager.cancel()
        context.stop(self)
      case (Some(sp), None, true) =>
        sender ! SellerToolsAggregatedResponse(sp, IndeterminateSubscriptionStatus)
        timeoutMessager.cancel()
        context.stop(self)
      case (_, _, true) =>
        sender ! TimeOut
        timeoutMessager.cancel()
        context.stop(self)
      case (_, _, false) =>
        println("waiting...")
    }
  }


  import context.dispatcher

  val timeoutMessager = context.system.scheduler.scheduleOnce(timeout, context.self, TimeOut)
}

object SellerToolsAggregatorActor {
  def props(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) =
    Props(classOf[SellerToolsAggregatorActor], dependency1, dependency2, timeout)
}
