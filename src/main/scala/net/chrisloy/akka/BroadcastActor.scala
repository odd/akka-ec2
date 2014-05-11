package net.chrisloy.akka

import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent._
import akka.actor.{RootActorPath, Actor}

class BroadcastActor extends Actor {

  case class Message(target: String, content: String)

  private val cluster = Cluster(context.system)
  private var members = Set.empty[Member]

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {

    case message: String =>
      println(message)

    case Message(target, content) =>
      members foreach (pathOf(_, target) ! content)

    case MemberUp(member) =>
      members += member

    case MemberRemoved(member, previousStatus) =>
      members.find(_.address == member.address) foreach (members -= _)

    case _: MemberEvent => // ignore
  }

  private def pathOf(member: Member, targetActor: String) = {
    context.actorSelection(RootActorPath(member.address) / "user" / targetActor )
  }
}