package commands;

import lombok.Getter;

@Getter
public class AuthenticationRequest extends AbstractCommand{

    String account;
    String password;

    public AuthenticationRequest(String account, String password) {
        this.account = account;
        this.password = password;
    }

    @Override
    public CommandType getType() {
        return CommandType.AUTHENTICATION_REQUEST;
    }
}
