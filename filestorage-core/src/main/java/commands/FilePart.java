package commands;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class FilePart extends AbstractCommand{

    private final String name;
    private final long size;
    private final byte[] data;
    private final boolean begin;
    private final boolean end;

    public FilePart(Path path, Boolean begin, Boolean end, byte[] data) throws IOException {
        name = path.getFileName().toString();
        size = Files.size(path);
        this.data = data;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_PART;
    }
}
