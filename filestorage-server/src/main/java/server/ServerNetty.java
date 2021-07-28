package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerNetty {

    public static void main(String[] args) throws InterruptedException {
        //EventLoopGroup работают в вечном цикле и их необходимо закрывать.
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        //ServerBootstrap декоратор
        ServerBootstrap bootstrap = new ServerBootstrap();
        //bootstrap будет читать с Handler приходящие сообщения и заворачивать их в объекты.
        bootstrap.group(auth, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                new MessageHandler()
                        );
                    }
                });
        ChannelFuture future = bootstrap.bind(8189).sync();
        log.debug("Server started...");
        future.channel().closeFuture().sync();
        //Закрываем вечный цикл EventLoopGroup
        auth.shutdownGracefully();
        worker.shutdownGracefully();
    }
}
