package ServerSide.Commands;

import lombok.Getter;

@Getter
public class FocusResponse extends AbstractCommand{
    private final String fileName;

    public FocusResponse(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public CommandType getType() {
        return CommandType.FOCUS_RESPONSE;
    }
}
