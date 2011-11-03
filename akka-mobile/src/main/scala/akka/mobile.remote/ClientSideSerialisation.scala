package akka.mobile.remote

import akka.actor.ActorRef
import akka.mobile.protocol.MobileProtocol._
import java.net.InetSocketAddress

/**
 * @author roman.stoffel@gamlor.info
 * @since 03.11.11
 */

class ClientSideSerialisation(messageSender: MessageSink) extends Serialisation {
  def toAddressProtocol(actorRef: ActorRef) = {
    AddressProtocol.newBuilder
      .setType(AddressType.DEVICE_ADDRESS)
      .setDeviceAddress(DeviceAddress.newBuilder().setAppId("mock").setDeviceID("mock"))
      .build
  }

  def deSerializeActorRef(refInfo: RemoteActorRefProtocol): ActorRef = {
    val remoteActorId = refInfo.getClassOrServiceName
    val homeAddress = refInfo.getNodeAddress
    homeAddress.getType match {
      case AddressType.SERVICE_ADDRESS => {
        ClientRemoteActorRef(
          deserializeServerId(homeAddress.getServiceAddress),
          remoteActorId, messageSender);
      }
      case AddressType.DEVICE_ADDRESS
      => throw new IllegalArgumentException("Cannot send answers back to other mobile devices")
      case ue => throw new Error("Unexpected type " + ue)
    }
  }

  def deserializeServerId(serverAddress: ServiceAddress)
  = new InetSocketAddress(serverAddress.getHostname, serverAddress.getPort)
}