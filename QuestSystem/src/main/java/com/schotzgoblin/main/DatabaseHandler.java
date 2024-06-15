package com.schotzgoblin.main;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.*;
import com.schotzgoblin.utils.Utils;
import org.bukkit.Location;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class DatabaseHandler {
    private static DatabaseHandler instance;
    private final Connection connection;


    public DatabaseHandler() {
        try {
            connection = DriverManager.getConnection(ConfigHandler.getInstance().getStringAsync("database.url").join(), "", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized DatabaseHandler getInstance() {
        if (instance == null) {
            instance = new DatabaseHandler();
        }
        return instance;
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
    public CompletableFuture<Void> saveAsync(Object entity) {
        return CompletableFuture.runAsync(() -> save(entity), Executors.newCachedThreadPool());
    }
    public void save(Object entity) {
        String tableName = camelToSnake(entity.getClass().getSimpleName());
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Field> fields = new ArrayList<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Arrays.asList(field.getType().getInterfaces()).contains(Identifiable.class)
                    || Arrays.asList(field.getType().getInterfaces()).contains(Collection.class)
                    || field.getName().equals("id")) {
                continue;
            } else {
                fields.add(field);
            }
            columns.append(camelToSnake(field.getName())).append(", ");
            values.append("?, ");
        }
        columns = new StringBuilder(columns.substring(0, columns.length() - 2));
        values = new StringBuilder(values.substring(0, values.length() - 2));
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

    public <T> CompletableFuture<T> getFromIdAsync(Class<T> clazz, int id) {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    public <T> CompletableFuture<List<T>> getAllAsync(Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
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
                return null;
            }
        });
    }

    public CompletableFuture<Quest> getQuestByNameAsync(String name) {
        String query = "SELECT * FROM Quest WHERE name = ?";
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, name);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Quest quest = new Quest();
                        quest.setId(Integer.parseInt(resultSet.getString("id")));
                        quest.setName(resultSet.getString("name"));
                        quest.setDescription(resultSet.getString("description"));
                        quest.setTimeLimit(Integer.parseInt(resultSet.getString("time_limit")));
                        var objective = getObjectiveAsync(resultSet.getInt("objective_id")).join();
                        if (objective != null) {
                            quest.setObjectiveId(objective.getId());
                            quest.setObjective(objective);
                        }
                        return quest;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private CompletableFuture<Objective> getObjectiveAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM Objective WHERE id =?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Objective obj = new Objective();
                        obj.setId(Integer.parseInt(resultSet.getString("id")));
                        obj.setObjective(resultSet.getString("objective"));
                        obj.setType(resultSet.getString("type"));
                        obj.setValue(resultSet.getString("value"));
                        obj.setCount(Integer.parseInt(resultSet.getString("count")));
                        return obj;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Void> updateAsync(Object entity) {
        return CompletableFuture.runAsync(() -> {
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
        });
    }

    public CompletableFuture<Void> deleteAsync(Object entity) {
        String tableName = entity.getClass().getSimpleName();
        String query = "DELETE FROM " + tableName + " WHERE id=?";
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, ((Identifiable) entity).getId());
                preparedStatement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addPlayerQuest(UUID uniqueId, String questName, Location location) {
        Quest quest = getQuestByNameAsync(questName).join();
        if (quest == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (getPlayerQuestByQuestIdAsync(uniqueId, quest.getId()).join().getId() != 0) {
            return CompletableFuture.completedFuture(null);
        }
        PlayerQuest playerQuest = new PlayerQuest();
        playerQuest.setPlayerUuid(uniqueId.toString());
        playerQuest.setQuestId(quest.getId());
        playerQuest.setQuestStatusId(3);
        playerQuest.setTime(0);
        playerQuest.setProgress("0");
        playerQuest.setStartLocation(Utils.convertLocationToString(location));
        return saveAsync(playerQuest);
    }

    public CompletableFuture<List<PlayerQuest>> getPlayerQuestsAsync(UUID uniqueId, String type) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerQuest> list = new ArrayList<>();
            String query = "SELECT * FROM player_quest WHERE player_uuid =?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, uniqueId.toString());
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        var playerQuest = createPlayerQuest(resultSet);
                        if (type.isEmpty() || playerQuest.getQuestStatus().getStatus().equals(type) || type.equals(com.schotzgoblin.enums.QuestStatus.NOT_STARTED.name()))
                            list.add(playerQuest);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        });
    }

    public CompletableFuture<PlayerQuest> getPlayerQuestByQuestIdAsync(UUID uniqueId, int questId) {
        String query = "SELECT * FROM player_quest WHERE player_uuid =? AND quest_id =?";
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    private PlayerQuest createPlayerQuest(ResultSet resultSet) throws SQLException {
        PlayerQuest playerQuest = new PlayerQuest();
        playerQuest.setId(resultSet.getInt("id"));
        playerQuest.setPlayerUuid(resultSet.getString("player_uuid"));
        playerQuest.setQuestId(resultSet.getInt("quest_id"));
        playerQuest.setTime(resultSet.getInt("time"));
        playerQuest.setProgress(resultSet.getString("progress"));
        playerQuest.setStartLocation(resultSet.getString("start_location"));
        playerQuest.setQuest(getQuestFromIdAsync(playerQuest.getQuestId()).join());
        playerQuest.setQuestStatusId(resultSet.getInt("quest_status_id"));
        playerQuest.setQuestStatus(getFromIdAsync(QuestStatus.class, playerQuest.getQuestStatusId()).join());
        return playerQuest;
    }

    private CompletableFuture<Quest> getQuestFromIdAsync(int questId) {
        String query = "SELECT * FROM Quest WHERE id =?";
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, questId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Quest quest = new Quest();
                        quest.setId(Integer.parseInt(resultSet.getString("id")));
                        quest.setName(resultSet.getString("name"));
                        quest.setDescription(resultSet.getString("description"));
                        quest.setTimeLimit(Integer.parseInt(resultSet.getString("time_limit")));
                        return getObjectiveAsync(resultSet.getInt("objective_id"))
                                .thenApply(objective -> {
                                    if (objective != null) {
                                        quest.setObjectiveId(objective.getId());
                                        quest.setObjective(objective);
                                    }
                                    return quest;
                                }).join();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Void> changePlayerQuestType(UUID uniqueId, String questName, String type, Location location) {
        var quest = getQuestByNameAsync(questName).join();
        var playerQuest = getPlayerQuestByQuestIdAsync(uniqueId, quest.getId()).join();
        if (type.equals((com.schotzgoblin.enums.QuestStatus.IN_PROGRESS.name()))) {
            playerQuest.setTime(0);
            playerQuest.setProgress("0");
            playerQuest.setStartLocation(Utils.convertLocationToString(location));
        }

        playerQuest.setQuestStatusId(getQuestStatusByNameAsync(type).join());
        return updateAsync(playerQuest);
    }

    private CompletableFuture<Integer> getQuestStatusByNameAsync(String type) {
        String query = "SELECT * FROM quest_status WHERE status =?";
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    public CompletableFuture<List<Quest>> getAllQuestsAsync() {
        String query = "SELECT * FROM Quest";
        List<Quest> list = new ArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Quest quest = new Quest();
                        quest.setId(Integer.parseInt(resultSet.getString("id")));
                        quest.setName(resultSet.getString("name"));
                        quest.setDescription(resultSet.getString("description"));
                        quest.setTimeLimit(Integer.parseInt(resultSet.getString("time_limit")));
                        CompletableFuture<Objective> objectiveFuture = getObjectiveAsync(resultSet.getInt("objective_id"));
                        objectiveFuture.thenAccept(objective -> {
                            if (objective != null) {
                                quest.setObjectiveId(objective.getId());
                                quest.setObjective(objective);
                            }
                            list.add(quest);
                        }).join();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        });
    }

    public CompletableFuture<List<Reward>> getQuestRewardsAsync(Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            List<Reward> rewards = new ArrayList<>();
            String query = "SELECT * FROM quest_reward WHERE quest_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, quest.getId());
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        var id = resultSet.getInt("reward_id");
                        getRewardFromResultSetAsync(rewards, id).join();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return rewards;
        });
    }

    private CompletableFuture<Void> getRewardFromResultSetAsync(List<Reward> rewards, int id) {
        String query = "SELECT * FROM reward WHERE id =?";
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Reward reward = new Reward();
                        reward.setId(resultSet.getInt("id"));
                        reward.setName(resultSet.getString("name"));
                        reward.setRewardTypeId(resultSet.getInt("reward_type_id"));
                        reward.setAmount(resultSet.getInt("amount"));
                        reward.setValue(resultSet.getString("value"));
                        reward.setRewardType(getFromIdAsync(RewardType.class, reward.getRewardTypeId()).join());
                        rewards.add(reward);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}