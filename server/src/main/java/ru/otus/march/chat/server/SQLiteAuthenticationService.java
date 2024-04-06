package ru.otus.march.chat.server;

import java.sql.*;

public class SQLiteAuthenticationService implements AuthenticationService {
    private static final String NICKNAME_BY_LOGIN_AND_PASSWORD =
            "select max(u.nickname) nickname from users u " +
                    " where u.login = ? and u.password = ?;";

    private static final String ROLE_BY_LOGIN_AND_PASSWORD =
            "select coalesce(max(r.name),\"\") role from user_to_role ur" +
                    " inner join users u on ur.user_id = u.id" +
                    " inner join roles r on ur.role_id = r.id" +
                    " where u.login = ? and u.password = ?;";

    private static final String IS_NICKNAME_ALREADY_EXISTS =
            "select count(u.id) cnt from users u" +
                    " where u.nickname = ?;";

    private static final String IS_LOGIN_ALREADY_EXISTS =
            "select count(u.id) cnt from users u" +
                    " where u.login = ?;";

    private static final String INSERT_INTO_USERS =
            "insert into users(login, password, nickname) values(?, ?, ?);";

    private static final String DELETE_FROM_USERS =
            "delete from users where nickname = ?;";
    private static final String DATABASE_URL = "jdbc:sqlite:chat.db";
    private static Connection connection;

     public SQLiteAuthenticationService() {
        try {
            connect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try (
                PreparedStatement ps = connection.prepareStatement(NICKNAME_BY_LOGIN_AND_PASSWORD)
        ) {
            ps.setString(1, login);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Role getRoleByLoginAndPassword(String login, String password) {
        try (
                PreparedStatement ps = connection.prepareStatement(ROLE_BY_LOGIN_AND_PASSWORD)
        ) {
            ps.setString(1, login);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                String role = rs.getString("role");
                return role.equals(Role.ADMIN.getName()) ? Role.ADMIN : Role.USER;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        try (
                PreparedStatement ps = connection.prepareStatement(IS_LOGIN_ALREADY_EXISTS)
        ) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                int cnt = rs.getInt("cnt");
                return cnt != 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isNicknameAlreadyExist(String nickname) {
        try (
                PreparedStatement ps = connection.prepareStatement(IS_NICKNAME_ALREADY_EXISTS)
        ) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                int cnt = rs.getInt("cnt");
                return cnt != 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnect() throws SQLException {
        connection.close();
    }

    private static void prStmtInsertIntoUsers(String login, String password, String nickname) {
        try (
                PreparedStatement ps = connection.prepareStatement(INSERT_INTO_USERS)
        ) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nickname);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean register(String login, String password, String nickname) {
        if (isLoginAlreadyExist(login)) {
            return false;
        }
        if (isNicknameAlreadyExist(nickname)) {
            return false;
        }
        prStmtInsertIntoUsers(login, password, nickname);
        return true;
    }

    @Override
    public boolean unRegister(String nickname) {
        try (PreparedStatement ps = connection.prepareStatement(DELETE_FROM_USERS)) {
            ps.setString(1, nickname);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
