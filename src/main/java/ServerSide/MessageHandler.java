package ServerSide;

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ServerSide.Commands.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class MessageHandler extends SimpleChannelInboundHandler<AbstractCommand> {
    private Path currentPath;

    public MessageHandler() {
        currentPath = Paths.get("O:\\Tests");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //Отправляем клиенту путь к корневому каталогу
        ctx.writeAndFlush(new PathUpResponse(currentPath.toString()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand command) throws Exception {
        log.debug("received: {}", command.getType());
        switch (command.getType()) {
            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) command;
                FileMessage msg = new FileMessage(currentPath.resolve(fileRequest.getName()));
                ctx.writeAndFlush(msg);
                break;
            case FILE_MESSAGE:
                FileMessage message = (FileMessage) command;
                System.out.println("File Message = " + message);
                Files.write(currentPath.resolve(message.getName()), message.getData());
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case PATH_UP:
                if (currentPath.getParent() != null) {
                    currentPath = currentPath.getParent();
                }
                ctx.writeAndFlush(new PathUpResponse(currentPath.toString()));
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case LIST_REQUEST:
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case PATH_IN_REQUEST:
                PathInRequest request = (PathInRequest) command;
                Path newPath = currentPath.resolve(request.getDir());
                if (Files.isDirectory(newPath)) {
                    currentPath = newPath;
                    ctx.writeAndFlush(new PathUpResponse(currentPath.toString()));
                    ctx.writeAndFlush(new ListResponse(currentPath));
                }
                break;
        }
    }
}
