package com.codebrig.jnomad.plugin.intellij.inspection;

import com.codebrig.jnomad.model.FileFullReport;
import com.codebrig.jnomad.model.QueryScore;
import com.codebrig.jnomad.model.RecommendedIndex;
import com.codebrig.jnomad.model.SourceCodeExtract;
import com.codebrig.jnomad.task.explain.QueryIndexReport;
import com.github.javaparser.Range;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class JNomadQueryVisitor extends JavaElementVisitor {

    @NonNls
    private static final String CHECKED_CLASSES = "javax.persistence.Query;javax.persistence.TypedQuery;java.sql.PreparedStatement";

    private final ProblemsHolder holder;
    private final VirtualFile virtualFile;
    private final FileFullReport fileFullReport;

    JNomadQueryVisitor(ProblemsHolder holder, VirtualFile virtualFile, FileFullReport fileFullReport) {
        this.holder = holder;
        this.virtualFile = virtualFile;
        this.fileFullReport = fileFullReport;
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (fileFullReport == null || JNomadInspection.jnomad == null) {
            return;
        }

        String methodCallName = expression.getMethodExpression().getReferenceName();
        if (isCheckedType(expression.getType()) && methodCallName != null && methodCallName.toLowerCase().contains("query")) {
            int lineNumber = getLineNumber(this.virtualFile, expression.getTextRange());

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
                        && queryScore.getScore() >= JNomadInspection.pluginConfiguration.getSlowQueryThreshold()) {
                    holder.registerProblem(expression.getArgumentList(), "Slow query detected! Query score: " + queryScore.getScore());
                    System.out.println("Registered slow query to expression: " + expression + " - Line number: " + lineNumber);
                    return;
                }
            }

            //failed queries
            Map<String, SourceCodeExtract> failedParseQueries = JNomadInspection.queryParser.getFailedQueries();
            QueryIndexReport indexReport = fileFullReport.getQueryIndexReport();
            List<String> failedQueryParseList = indexReport.getFailedQueryParseList();
            failedQueryParseList.addAll(failedParseQueries.keySet());

            for (String failedQuery : failedQueryParseList) {
                SourceCodeExtract sourceCodeExtract = failedParseQueries.get(failedQuery);
                if (sourceCodeExtract == null) {
                    sourceCodeExtract = indexReport.getSourceCodeExtractMap().get(failedQuery);
                }
                if (sourceCodeExtract != null) {
                    Range failedQueryRange = sourceCodeExtract.getQueryLiteralExtractor().getQueryCallRange(failedQuery);
                    if (lineNumber == failedQueryRange.begin.line || lineNumber == failedQueryRange.end.line) {
                        String reason = indexReport.getFailedQueryReason(failedQuery);
                        if (reason == null) {
                            reason = "Failed to parse query";
                        }

                        holder.registerProblem(expression.getArgumentList(), "Invalid query detected! Reason: " + reason);
                        System.out.println("Registered invalid query to expression: " + expression + " - Line number: " + lineNumber);
                        return;
                    }
                }
            }
        }
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


    private static int getLineNumber(VirtualFile virtualFile, TextRange textRange) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(virtualFile.getInputStream()));
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
