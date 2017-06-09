package com.codebrig.jnomad.plugin.intellij.inspection;

import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class JNomadPluginRegistration implements ApplicationComponent {

    @NotNull
    @Override
    public String getComponentName() {
        return "JNomadPlugin";
    }

    @Override
    public void initComponent() {
        ActionManager am = ActionManager.getInstance();
        AnAction action = new AnAction("JNomad Configuration") {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                Project project = Objects.requireNonNull(anActionEvent.getData(PlatformDataKeys.PROJECT));
                JNomadConfigurationPanel configPanel = new JNomadConfigurationPanel();
                DialogBuilder builder = new DialogBuilder(project);
                builder.setTitle("JNomad Configuration");
                builder.setCenterPanel(configPanel);
                builder.setOkActionEnabled(true);
                if (configPanel.getPluginConfiguration().getEnvironmentList().isEmpty()) {
                    builder.setPreferredFocusComponent(configPanel.getEnvironmentNameTextField());
                } else {
                    builder.setPreferredFocusComponent(configPanel.getDatabaseHostTextField());
                }

                if (DialogWrapper.OK_EXIT_CODE == builder.show()) {
                    JNomadPluginConfiguration pluginConfiguration = configPanel.getPluginConfiguration();
                    pluginConfiguration.setSlowQueryThreshold(configPanel.getSlowQueryThreshold());
                    pluginConfiguration.setRecommendIndexThreshold(configPanel.getRecommendIndexThreshold());

                    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
                    propertiesComponent.setValue("jnomad.plugin.configuration", new Gson().toJson(pluginConfiguration));
                    JNomadInspection.resetSetup();
                }
            }
        };
        am.registerAction("JNomadPluginAction", action);

        //add to analyze menu
        DefaultActionGroup windowM = (DefaultActionGroup) am.getAction("AnalyzeMenu");
        windowM.addSeparator();
        windowM.add(action);
    }

    @Override
    public void disposeComponent() {
    }

}