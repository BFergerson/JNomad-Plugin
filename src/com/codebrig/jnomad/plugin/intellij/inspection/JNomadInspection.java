package com.codebrig.jnomad.plugin.intellij.inspection;

import com.codebrig.jnomad.JNomad;
import com.codebrig.jnomad.JNomadCLI;
import com.codebrig.jnomad.SourceCodeTypeSolver;
import com.codebrig.jnomad.model.FileFullReport;
import com.codebrig.jnomad.model.QueryScore;
import com.codebrig.jnomad.model.RecommendedIndex;
import com.codebrig.jnomad.model.SourceCodeExtract;
import com.codebrig.jnomad.task.explain.QueryIndexReport;
import com.codebrig.jnomad.task.explain.adapter.postgres.PostgresDatabaseDataType;
import com.codebrig.jnomad.task.extract.extractor.query.QueryLiteralExtractor;
import com.codebrig.jnomad.task.parse.QueryParser;
import com.github.javaparser.Range;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class JNomadInspection extends BaseJavaLocalInspectionTool {

    @NonNls
    private static final String CHECKED_CLASSES = "javax.persistence.Query;javax.persistence.TypedQuery;java.sql.PreparedStatement";

    private transient static final PostgresDatabaseDataType databaseDataType = new PostgresDatabaseDataType();
    private final Cache<String, FileFullReport> fileReportCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES).build();
    private transient static JNomad jnomad;
    private transient static QueryParser queryParser;
    private transient static boolean setupStarted = false;
    private transient static JNomadPluginConfiguration pluginConfiguration;

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

        final String scanFileLocation = holder.getFile().getVirtualFile().getPath();
        final File scanFile = new File(scanFileLocation);
        final FileFullReport fileFullReport = getFileFullReport(scanFile);
        return new JavaElementVisitor() {

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                if (fileFullReport == null || JNomadInspection.jnomad == null) {
                    return;
                }

                String methodCallName = expression.getMethodExpression().getReferenceName();
                if (isCheckedType(expression.getType()) && methodCallName != null && methodCallName.toLowerCase().contains("query")) {
                    int lineNumber = getLineNumber(scanFile, expression.getTextRange());

                    //recommend indexes
                    for (QueryScore queryScore : fileFullReport.getQueryScoreList()) {
                        if ((lineNumber == queryScore.getQueryLocation().begin.line || lineNumber == queryScore.getQueryLocation().end.line)) {
                            for (RecommendedIndex rIndex : fileFullReport.getRecommendedIndexList()) {
                                if (rIndex.isIndexAffect(queryScore.getOriginalQuery())) {
                                    holder.registerProblem(expression.getArgumentList(),
                                            "Missing index detected! Recommended Index: " + rIndex.getIndexCreateSQL()
                                                    + "\nIndex Priority: " + rIndex.getIndexPriority());
                                    System.out.println("Registered missing index to expression: " + expression + " - Line number: " + lineNumber);
                                    return;
                                }
                            }
                        }
                    }

                    //slow queries
                    for (QueryScore queryScore : fileFullReport.getQueryScoreList()) {
                        if ((lineNumber == queryScore.getQueryLocation().begin.line || lineNumber == queryScore.getQueryLocation().end.line)
                                && queryScore.getScore() >= pluginConfiguration.getSlowQueryThreshold()) {
                            holder.registerProblem(expression.getArgumentList(), "Slow query detected! Query score: " + queryScore.getScore());
                            System.out.println("Registered slow query to expression: " + expression + " - Line number: " + lineNumber);
                            return;
                        }
                    }

                    //failed queries
                    QueryIndexReport indexReport = fileFullReport.getQueryIndexReport();
                    List<String> failedQueryParseList = indexReport.getFailedQueryParseList();
                    for (String failedQuery : failedQueryParseList) {
                        SourceCodeExtract sourceCodeExtract = indexReport.getSourceCodeExtractMap().get(failedQuery);
                        if (sourceCodeExtract != null) {
                            Range failedQueryRange = sourceCodeExtract.getQueryLiteralExtractor().getQueryCallRange(failedQuery);
                            if (lineNumber == failedQueryRange.begin.line || lineNumber == failedQueryRange.end.line) {
                                holder.registerProblem(expression.getArgumentList(), "Invalid query detected! Reason: " + indexReport.getFailedQueryReason(failedQuery));
                                System.out.println("Registered invalid query to expression: " + expression + " - Line number: " + lineNumber);
                                return;
                            }
                        }
                    }
                }
            }
        };
    }

    private synchronized FileFullReport getFileFullReport(File f) {
        FileFullReport fileReport = null;
        try {
            if (f.exists() && f.getAbsolutePath().endsWith("java")) {
                String md5Hash = com.google.common.io.Files.hash(f, Hashing.md5()).toString();
                fileReport = fileReportCache.getIfPresent(md5Hash);

                if (fileReport == null && JNomadInspection.jnomad != null) { //no cache; load file from disk
                    QueryLiteralExtractor.isDisabled = false;
                    SourceCodeExtract extract = JNomadInspection.jnomad.scanSingleFile(f);
                    if (extract.getQueryLiteralExtractor().getQueryFound()) {
                        List<SourceCodeExtract> scanList = Collections.singletonList(extract);
                        queryParser.run(scanList);
                        fileReport = new FileFullReport(f, JNomadInspection.jnomad, databaseDataType, queryParser.getAliasMap(), scanList);
                        fileReportCache.put(md5Hash, fileReport);
                    }
                }
            }
        } catch (IOException e) {
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

    @Contract("null -> false")
    private boolean isCheckedType(PsiType type) {
        if (!(type instanceof PsiClassType)) return false;

        PsiClass element = ((PsiClassType) type).resolveGenerics().getElement();
        StringTokenizer tokenizer = new StringTokenizer(CHECKED_CLASSES, ";");
        while (tokenizer.hasMoreTokens()) {
            String className = tokenizer.nextToken();
            if (element != null && className.equals(element.getQualifiedName())) return true;
            if (type.equalsToText(className)) return true;
        }

        return false;
    }

    @Override
    public void cleanup(@NotNull Project project) {
        super.cleanup(project);
        if (jnomad != null) jnomad.closeCache();
    }

    private static int getLineNumber(File f, TextRange textRange) {
        try {
            BufferedReader br = java.nio.file.Files.newBufferedReader(f.toPath());
            String line;
            int pos = 0;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                pos += line.length() + 1; //+1 for new line
                lineNumber++;

                if (pos >= textRange.getStartOffset()) {
                    return lineNumber;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

}