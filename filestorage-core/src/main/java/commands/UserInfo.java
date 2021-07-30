package commands;

import lombok.Getter;

@Getter
public class UserInfo extends AbstractCommand{

    String info;

    public UserInfo(String info) {
        this.info = info;
    }

    @Override
    public CommandType getType() {
        return CommandType.USER_INFO;
    }
}
