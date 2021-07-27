package ServerSide.Commands;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.Getter;
import lombok.ToString;

/**
 * Команда для передачи файлов.
 */
@ToString
@Getter
public class FileMessage extends AbstractCommand {

    private final String name;
    private final long size;
    private final byte[] data;

    public FileMessage(Path path) throws IOException {
        name = path.getFileName().toString();
        size = Files.size(path);
        data = Files.readAllBytes(path);
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_MESSAGE;
    }
}