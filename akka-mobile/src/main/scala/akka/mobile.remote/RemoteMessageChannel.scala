package akka.mobile.remote

import java.net.{Socket, InetSocketAddress}
import java.io.{OutputStream, InputStream}
import akka.mobile.protocol.MobileProtocol.AkkaMobileProtocol

/**
 * @author roman.stoffel@gamlor.info
 * @since 14.10.11
 */

/**
 * A simple sink/source of messages.
 *
 */
class RemoteMessageChannel(socket: SocketRepresentation) {

  def send(msg: AkkaMobileProtocol) {
    msg.writeDelimitedTo(socket.out)
    socket.out.flush()
  }

  def receive(): AkkaMobileProtocol = {
    AkkaMobileProtocol.parseDelimitedFrom(socket.in);
  }

  def close() {
    socket.close()
  }

}

/**
 * For testing purpose it is handy to have an abstraction of a regular
 * socket.
 */
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
    s.connect(addr, 5000)
    s
  }

  val out = socket.getOutputStream
  val in = socket.getInputStream

  def close() = socket.close()
}

