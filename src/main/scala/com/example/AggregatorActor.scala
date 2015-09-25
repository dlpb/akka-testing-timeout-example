package com.example

import akka.actor.{ Actor, Props, ActorRef }
import akka.event.LoggingReceive
import com.example.AggregatorActor.Data

import scala.concurrent.duration.FiniteDuration

class AggregatorActor(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) extends Actor {

  import context.become

  override def preStart: Unit = {
    println(s"**************** preStart: $self")
  }

  override def postStop: Unit = {
    println(s"**************** postStop: $self")
  }

  override def preRestart(reason: Throwable, message: Option[ Any ]): Unit = {
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

  var data = Data(None, None)

  override def receive: Receive = LoggingReceive {
    case AggregationRequested =>
      val originalSender = sender()
      dependency1 ! Dependency1Request
      dependency2 ! Dependency2Request
      become(waitForResponse(originalSender))
  }


  def waitForResponse(sender: ActorRef): Receive = {
    case d: Dependency1Response =>
      data = data.copy(d1 = Some(d.s))
      checkForAllData(sender, false)

    case d: Dependency2Response =>
      data = data.copy(d2 = Some(d.f))
      checkForAllData(sender, false)

    case TimeOut =>
      checkForAllData(sender, true)

    case x =>
      throw new RuntimeException(s"""received "${x}" unexpectedly""")
  }

  def checkForAllData(sender: ActorRef, timedOut: Boolean): Unit = {
    val done = data.productIterator.forall(_.asInstanceOf[Option[_]].isDefined)

    if (done || timedOut) {
      timeoutMessager.cancel()

      val response = AggregatedResponse(
        data.d1 getOrElse IndeterminateData1,
        data.d2 getOrElse IndeterminateData2
      )

      sender ! response
      context.stop(self)
    }
  }

  import context.dispatcher

  val timeoutMessager = context.system.scheduler.scheduleOnce(timeout, context.self, TimeOut)
}

object AggregatorActor {
  case class Data(d1: Option[ Data1 ], d2: Option[ Data2 ])

  def props(dependency1: ActorRef, dependency2: ActorRef, timeout: FiniteDuration) =
    Props(classOf[ AggregatorActor ], dependency1, dependency2, timeout)
}
