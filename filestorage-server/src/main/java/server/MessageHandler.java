package server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import commands.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class MessageHandler extends SimpleChannelInboundHandler<AbstractCommand> {
    private Path currentPath;
    //TODO Нужно сделать тут массив с именами файлов, так как пользователь может выделить сразу несколько файлов.
    private String currentFocus; // Информация о том, какой конкретно файл сейчас выделен у пользователя
    private final byte[] buffer;

    public MessageHandler() {
        buffer = new byte[1024];
        //TODO тут должна открываться папка авторизованного пользователя
        currentPath = Paths.get("O:\\Tests");
        currentFocus = currentPath.toAbsolutePath().toString();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //Отправляем клиенту путь к корневому каталогу на сервере
        ctx.writeAndFlush(new PathUpResponse(currentPath.toString()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand command) throws Exception {
        log.debug("received: {}", command.getType());
        switch (command.getType()) {
            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) command;
                FileMessage msg = new FileMessage(currentPath.resolve(fileRequest.getFileName()));
                ctx.writeAndFlush(msg);
                break;
            case FILE_MESSAGE:
                FileMessage fileMessage = (FileMessage) command;
                Files.write(Path.of(currentFocus).resolve(fileMessage.getName()), fileMessage.getData());
                break;
            case DELETE_REQUEST:
                if(Files.isDirectory(Path.of(currentFocus))){
                System.out.println("удаление директорий в разработке");
            }else {
                    Files.delete(Path.of(currentFocus));
                }
                break;
            case FOCUS_RESPONSE: //актуализирует фокусировку клиента на файлах
                FocusResponse focus = (FocusResponse) command;
                currentFocus = focus.getFileName();
                break;
            case FILE_PART: //передача файлов частями
                //TODO реализовать передачу файлов через буфер с возможностью передавать большие файлы.
                FilePart filePart = (FilePart) command;
                if (filePart.isBegin()){
                    Files.write(Path.of(currentFocus).resolve(filePart.getName()), filePart.getData());
                }
                if (!filePart.isBegin() && !filePart.isEnd()){
                    Files.write(Path.of(currentFocus).resolve(filePart.getName()), filePart.getData(), StandardOpenOption.APPEND);
                }
                if (filePart.isEnd()){
                    Files.write(Path.of(currentFocus).resolve(filePart.getName()), filePart.getData(), StandardOpenOption.APPEND);
                }
                break;
        }
    }
}
