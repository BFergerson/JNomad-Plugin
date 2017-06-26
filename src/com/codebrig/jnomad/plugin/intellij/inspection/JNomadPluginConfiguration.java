package com.codebrig.jnomad.plugin.intellij.inspection;

import com.codebrig.jnomad.task.explain.DatabaseDataType;
import com.codebrig.jnomad.task.explain.adapter.DatabaseAdapterType;
import com.codebrig.jnomad.task.explain.adapter.postgres.MysqlDatabaseDataType;
import com.codebrig.jnomad.task.explain.adapter.postgres.PostgresDatabaseDataType;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class JNomadPluginConfiguration {

    static class DBConnection {

        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private DatabaseAdapterType databaseType;
        private DatabaseDataType dataType;

        public Connection toConnection() throws SQLException {
            String connUrl;
            try {
                if (getDatabaseType().equals(DatabaseAdapterType.POSTGRESQL)) {
                    Class.forName("org.postgresql.Driver");
                    connUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                } else if (getDatabaseType().equals(DatabaseAdapterType.MYSQL)) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    connUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
                } else {
                    throw new UnsupportedOperationException();
                }
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException(e);
            }
            return DriverManager.getConnection(connUrl, username, password);
        }

        public DatabaseDataType getDataType() {
            if (dataType == null) {
                if (databaseType == DatabaseAdapterType.POSTGRESQL) {
                    dataType = new PostgresDatabaseDataType();
                } else if (databaseType == DatabaseAdapterType.MYSQL) {
                    dataType = new MysqlDatabaseDataType();
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            return dataType;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public DatabaseAdapterType getDatabaseType() {
            return databaseType;
        }

        public void setDatabaseType(DatabaseAdapterType databaseType) {
            this.databaseType = databaseType;
        }

        public String toString() {
            return "Connection: " + host + ":" + port + "/" + database;
        }
    }

    static class DBEnvironment {

        private String environmentName;
        private List<DBConnection> connectionList;

        public String getEnvironmentName() {
            return environmentName;
        }

        public void setEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
        }

        public List<DBConnection> getConnectionList() {
            if (connectionList == null) {
                connectionList = new ArrayList<>();
            }
            return connectionList;
        }

        public void setConnectionList(List<DBConnection> connectionList) {
            this.connectionList = connectionList;
        }

        public String toString() {
            return "Environment: " + environmentName;
        }
    }

    private List<DBEnvironment> environmentList;
    private int slowQueryThreshold = 100;
    private int recommendIndexThreshold = 50;

    public List<DBEnvironment> getEnvironmentList() {
        if (environmentList == null) {
            environmentList = new ArrayList<>();
        }
        return environmentList;
    }

    public void setEnvironmentList(List<DBEnvironment> environmentList) {
        this.environmentList = environmentList;
    }

    public int getSlowQueryThreshold() {
        return slowQueryThreshold;
    }

    public void setSlowQueryThreshold(int slowQueryThreshold) {
        this.slowQueryThreshold = slowQueryThreshold;
    }

    public int getRecommendIndexThreshold() {
        return recommendIndexThreshold;
    }

    public void setRecommendIndexThreshold(int recommendIndexThreshold) {
        this.recommendIndexThreshold = recommendIndexThreshold;
    }

    public String toString() {
        return new Gson().toJson(this);
    }

}
