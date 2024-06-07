package com.schotzgoblin.main;

import com.schotzgoblin.database.Identifiable;
import com.schotzgoblin.database.Quest;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

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
            if (Character.isUpperCase(c)) {
                snakeCase.append('_');
                snakeCase.append(Character.toLowerCase(c));
            } else {
                snakeCase.append(c);
            }
        }
        return snakeCase.toString();
    }

    public void save(Object entity) {
        String tableName = entity.getClass().getSimpleName();
        System.out.println(tableName + "----------------------");
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
        String tableName = clazz.getSimpleName();
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
        String tableName = clazz.getSimpleName();
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
                    return quest;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Object entity) {
        String tableName = entity.getClass().getSimpleName();
        String columns = "";
        for (Field field : entity.getClass().getDeclaredFields()) {
            String snakeCaseName = camelToSnake(field.getName());
            columns += snakeCaseName + " =?, ";
        }
        columns = columns.substring(0, columns.length() - 2);

        String query = "UPDATE " + tableName + " SET " + columns + " WHERE id =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int i = 1;
            for (Field field : entity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                preparedStatement.setObject(i++, field.get(entity));
            }
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
}