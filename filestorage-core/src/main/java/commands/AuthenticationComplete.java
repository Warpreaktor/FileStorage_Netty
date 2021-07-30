package commands;

import lombok.Getter;

@Getter
public class AuthenticationComplete extends AbstractCommand {

    private final String rootPath;

    public AuthenticationComplete(String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public CommandType getType() {
        return CommandType.AUTHENTICATION_COMPLETE;
    }
}
