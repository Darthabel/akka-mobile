package akka.mobile.remote

import java.net.{Socket, InetSocketAddress}
import java.io.{OutputStream, InputStream}
import akka.mobile.protocol.MobileProtocol.{AkkaMobileProtocol, MobileMessageProtocol}

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

class RemoteMessaging(socketFactory: InetSocketAddress => SocketRepresentation) {
  private val messangers = scala.collection.mutable.Map[InetSocketAddress, RemoteMessageChannel]()

  def channelFor(address: InetSocketAddress): RemoteMessageChannel = {
    messangers.synchronized {
      messangers.getOrElseUpdate(address, RemoteMessageChannel(address, socketFactory))
    }
  }

}

object RemoteMessaging {
  val DEFAULT_TCP_SOCKET_FACTOR = (address: InetSocketAddress) => new TCPSocket(address)

  def apply() = new RemoteMessaging(DEFAULT_TCP_SOCKET_FACTOR)

  def apply(socketFactory: InetSocketAddress => SocketRepresentation) = new RemoteMessaging(socketFactory)
}


case class RemoteMessageChannel(address: InetSocketAddress, socketFactory: InetSocketAddress => SocketRepresentation) {
  val socket = socketFactory(address)

  def send(msg: MobileMessageProtocol) {
    Serialisation.toWireProtocol(msg).writeDelimitedTo(socket.out)
    socket.out.flush()
  }

  def receive(): MobileMessageProtocol = {
    AkkaMobileProtocol.parseDelimitedFrom(socket.in).getMessage();
  }

}

trait SocketRepresentation {

  def in: InputStream

  def out: OutputStream

  def close()
}

class TCPSocket(addr: InetSocketAddress) extends SocketRepresentation {
  val socket = {
    val s = new Socket()
    s.setKeepAlive(true)
    s.setTcpNoDelay(true)
    s.setSoTimeout(1000)
    s.connect(addr)
    s
  }

  val out = socket.getOutputStream
  val in = socket.getInputStream

  def close() = socket.close()
}

