package com.codebrig.jnomad.plugin.intellij.inspection;

import com.codebrig.jnomad.JNomad;
import com.codebrig.jnomad.model.FileFullReport;
import com.codebrig.jnomad.model.SourceCodeExtract;
import com.codebrig.jnomad.task.explain.DatabaseDataType;
import com.codebrig.jnomad.task.parse.QueryEntityAliasMap;

import java.io.File;
import java.sql.Connection;
import java.util.List;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class EnvFileFullReport extends FileFullReport {

    private JNomadPluginConfiguration.DBEnvironment environment;

    EnvFileFullReport(File file, JNomad jNomad, DatabaseDataType databaseDataType, QueryEntityAliasMap aliasMap, List<SourceCodeExtract> scannedFileList, Connection... dbConnections) {
        super(file, jNomad, databaseDataType, aliasMap, scannedFileList, dbConnections);
    }

    JNomadPluginConfiguration.DBEnvironment getEnvironment() {
        return environment;
    }

    void setEnvironment(JNomadPluginConfiguration.DBEnvironment environment) {
        this.environment = environment;
    }

}
