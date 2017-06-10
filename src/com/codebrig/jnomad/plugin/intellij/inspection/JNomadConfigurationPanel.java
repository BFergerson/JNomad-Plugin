package com.codebrig.jnomad.plugin.intellij.inspection;

import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class JNomadConfigurationPanel extends JPanel {

    private DefaultListModel<JNomadPluginConfiguration.DBEnvironment> environmentListModel = new DefaultListModel<>();
    private DefaultListModel<JNomadPluginConfiguration.DBConnection> databaseListModel = new DefaultListModel<>();
    private JNomadPluginConfiguration pluginConfiguration;

    JNomadConfigurationPanel() {
        initComponents();

        //button listeners
        testConnectionButton.addActionListener(this::testConnectionButton);
        addConnectionButton.addActionListener(this::addConnectionButton);
        deleteConnectionButton.addActionListener(this::deleteConnectionButton);
        addEnvironmentButton.addActionListener(this::addEnvironmentButton);
        deleteEnvironmentButton.addActionListener(this::deleteEnvironmentButton);

        //UI stuff
        environmentList.setModel(environmentListModel);
        environmentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadEnvironment();
            }
        });
        databaseList.setModel(databaseListModel);
        databaseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDatabase();
            }
        });

        //load JNomad plugin configuration
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
            String configStr = propertiesComponent.getValue("jnomad.plugin.configuration");
            if (configStr != null) {
                pluginConfiguration = new Gson().fromJson(configStr, JNomadPluginConfiguration.class);
            }
        }
        if (pluginConfiguration == null) {
            pluginConfiguration = new JNomadPluginConfiguration();
        }

        for (JNomadPluginConfiguration.DBEnvironment env : pluginConfiguration.getEnvironmentList()) {
            environmentListModel.addElement(env);
        }
        slowQuerySpinner.setValue(pluginConfiguration.getSlowQueryThreshold());
        recommendIndexSpinner.setValue(pluginConfiguration.getRecommendIndexThreshold());

        //default select first environment
        if (!environmentListModel.isEmpty()) {
            environmentList.setSelectedIndex(0);
            databaseList.clearSelection();
            databaseHostTextField.setText("");
            databasePortTextField.setText("");
            databaseDBTextField.setText("");
            databaseUsernameTextField.setText("");
            databasePasswordTextField.setText("");
        }
    }

    private void loadDatabase() {
        boolean databaseSelected = !databaseList.isSelectionEmpty();
        deleteConnectionButton.setEnabled(databaseSelected);

        if (databaseSelected) {
            JNomadPluginConfiguration.DBConnection conn = databaseList.getSelectedValue();
            databaseHostTextField.setText(conn.getHost());
            databasePortTextField.setText(Integer.toString(conn.getPort()));
            databaseDBTextField.setText(conn.getDatabase());
            databaseUsernameTextField.setText(conn.getUsername());
            databasePasswordTextField.setText(conn.getPassword());
        } else {
            databaseHostTextField.setText("");
            databasePortTextField.setText("");
            databaseDBTextField.setText("");
            databaseUsernameTextField.setText("");
            databasePasswordTextField.setText("");
        }
    }

    private void loadEnvironment() {
        boolean environmentSelected = !environmentList.isSelectionEmpty();
        deleteEnvironmentButton.setEnabled(environmentSelected);
        databaseHostTextField.setEnabled(environmentSelected);
        databasePortTextField.setEnabled(environmentSelected);
        databaseDBTextField.setEnabled(environmentSelected);
        databaseUsernameTextField.setEnabled(environmentSelected);
        databasePasswordTextField.setEnabled(environmentSelected);
        testConnectionButton.setEnabled(environmentSelected);
        addConnectionButton.setEnabled(environmentSelected);
        deleteConnectionButton.setEnabled(environmentSelected);
        databaseListModel.clear();
        databaseHostTextField.setText("");
        databasePortTextField.setText("");
        databaseDBTextField.setText("");
        databaseUsernameTextField.setText("");
        databasePasswordTextField.setText("");

        if (environmentSelected) {
            JNomadPluginConfiguration.DBEnvironment env = environmentList.getSelectedValue();
            for (int z = 0; z < env.getConnectionList().size(); z++) {
                databaseListModel.addElement(env.getConnectionList().get(z));
            }
        }
    }

    private void addEnvironmentButton(ActionEvent evt) {
        String environmentName = environmentNameTextField.getText();
        if (environmentName.isEmpty()) {
            Messages.showErrorDialog("Missing environment name!", "Invalid Environment");
            return;
        } else {
            for (JNomadPluginConfiguration.DBEnvironment env : pluginConfiguration.getEnvironmentList()) {
                if (environmentName.equalsIgnoreCase(env.getEnvironmentName())) {
                    Messages.showErrorDialog("Duplicate environment name!", "Invalid Environment");
                    return;
                }
            }
        }

        JNomadPluginConfiguration.DBEnvironment env = new JNomadPluginConfiguration.DBEnvironment();
        env.setEnvironmentName(environmentName);

        pluginConfiguration.getEnvironmentList().add(env);
        environmentListModel.addElement(env);

        //reset for another environment
        environmentNameTextField.setText("");
    }

    private void deleteEnvironmentButton(ActionEvent evt) {
        pluginConfiguration.getEnvironmentList().remove(environmentList.getSelectedValue());
        environmentListModel.removeElementAt(environmentList.getSelectedIndex());
    }

    private void addConnectionButton(ActionEvent evt) {
        if (getDatabaseHost().isEmpty()) {
            Messages.showErrorDialog("Missing database host!",
                    "Invalid Database Connection");
            return;
        } else if (getDatabaseDB().isEmpty()) {
            Messages.showErrorDialog("Missing database!",
                    "Invalid Database Connection");
            return;
        } else if (getDatabasePort() == -1) {
            Messages.showErrorDialog("Invalid database port: " + databasePortTextField.getText(),
                    "Invalid Database Connection");
            return;
        }

        //save database connection to environment settings
        JNomadPluginConfiguration.DBEnvironment env = environmentList.getSelectedValue();
        JNomadPluginConfiguration.DBConnection conn = new JNomadPluginConfiguration.DBConnection();
        conn.setHost(getDatabaseHost());
        conn.setPort(getDatabasePort());
        conn.setDatabase(getDatabaseDB());
        conn.setUsername(getDatabaseUsername());
        conn.setPassword(getDatabasePassword());
        env.getConnectionList().add(conn);
        databaseListModel.addElement(conn);

        //reset for another connection
        databaseHostTextField.setText("");
        databasePortTextField.setText("");
        databaseDBTextField.setText("");
        databaseUsernameTextField.setText("");
        databasePasswordTextField.setText("");
    }

    private void deleteConnectionButton(ActionEvent evt) {
        JNomadPluginConfiguration.DBEnvironment env = environmentList.getSelectedValue();
        env.getConnectionList().remove(databaseList.getSelectedValue());
        databaseListModel.removeElementAt(databaseList.getSelectedIndex());
    }

    private void testConnectionButton(ActionEvent evt) {
        try {
            String[] hostParts = getDatabaseHost().split(";");
            String[] databaseParts = getDatabaseDB().split(";");
            String[] usernameParts = getDatabaseUsername().split(";");
            String[] passwordParts = getDatabasePassword().split(";");
            for (int i = 0; i < hostParts.length; i++) {
                Class.forName("org.postgresql.Driver"); //todo: don't hardcode postgres
                String connUrl = "jdbc:postgresql://" + hostParts[i] + ":" + getDatabasePort() + "/" + databaseParts[i];
                Connection connection = DriverManager.getConnection(connUrl, usernameParts[i], passwordParts[i]);
                connection.close();
                System.out.println("Valid connection settings for database: " + databaseParts[i] + " (Host: " + hostParts[i] + ")");
            }
            Messages.showInfoMessage("Valid connection established!", "Valid Database Connection");
        } catch (Exception ex) {
            Messages.showErrorDialog("Invalid connection!\nReason: " + ex.getMessage(), "Invalid Database Connection");
        }
    }

    private String getDatabaseHost() {
        return databaseHostTextField.getText();
    }

    private int getDatabasePort() {
        String portStr = databasePortTextField.getText();
        int port = -1;
        try {
            port = Integer.valueOf(portStr);
        } catch (Exception ex) {
            //ignore
        }
        return port;
    }

    private String getDatabaseDB() {
        return databaseDBTextField.getText();
    }

    private String getDatabaseUsername() {
        return databaseUsernameTextField.getText();
    }

    private String getDatabasePassword() {
        return new String(databasePasswordTextField.getPassword());
    }

    int getSlowQueryThreshold() {
        return (int) slowQuerySpinner.getValue();
    }

    int getRecommendIndexThreshold() {
        return (int) recommendIndexSpinner.getValue();
    }

    /**
     * Method needed for setPreferredFocusComponent()
     */
    public JTextField getEnvironmentNameTextField() {
        return environmentNameTextField;
    }

    /**
     * Method needed for setPreferredFocusComponent()
     */
    public JTextField getDatabaseHostTextField() {
        return databaseHostTextField;
    }

    public JNomadPluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        generalSettingsPanel = new javax.swing.JPanel();
        slowQueryLabel = new javax.swing.JLabel();
        recommendIndexLabel = new javax.swing.JLabel();
        slowQuerySpinner = new javax.swing.JSpinner();
        recommendIndexSpinner = new javax.swing.JSpinner();
        databaseSettingsPanel = new javax.swing.JPanel();
        testConnectionButton = new javax.swing.JButton();
        databasePasswordTextField = new javax.swing.JPasswordField();
        databaseUsernameTextField = new javax.swing.JTextField();
        databasePortTextField = new javax.swing.JTextField();
        databaseHostTextField = new javax.swing.JTextField();
        databaseHostLabel = new javax.swing.JLabel();
        databasePortLabel = new javax.swing.JLabel();
        databaseUsernameLabel = new javax.swing.JLabel();
        databasePasswordLabel = new javax.swing.JLabel();
        databaseDBTextField = new javax.swing.JTextField();
        databaseDBLabel = new javax.swing.JLabel();
        databaseScrollPanel = new javax.swing.JScrollPane();
        databaseList = new javax.swing.JList<>();
        addConnectionButton = new javax.swing.JButton();
        deleteConnectionButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        environmentList = new javax.swing.JList<>();
        deleteEnvironmentButton = new javax.swing.JButton();
        addEnvironmentButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        environmentNameTextField = new javax.swing.JTextField();
        environmentExampleLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        generalSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("General Settings"));

        slowQueryLabel.setText("Slow Query Threshold:");

        recommendIndexLabel.setText("Recommend Index Threshold:");

        slowQuerySpinner.setModel(new javax.swing.SpinnerNumberModel(100, 0, null, 1));

        recommendIndexSpinner.setModel(new javax.swing.SpinnerNumberModel(50, 0, null, 1));

        javax.swing.GroupLayout generalSettingsPanelLayout = new javax.swing.GroupLayout(generalSettingsPanel);
        generalSettingsPanel.setLayout(generalSettingsPanelLayout);
        generalSettingsPanelLayout.setHorizontalGroup(
                generalSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(generalSettingsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(generalSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(slowQueryLabel)
                                        .addComponent(recommendIndexLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(slowQuerySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(recommendIndexSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        generalSettingsPanelLayout.setVerticalGroup(
                generalSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(generalSettingsPanelLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(generalSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(slowQueryLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(slowQuerySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(recommendIndexSpinner)
                                        .addComponent(recommendIndexLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        databaseSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Database Settings"));

        testConnectionButton.setText("Test Connection");
        testConnectionButton.setEnabled(false);
        testConnectionButton.setFocusPainted(false);

        databasePasswordTextField.setEnabled(false);

        databaseUsernameTextField.setEnabled(false);

        databasePortTextField.setEnabled(false);

        databaseHostTextField.setEnabled(false);

        databaseHostLabel.setText("Host:");

        databasePortLabel.setText("Port:");

        databaseUsernameLabel.setText("Username:");

        databasePasswordLabel.setText("Password:");

        databaseDBTextField.setEnabled(false);

        databaseDBLabel.setText("Database:");

        databaseList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        databaseScrollPanel.setViewportView(databaseList);

        addConnectionButton.setText("Add Connection");
        addConnectionButton.setEnabled(false);

        deleteConnectionButton.setText("Delete Connection");
        deleteConnectionButton.setEnabled(false);

        javax.swing.GroupLayout databaseSettingsPanelLayout = new javax.swing.GroupLayout(databaseSettingsPanel);
        databaseSettingsPanel.setLayout(databaseSettingsPanelLayout);
        databaseSettingsPanelLayout.setHorizontalGroup(
                databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(databaseHostLabel)
                                                        .addComponent(databaseUsernameLabel)
                                                        .addComponent(databasePasswordLabel)
                                                        .addComponent(databaseDBLabel))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(databaseDBTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(databasePasswordTextField)
                                                                        .addComponent(databaseUsernameTextField))
                                                                .addGap(1, 1, 1))
                                                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                                                .addComponent(databaseHostTextField)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(databasePortLabel)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(databasePortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addComponent(databaseScrollPanel, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, databaseSettingsPanelLayout.createSequentialGroup()
                                                .addComponent(testConnectionButton)
                                                .addGap(18, 18, Short.MAX_VALUE)
                                                .addComponent(addConnectionButton)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(deleteConnectionButton)))
                                .addContainerGap())
        );
        databaseSettingsPanelLayout.setVerticalGroup(
                databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                                        .addComponent(databasePortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databasePortLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databaseHostTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databaseHostLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(databaseDBTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databaseDBLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(databaseUsernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databaseUsernameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(databasePasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databasePasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(testConnectionButton)
                                        .addComponent(addConnectionButton)
                                        .addComponent(deleteConnectionButton))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(databaseScrollPanel)
                                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Environment"));

        environmentList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jScrollPane1.setViewportView(environmentList);

        deleteEnvironmentButton.setText("Delete Environment");
        deleteEnvironmentButton.setEnabled(false);
        deleteEnvironmentButton.setFocusPainted(false);

        addEnvironmentButton.setText("Add Environment");

        jLabel1.setText("Name:");

        environmentExampleLabel.setText("Ex: LOCAL, DEV, UAT, PROD");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(deleteEnvironmentButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(addEnvironmentButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(environmentNameTextField))
                                        .addComponent(jSeparator1)
                                        .addComponent(environmentExampleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(environmentNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(environmentExampleLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(addEnvironmentButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(deleteEnvironmentButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1)
                                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(databaseSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(generalSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(databaseSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(generalSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
    }// </editor-fold>

    // Variables declaration - do not modify
    private javax.swing.JButton addConnectionButton;
    private javax.swing.JButton addEnvironmentButton;
    private javax.swing.JLabel databaseDBLabel;
    private javax.swing.JTextField databaseDBTextField;
    private javax.swing.JLabel databaseHostLabel;
    private javax.swing.JTextField databaseHostTextField;
    private javax.swing.JList<JNomadPluginConfiguration.DBConnection> databaseList;
    private javax.swing.JLabel databasePasswordLabel;
    private javax.swing.JPasswordField databasePasswordTextField;
    private javax.swing.JLabel databasePortLabel;
    private javax.swing.JTextField databasePortTextField;
    private javax.swing.JScrollPane databaseScrollPanel;
    private javax.swing.JPanel databaseSettingsPanel;
    private javax.swing.JLabel databaseUsernameLabel;
    private javax.swing.JTextField databaseUsernameTextField;
    private javax.swing.JButton deleteConnectionButton;
    private javax.swing.JButton deleteEnvironmentButton;
    private javax.swing.JLabel environmentExampleLabel;
    private javax.swing.JList<JNomadPluginConfiguration.DBEnvironment> environmentList;
    private javax.swing.JTextField environmentNameTextField;
    private javax.swing.JPanel generalSettingsPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel recommendIndexLabel;
    private javax.swing.JSpinner recommendIndexSpinner;
    private javax.swing.JLabel slowQueryLabel;
    private javax.swing.JSpinner slowQuerySpinner;
    private javax.swing.JButton testConnectionButton;
    // End of variables declaration

}
