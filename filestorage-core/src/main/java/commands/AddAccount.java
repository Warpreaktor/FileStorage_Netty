package commands;

import lombok.Getter;

@Getter
public class AddAccount extends AbstractCommand{

    String account;
    String password;

    public AddAccount(String account, String password) {
        this.account = account;
        this.password = password;
    }

    @Override
    public CommandType getType() {
        return CommandType.ADD_ACCOUNT;
    }
}
