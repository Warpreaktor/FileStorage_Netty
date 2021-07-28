package commands;

/**
 * Команда для удаления файла.
 * С клиента на сервер или на тот же клиент передается имя файла в виде полного его пути
 * и принимающая сторона обрабатывает запрос удаляя файл с указанным именем.
 */
public class DeleteRequest extends AbstractCommand{
    private final String fileName;

    public DeleteRequest(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public CommandType getType() {
        return CommandType.DELETE_REQUEST;
    }
}
