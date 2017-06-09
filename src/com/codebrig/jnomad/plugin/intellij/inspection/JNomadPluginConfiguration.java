package com.codebrig.jnomad.plugin.intellij.inspection;

import com.google.gson.Gson;

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
