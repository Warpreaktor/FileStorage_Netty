package commands;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Команда для передачи больших файлов частями. Один такой объект по сути, является частью файла.
 * Взведенный флаг begin означает, что это первая посылка. Взведенный флаг end означает что это последний пакет.
 */
@Getter
public class FilePartMessage extends AbstractCommand{

    private final String name;
    private final long fullSize;
    private final byte[] data;
    private final boolean begin;
    private final boolean end;

    public FilePartMessage(Path path, Boolean begin, Boolean end, byte[] data) throws IOException {
        name = path.getFileName().toString();
        fullSize = Files.size(path);
        this.data = data;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_PART;
    }
}
