package com.schotzgoblin.main;

import com.schotzgoblin.database.Identifiable;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.QuestStatus;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class DatabaseHandler {
    private final Connection connection;

    public DatabaseHandler() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\Max\\Documents\\GitHub\\QuestSystemPlugin\\QuestSystem\\src\\main\\java\\com\\schotzgoblin\\database\\QuestSystemDB.sqlite", "", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String snakeToCamel(String snakeCase) {
        StringBuilder camelCase = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCase.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    camelCase.append(c);
                }
            }
        }
        return camelCase.toString();
    }

    private String camelToSnake(String camelCase) {
        StringBuilder snakeCase = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c) && !snakeCase.isEmpty()) {
                snakeCase.append('_');
                snakeCase.append(Character.toLowerCase(c));
            } else {
                snakeCase.append(c);
            }
        }
        return snakeCase.toString();
    }

    public void save(Object entity) {
        String tableName = camelToSnake(entity.getClass().getSimpleName());
        String columns = "";
        String values = "";
        List<Field> fields = new ArrayList<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Arrays.asList(field.getType().getInterfaces()).contains(Identifiable.class)
                    || Arrays.asList(field.getType().getInterfaces()).contains(Collection.class)
                    || field.getName().equals("id")) {
                continue;
            } else {
                fields.add(field);
            }
            columns += camelToSnake(field.getName()) + ", ";
            values += "?, ";
        }
        columns = columns.substring(0, columns.length() - 2);
        values = values.substring(0, values.length() - 2);
        String query = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int i = 1;
            for (Field field : fields) {
                field.setAccessible(true);
                preparedStatement.setObject(i++, field.get(entity));
            }
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> T getFromId(Class<T> clazz, int id) {
        String tableName = camelToSnake(clazz.getSimpleName());
        String query = "SELECT * FROM " + tableName + " WHERE id =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    T entity = clazz.getDeclaredConstructor().newInstance();
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String camelCaseName = snakeToCamel(columnName);
                        Field field = clazz.getDeclaredField(camelCaseName);
                        field.setAccessible(true);
                        field.set(entity, resultSet.getObject(columnName));
                    }
                    return entity;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> List<T> getAll(Class<T> clazz) {
        String tableName = camelToSnake(clazz.getSimpleName());
        String query = "SELECT * FROM " + tableName;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<T> list = new ArrayList<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    T entity = clazz.getDeclaredConstructor().newInstance();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String camelCaseName = snakeToCamel(columnName);
                        Field field = clazz.getDeclaredField(camelCaseName);
                        field.setAccessible(true);
                        field.set(entity, resultSet.getObject(columnName));
                    }
                    list.add(entity);
                }
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Quest getQuestByName(String name) {
        String query = "SELECT * FROM Quest WHERE name =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Quest quest = new Quest();
                    quest.setId(Integer.parseInt(resultSet.getString("id")));
                    quest.setName(resultSet.getString("name"));
                    quest.setDescription(resultSet.getString("description"));
                    quest.setTimeLimit(Integer.parseInt(resultSet.getString("time_limit")));
                    quest.setObjective(resultSet.getString("objective"));
                    return quest;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Object entity) {
        String tableName = camelToSnake(entity.getClass().getSimpleName());
        String columns = "";
        List<Field> fields = new ArrayList<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Arrays.asList(field.getType().getInterfaces()).contains(Identifiable.class)
                    || Arrays.asList(field.getType().getInterfaces()).contains(Collection.class)
                    || field.getName().equals("id")) {
                continue;
            } else {
                fields.add(field);
            }
            String snakeCaseName = camelToSnake(field.getName());
            columns += snakeCaseName + " =?, ";
        }
        columns = columns.substring(0, columns.length() - 2);

        String query = "UPDATE " + tableName + " SET " + columns + " WHERE id =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int i = 1;
            for (Field field : fields) {
                field.setAccessible(true);
                preparedStatement.setObject(i++, field.get(entity));
            }
            preparedStatement.setObject(i, ((Identifiable) entity).getId());
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(Object entity) {
        String tableName = entity.getClass().getSimpleName();
        String query = "DELETE FROM " + tableName + " WHERE id=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, ((Identifiable) entity).getId());
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPlayerQuest(UUID uniqueId, String questName) {

        Quest quest = getQuestByName(questName);
        if (quest == null) {
            return;
        }
        if (getPlayerQuestByQuestId(uniqueId, quest.getId()).getId() != 0) {
            return;
        }
        PlayerQuest playerQuest = new PlayerQuest();
        playerQuest.setPlayerUuid(uniqueId.toString());
        playerQuest.setQuestId(quest.getId());
        playerQuest.setQuestStatusId(3);
        save(playerQuest);
    }

    public List<PlayerQuest> getPlayerQuests(UUID uniqueId, String type) {
        List<PlayerQuest> list = new ArrayList<>();
        String query = "SELECT * FROM player_quest WHERE player_uuid =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, uniqueId.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    var playerQuest = createPlayerQuest(resultSet);
                    if (type.isEmpty() || playerQuest.getQuestStatus().getStatus().equals(type) || type.equals("NOT_STARTED"))
                        list.add(playerQuest);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public PlayerQuest getPlayerQuestByQuestId(UUID uniqueId, int questId) {
        String query = "SELECT * FROM player_quest WHERE player_uuid =? AND quest_id =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, uniqueId.toString());
            preparedStatement.setInt(2, questId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return createPlayerQuest(resultSet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PlayerQuest();
    }

    private PlayerQuest createPlayerQuest(ResultSet resultSet) throws SQLException {
        PlayerQuest playerQuest = new PlayerQuest();
        playerQuest.setId(resultSet.getInt("id"));
        playerQuest.setPlayerUuid(resultSet.getString("player_uuid"));
        playerQuest.setQuestId(resultSet.getInt("quest_id"));
        playerQuest.setTime(resultSet.getInt("time"));
        playerQuest.setProgress(resultSet.getInt("progress"));
        playerQuest.setQuest(getFromId(Quest.class, playerQuest.getQuestId()));
        playerQuest.setQuestStatusId(resultSet.getInt("quest_status_id"));
        playerQuest.setQuestStatus(getFromId(QuestStatus.class, playerQuest.getQuestStatusId()));
        return playerQuest;
    }

    public void changePlayerQuestType(UUID uniqueId, String questName, String type) {
        var quest = getQuestByName(questName);
        var playerQuest = getPlayerQuestByQuestId(uniqueId, quest.getId());
        if (type.equals(("IN_PROGRESS"))) {
            playerQuest.setTime(0);
            playerQuest.setProgress(0);
        }

        playerQuest.setQuestStatusId(getQuestStatusByName(type));
        update(playerQuest);
    }

    private int getQuestStatusByName(String type) {
        String query = "SELECT * FROM quest_status WHERE status =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, type);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}