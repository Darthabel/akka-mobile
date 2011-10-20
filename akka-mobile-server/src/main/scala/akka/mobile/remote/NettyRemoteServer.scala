package akka.mobile.remote

import java.lang.IllegalStateException
import org.jboss.netty.bootstrap.ServerBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.group.ChannelGroup
import akka.remote.netty.DefaultDisposableChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.codec.frame.{LengthFieldPrepender, LengthFieldBasedFrameDecoder}
import org.jboss.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufDecoder}
import org.jboss.netty.handler.execution.{OrderedMemoryAwareThreadPoolExecutor, ExecutionHandler}
import java.util.concurrent.{TimeUnit, ThreadFactory, Executors}
import org.jboss.netty.channel._
import akka.mobile.protocol.MobileProtocol.{MobileMessageProtocol, AkkaMobileProtocol}
import akka.mobile.protocol.MobileProtocol.ActorType._
import akka.actor.{IllegalActorStateException, ActorRef}
import akka.remote.{MessageSerializer, RemoteServerSettings}

/**
 * @author roman.stoffel@gamlor.info
 * @since 20.10.11
 */
object NettyRemoteServer {
  def start(hostName: String, portNumber: Int): MobileRemoteServer
  = new NettyRemoteServer(hostName, portNumber)


  class NettyRemoteServer(hostName: String, portNumber: Int) extends MobileRemoteServer {
    private val actorRegistry = new ActorRegistry();
    @volatile var isAlive = true
    val name = "NettyRemoteServer@" + hostName + ":" + portNumber

    private val threadPool = newNamedPool(name)
    private val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(threadPool, threadPool))
    private val openChannels: ChannelGroup = new DefaultDisposableChannelGroup("akka--mobile-server")
    bootstrap.setPipelineFactory(new MobileServerPipelineFactory(openChannels,actorRegistry))
    bootstrap.setOption("backlog", RemoteServerSettings.BACKLOG)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)


    openChannels.add(bootstrap.bind(new InetSocketAddress(hostName, portNumber)))


    def register(idOfActor: String, actorRef: ActorRef) {
      checkAlive()
      actorRegistry.registerActor(idOfActor,actorRef)
    }

    def shutdownServerModule() {
      isAlive = false;
      openChannels.close()
      bootstrap.releaseExternalResources()
    }


    private def checkAlive() {
      if (!isAlive) {
        throw new IllegalStateException("Server was shut down. Start it with NettyRemoteServer.start")
      }
    }

    private def newNamedPool(name: String) =
      Executors.newCachedThreadPool(new ThreadFactory {
        def newThread(r: Runnable) = {
          val thread = Executors.defaultThreadFactory().newThread(r);
          thread.setName(thread.getName + " for " + name)
          thread.setDaemon(true)
          thread
        }
      })

  }

  class MobileServerPipelineFactory(channels: ChannelGroup, actorRegistry :ActorRegistry) extends ChannelPipelineFactory {
    def getPipeline = {
      val lenDec = new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4)
      val lenPrep = new LengthFieldPrepender(4)
      val protobufDec = new ProtobufDecoder(AkkaMobileProtocol.getDefaultInstance)
      val protobufEnc = new ProtobufEncoder


      val executor = new ExecutionHandler(
        new OrderedMemoryAwareThreadPoolExecutor(
          16,
          0,
          0,
          60, TimeUnit.SECONDS));

      val serverHandler = new RemoteServerHandler(channels,actorRegistry)
      val stages: List[ChannelHandler]
      = lenDec :: protobufDec :: lenPrep :: protobufEnc :: executor :: serverHandler :: Nil
      new StaticChannelPipeline(stages: _*)
    }
  }

  @ChannelHandler.Sharable
  class RemoteServerHandler(channels: ChannelGroup, actorRegistry :ActorRegistry) extends SimpleChannelUpstreamHandler {


    override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {
      event.getMessage match {
        case remoteProtocol: AkkaMobileProtocol if remoteProtocol.hasMessage => {
          dispatchMessage(remoteProtocol.getMessage)
        }
        case _ => {
          throw new Error("Not implemented")
        }
      }
    }

    private def dispatchMessage(message: MobileMessageProtocol) {
      message.getActorInfo.getActorType match {
        case SCALA_ACTOR ⇒ dispatchToActor(message)
        case TYPED_ACTOR ⇒ throw new IllegalActorStateException("ActorType TYPED_ACTOR is currently not supported")
        case JAVA_ACTOR ⇒ throw new IllegalActorStateException("ActorType JAVA_ACTOR is currently not supported")
        case other ⇒ throw new IllegalActorStateException("Unknown ActorType [" + other + "]")
      }
    }

    private def dispatchToActor(message: MobileMessageProtocol) {
      val actorInfo = message.getActorInfo
      val actor = actorRegistry.findActorById(actorInfo.getId)


      val msgForActor = Serialisation.deSerializeMsg(message.getMessage);

      if(message.getOneWay){
        actor.postMessageToMailbox(msgForActor,None)
      } else{
        throw new Error("Not yet implemented")
      }
    }

  }

}