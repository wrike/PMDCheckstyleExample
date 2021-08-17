package com.wrike.checkstyle.wrikerules;

import com.wrike.annotation.TestCaseId;
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

public class TestMustHaveTestIdAnnotationRule extends AbstractJavaRule {

    @Override
    public Object visit(final ASTMethodDeclaration node, final Object data) {
        ASTAnnotation testAnnotation = node.getAnnotation(Test.class.getCanonicalName());
        ASTAnnotation parameterizedTestAnnotation = node.getAnnotation(ParameterizedTest.class.getCanonicalName());

        if (testAnnotation != null || parameterizedTestAnnotation != null) {
            ASTAnnotation testCaseIdAnnotation = node.getAnnotation(TestCaseId.class.getCanonicalName());
            if (testCaseIdAnnotation == null) {
                addViolation(data, node);
            }
        }
        return super.visit(node, data);
    }
}
