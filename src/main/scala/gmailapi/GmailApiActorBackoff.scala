package gmailapi

import akka.actor.{ Actor, ActorRef, Props, Stash, Cancellable }
import akka.event.Logging
import gmailapi._
import gmailapi.restclient._
import gmailapi.resources.GmailResource
import gmailapi.methods.GmailRestRequest
import akka.actor.OneForOneStrategy
import scala.util.Random
import scala.concurrent.duration._

object GmailApiActorBackoff {
  case class Retry(request: GmailRestRequest)
}

class GmailApiActorBackoff(maxRetries: Int = 5) extends Actor with Stash {
  import context.dispatcher
  import gmailapi.restclient.RestResponses._
  import GmailApiActorBackoff._

  val gmailApi = context.actorOf(Props[GmailApiActor])
  var scheduledRetry: Option[Cancellable] = None
  val log = Logging(context.system, this)

  def receive = {
    case request: GmailRestRequest ⇒
      gmailApi ! request
      val sender = context.sender()
      context.become(awaitingResponse(0, request, sender), discardOld = false)
  }

  def awaitingResponse(retry: Int, request: GmailRestRequest, sender: ActorRef): Receive = {
    case RateLimitExceeded ⇒
      if (retry >= maxRetries) {
        log.info(s"Exceeded max retries on $request")
        sender ! RateLimitExceeded
        scheduledRetry = None
        unstashAll()
        context.become(receive, discardOld = false)
      } else {
        val waitTime = 1000 * Math.pow(2, retry).toInt + Random.nextInt(1000)
        log.info(s"Will retry for ${retry+1} time $request after $waitTime ms")

        scheduledRetry = Some(context.system.scheduler.scheduleOnce(
          FiniteDuration(waitTime, MILLISECONDS),
          self,
          Retry(request)))
        context.become(awaitingResponse(retry + 1, request, sender), discardOld = false)
      }
    case Retry(request) ⇒
      gmailApi ! request
      scheduledRetry = None
    case response: RestResponse ⇒
      sender ! response
      scheduledRetry foreach { _.cancel() }
      scheduledRetry = None
      unstashAll()
      context.become(receive, discardOld = false)
    case _: GmailRestRequest ⇒ stash()
  }
}