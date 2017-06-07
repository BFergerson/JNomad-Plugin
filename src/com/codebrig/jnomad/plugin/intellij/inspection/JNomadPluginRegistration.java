package com.codebrig.jnomad.plugin.intellij.inspection;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

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
                Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
                JNomadConfigurationPanel configPanel = new JNomadConfigurationPanel();
                DialogBuilder builder = new DialogBuilder(project);
                builder.setTitle("JNomad Configuration");
                builder.setCenterPanel(configPanel);
                builder.setOkActionEnabled(true);

                if (DialogWrapper.OK_EXIT_CODE == builder.show()) {
                    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
                    if (configPanel.getDatabasePort() != null && !configPanel.getDatabasePort().isEmpty()) {
                        propertiesComponent.setValue("jnomad.database.host", configPanel.getDatabaseHost() + ":" + configPanel.getDatabasePort());
                    } else {
                        propertiesComponent.setValue("jnomad.database.host", configPanel.getDatabaseHost());
                    }
                    propertiesComponent.setValue("jnomad.database.db", configPanel.getDatabaseDB());
                    propertiesComponent.setValue("jnomad.database.username", configPanel.getDatabaseUsername());
                    propertiesComponent.setValue("jnomad.database.password", configPanel.getDatabasePassword());
                    propertiesComponent.setValue("jnomad.slow_query.threshold", Integer.toString(configPanel.getSlowQueryThreshold()));
                    propertiesComponent.setValue("jnomad.recommend_index.threshold", Integer.toString(configPanel.getRecommendIndexThreshold()));
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