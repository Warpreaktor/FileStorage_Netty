package server;

import java.sql.*;

public class AuthService {
    //Переменная хранит подключение к базе
    private static Connection connection;
    //В переменной хранится состояние запроса к базе. По сути это SELECT
    private static Statement statement;

    public static void connect(){
        try {
            //Инициализация драйвера JDBC
            Class.forName("org.sqlite.JDBC");
            //Устанавливаем соединение с базой
            connection = DriverManager.getConnection("jdbc:sqlite:filestorage-server/src/main/java/sql/database.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Запрос возвращает null если пара аккаунт пароль не найдена и возвращает польщователя БД если найдено полное совпадание.
     * @return String
     */
    public static String authentication(String account, String password){
        //Направляем запрос в бд
        String sqlRequest = String.format("SELECT root_directory FROM users WHERE account = '%s' AND password = '%s'", account, password);
        try {
            //Получаем результат запроса и сохраняем его
            ResultSet resultSet = statement.executeQuery(sqlRequest);
            if(resultSet.next()){
                return resultSet.getString(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void addUser(String account, String pass) throws SQLException{
        String acc = getAccount(account, pass);
        if (acc == null){
            try {
                String sqlRequest = String.format("INSERT INTO users (account, password) VALUES ('%s', '%s');", account, pass);
                PreparedStatement ps = connection.prepareStatement(sqlRequest);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (acc != null){
            if (acc.equals(account)){
                throw new SQLException("Аккаунт уже зарегистрирован");
            }
        }
    }

    public static String getAccount(String account, String pass) {
        try {
            String sqlRequest = String.format("SELECT " +
                    "account, password " +
                    "FROM users " +
                    "WHERE login = '%s' AND password = '%s' ;", account, pass);
            ResultSet resultSet = statement.executeQuery(sqlRequest);
            int myHash = pass.hashCode();
            if (resultSet.next()) {
                String nick = resultSet.getString(1);
                //int dbHash = rs.getInt(2);
               // if (myHash == dbHash) {
                return nick;
                //}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
