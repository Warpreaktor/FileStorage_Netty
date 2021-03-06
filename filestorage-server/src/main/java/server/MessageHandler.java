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
    private final String rootDir = "O:\\Tests\\";
    private Path userRootPath;
    //TODO Нужно сделать тут массив с именами файлов, так как пользователь может выделить сразу несколько файлов.
    private String currentFocus; // Информация о том, какой конкретно файл сейчас выделен у пользователя

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelActive!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand command) throws Exception {
        log.debug("received: {}", command.getType());
        switch (command.getType()) {
            case AUTHENTICATION_REQUEST:
                AuthenticationRequest authentication = (AuthenticationRequest) command;
                if (AuthService.authentication(authentication.getAccount(), authentication.getPassword()) == null){
                    ctx.writeAndFlush(new UserInfo("Account not exist"));
                    break;
                }else {
                    userRootPath = Paths.get("O:\\Tests\\" + authentication.getAccount());
                    currentFocus = userRootPath.toAbsolutePath().toString();
                    //Отправляем клиенту путь к корневому каталогу на сервере
                    ctx.writeAndFlush(new AuthenticationComplete(userRootPath.toString()));
                    break;
                }
            case ADD_ACCOUNT:
                AddAccount addAccount = (AddAccount) command;
                if (AuthService.checkAccount(addAccount.getAccount()) == null){
                    userRootPath = Path.of(rootDir + addAccount.getAccount());
                    AuthService.addAccount(addAccount.getAccount(), addAccount.getPassword(), userRootPath.toString());
                    Files.createDirectory(Path.of(rootDir + addAccount.getAccount()));
                    ctx.writeAndFlush(new UserInfo("User " + addAccount.getAccount() + " registered"));
                }else{
                    ctx.writeAndFlush(new UserInfo("Account already exist"));
                }
                break;
            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) command;
                FileMessage msg = new FileMessage(userRootPath.resolve(fileRequest.getFileName()));
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
                //TODO здесь нужно как-то урегулировать ситуацию, когда пользователь может смнеить фокусировку и тем самым изменить путь доставки пакетов.
                FilePartMessage filePart = (FilePartMessage) command;
                if (filePart.isBegin()){
                    System.out.println("FileMessage Begin. SIZE = " + filePart.getFullSize());
                    Files.write(Path.of(currentFocus).resolve(filePart.getName()), filePart.getData(), StandardOpenOption.CREATE_NEW);
                }
                if (!filePart.isBegin() && !filePart.isEnd()){
                    Files.write(Path.of(currentFocus).resolve(filePart.getName()), filePart.getData(), StandardOpenOption.APPEND);
                }
                if (filePart.isEnd()){
                    Files.write(Path.of(currentFocus).resolve(filePart.getName()), filePart.getData(), StandardOpenOption.APPEND);
                    System.out.println("Total sended - " + filePart.getFullSize() + "; Total recieved - " + Path.of(currentFocus));
                }
                break;
            case CREATE_DIRECTORY:
                CreateDirectory createDirectory = (CreateDirectory) command;
                Files.createDirectory(Path.of(createDirectory.getName()));
                break;

        }
    }
}
