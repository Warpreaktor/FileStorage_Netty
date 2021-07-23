package ServerSide.Commands;

public class FileRequest extends AbstractCommand {

    private final String fileName;

    public FileRequest(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_REQUEST;
    }
}
