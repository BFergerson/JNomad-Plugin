package com.codebrig.jnomad.plugin.intellij.inspection;

import com.intellij.codeInspection.InspectionToolProvider;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class JNomadInspectionProvider implements InspectionToolProvider {

    @Override
    public Class[] getInspectionClasses() {
        return new Class[]{JNomadInspection.class};
    }

}
