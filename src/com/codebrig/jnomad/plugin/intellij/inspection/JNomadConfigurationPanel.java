package com.codebrig.jnomad.plugin.intellij.inspection;

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

    JNomadConfigurationPanel() {
        initComponents ();
        testConnectionButton.addActionListener(this::testConnectionButton);

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
            String host = propertiesComponent.getValue("jnomad.database.host");
            String database = propertiesComponent.getValue("jnomad.database.db");
            String username = propertiesComponent.getValue("jnomad.database.username");
            String password = propertiesComponent.getValue("jnomad.database.password");
            String slowQueryThreshold = propertiesComponent.getValue("jnomad.slow_query.threshold");
            String recommendIndexThreshold = propertiesComponent.getValue("jnomad.recommend_index.threshold");

            if (host != null && database != null && username != null && password != null) {
                if (host.contains(":")) {
                    String[] hostParts = host.split(":");
                    databaseHostTextField.setText(hostParts[0]);
                    databasePortTextField.setText(hostParts[1]);
                } else {
                    databaseHostTextField.setText(host);
                    databasePortTextField.setText("5432");
                }
                databaseDBTextField.setText(database);
                databaseUsernameTextField.setText(username);
                databasePasswordTextField.setText(password);
            }
            if (slowQueryThreshold != null && recommendIndexThreshold != null) {
                slowQuerySpinner.setValue(Integer.valueOf(slowQueryThreshold));
                recommendIndexSpinner.setValue(Integer.valueOf(recommendIndexThreshold));
            }
        }
    }

    private void testConnectionButton(ActionEvent evt) {
        try {
            String[] hostParts = getDatabaseHost().split(";");
            String[] databaseParts = getDatabaseDB().split(";");
            String[] usernameParts = getDatabaseUsername().split(";");
            String[] passwordParts = getDatabasePassword().split(";");
            for (int i = 0; i < hostParts.length; i++) {
                Class.forName("org.postgresql.Driver");
                String connUrl = "jdbc:postgresql://" + hostParts[i] + ":" + getDatabasePort() + "/" +  databaseParts[i];
                Connection connection = DriverManager.getConnection(connUrl, usernameParts[i], passwordParts[i]);
                connection.close();
                System.out.println("Valid connection settings for database: " + databaseParts[i] + " (Host: " + hostParts[i] + ")");
            }
            Messages.showInfoMessage("Valid connection established!", "Valid Database Connection");
        } catch (Exception ex) {
            Messages.showErrorDialog("Invalid connection!\nReason: " + ex.getMessage(), "Invalid Database Connection");
        }
    }

    String getDatabaseHost() {
        return databaseHostTextField.getText();
    }

    String getDatabasePort() {
        return databasePortTextField.getText();
    }

    String getDatabaseDB() {
        return databaseDBTextField.getText();
    }

    String getDatabaseUsername() {
        return databaseUsernameTextField.getText();
    }

    String getDatabasePassword() {
        return new String(databasePasswordTextField.getPassword());
    }

    int getSlowQueryThreshold() {
        return (int) slowQuerySpinner.getValue();
    }

    int getRecommendIndexThreshold() {
        return (int) recommendIndexSpinner.getValue();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        slowQueryLabel = new javax.swing.JLabel();
        recommendIndexLabel = new javax.swing.JLabel();
        slowQuerySpinner = new javax.swing.JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
        recommendIndexSpinner = new javax.swing.JSpinner(new SpinnerNumberModel(50, 0, Integer.MAX_VALUE, 1));
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("General Settings"));

        slowQueryLabel.setText("Slow Query Threshold:");

        recommendIndexLabel.setText("Recommend Index Threshold:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(slowQueryLabel)
                                                        .addComponent(recommendIndexLabel))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(slowQuerySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(recommendIndexSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(slowQueryLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(slowQuerySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(recommendIndexSpinner)
                                        .addComponent(recommendIndexLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        databaseSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Database Settings"));

        testConnectionButton.setText("Test Connection");
        testConnectionButton.setFocusable(false);

        databaseHostLabel.setText("Host:");

        databasePortLabel.setText("Port:");

        databaseUsernameLabel.setText("Username:");

        databasePasswordLabel.setText("Password:");

        databaseDBLabel.setText("Database:");

        javax.swing.GroupLayout databaseSettingsPanelLayout = new javax.swing.GroupLayout(databaseSettingsPanel);
        databaseSettingsPanel.setLayout(databaseSettingsPanelLayout);
        databaseSettingsPanelLayout.setHorizontalGroup(
                databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, databaseSettingsPanelLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(testConnectionButton))
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
                                                                .addComponent(databaseHostTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(databasePortLabel)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(databasePortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                                .addContainerGap())
        );
        databaseSettingsPanelLayout.setVerticalGroup(
                databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(databaseSettingsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(databaseHostTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(databasePortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(databasePortLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(databaseHostLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(databaseDBTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                                        .addComponent(databaseDBLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(databaseUsernameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                                        .addComponent(databaseUsernameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(databaseSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(databasePasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(databasePasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(testConnectionButton)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(databaseSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(databaseSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>

    // Variables declaration - do not modify
    private javax.swing.JLabel databaseDBLabel;
    private javax.swing.JTextField databaseDBTextField;
    private javax.swing.JLabel databaseHostLabel;
    private javax.swing.JTextField databaseHostTextField;
    private javax.swing.JLabel databasePasswordLabel;
    private javax.swing.JPasswordField databasePasswordTextField;
    private javax.swing.JLabel databasePortLabel;
    private javax.swing.JTextField databasePortTextField;
    private javax.swing.JPanel databaseSettingsPanel;
    private javax.swing.JLabel databaseUsernameLabel;
    private javax.swing.JTextField databaseUsernameTextField;
    private javax.swing.JLabel slowQueryLabel;
    private javax.swing.JLabel recommendIndexLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSpinner slowQuerySpinner;
    private javax.swing.JSpinner recommendIndexSpinner;
    private javax.swing.JButton testConnectionButton;
    // End of variables declaration

}
