package com.codebrig.jnomad.plugin.intellij.inspection;

import com.codebrig.jnomad.JNomad;
import com.codebrig.jnomad.JNomadCLI;
import com.codebrig.jnomad.SourceCodeTypeSolver;
import com.codebrig.jnomad.model.FileFullReport;
import com.codebrig.jnomad.model.SourceCodeExtract;
import com.codebrig.jnomad.task.explain.adapter.postgres.PostgresDatabaseDataType;
import com.codebrig.jnomad.task.extract.extractor.query.QueryLiteralExtractor;
import com.codebrig.jnomad.task.parse.QueryParser;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl;
import com.intellij.psi.PsiElementVisitor;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class JNomadInspection extends BaseJavaLocalInspectionTool {

    private transient static final PostgresDatabaseDataType databaseDataType = new PostgresDatabaseDataType();
    private final static Cache<String, FileFullReport> fileReportCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES).build();
    transient static JNomad jnomad;
    transient static QueryParser queryParser;
    transient static boolean setupStarted = false;
    transient static JNomadPluginConfiguration pluginConfiguration;

    static synchronized void resetJNomadSetup() {
        JNomadInspection.jnomad = null;
        setupStarted = false;
    }

    private static synchronized void setupJNomad() {
        if (JNomadInspection.jnomad != null || setupStarted) {
            return;
        }
        setupStarted = true;

        SourceCodeTypeSolver typeSolver = new SourceCodeTypeSolver();

        //.Java source directories
        List<String> sourceDirectoryList = new ArrayList<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            sourceDirectoryList.add(project.getBasePath());

            ModuleManager instance = ModuleManager.getInstance(project);
            for (Module module : instance.getModules()) {
                sourceDirectoryList.add(new File(module.getModuleFilePath()).getParent());
            }
        }

        List<String> tempRemoveList = new ArrayList<>();
        List<String> tempAddList = new ArrayList<>();
        for (String sourceDirectory : sourceDirectoryList) {
            File srcMainJavaDir = new File(sourceDirectory, "src/main/java");
            if (srcMainJavaDir.exists()) {
                tempRemoveList.add(sourceDirectory);
                sourceDirectory = srcMainJavaDir.getAbsolutePath();
                tempAddList.add(sourceDirectory);
            }

            typeSolver.addJavaParserTypeSolver(new File(sourceDirectory));
        }
        sourceDirectoryList.removeAll(tempRemoveList);
        sourceDirectoryList.addAll(tempAddList);

        //setup JNomad instance
        JNomad jnomad = new JNomad(typeSolver);
        jnomad.setScanDirectoryList(sourceDirectoryList);
        jnomad.setCacheScanResults(false);
        jnomad.setOffenderReportPercentage(100);

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
            System.out.println("Found environment: " + env.getEnvironmentName() + " - Connections: " + env.getConnectionList().size());

            for (JNomadPluginConfiguration.DBConnection conn : env.getConnectionList()) {
                jnomad.getDbHost().add(conn.getHost() + ":" + conn.getPort());
                jnomad.getDbDatabase().add(conn.getDatabase());
                jnomad.getDbUsername().add(conn.getUsername());
                jnomad.getDbPassword().add(conn.getPassword());
                System.out.println("Found connection settings for database: " + conn.getDatabase() + " (Host: " + conn.getHost() + ":" + conn.getPort() + ")");
            }
        }
        jnomad.setIndexPriorityThreshold(pluginConfiguration.getRecommendIndexThreshold());

        System.out.println("Scanning all files!");
        QueryLiteralExtractor.isDisabled = true;
        jnomad.scanAllFiles();
        queryParser = new QueryParser(jnomad);
        queryParser.run();
        JNomadInspection.jnomad = jnomad;
        QueryLiteralExtractor.isDisabled = false;
        System.out.println("Done scanning all files!");
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        if (JNomadInspection.jnomad == null) {
            setupJNomad();
        }

        FileFullReport fileFullReport = null;
        VirtualFile virtualFile = holder.getFile().getVirtualFile();
        if (virtualFile.getPath().endsWith("java") && virtualFile instanceof VirtualFileImpl) {
            CharSequence contents = holder.getFile().getViewProvider().getContents();
            fileFullReport = getFileFullReport(contents);
        }
        return new JNomadQueryVisitor(holder, holder.getFile().getVirtualFile(), fileFullReport);
    }

    private static synchronized FileFullReport getFileFullReport(CharSequence charSequence) {
        FileFullReport fileReport = null;
        try {
            InputStream virtualFile = IOUtils.toInputStream(charSequence, "UTF-8");
            String md5Hash = ByteSource.wrap(ByteStreams.toByteArray(virtualFile)).hash(Hashing.md5()).toString();
            fileReport = fileReportCache.getIfPresent(md5Hash);
            virtualFile.reset();

            if (fileReport == null && JNomadInspection.jnomad != null) { //no cache; load file from disk
                QueryLiteralExtractor.isDisabled = false;
                SourceCodeExtract extract = JNomadInspection.jnomad.scanSingleFile(virtualFile);
                if (extract.getQueryLiteralExtractor().getQueryFound()) {
                    List<SourceCodeExtract> scanList = Collections.singletonList(extract);
                    queryParser.run(scanList);
                    fileReport = new FileFullReport(null, JNomadInspection.jnomad, databaseDataType, queryParser.getAliasMap(), scanList);
                    fileReportCache.put(md5Hash, fileReport);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileReport;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "JNomad (Query scanner/optimizer)";
    }

    @NotNull
    @Override
    public String getGroupDisplayName() {
        return GroupNames.PERFORMANCE_GROUP_NAME;
    }

    @NotNull
    @Override
    public String getShortName() {
        return "JNomad";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "JNomad - Version " + JNomadCLI.JNOMAD_VERSION + " (Build: " + JNomadCLI.JNOMAD_BUILD_DATE + ")";
    }

    @Override
    public void cleanup(@NotNull Project project) {
        super.cleanup(project);
        if (jnomad != null) jnomad.closeCache();
    }

}