package com.wrike.checkstyle.wrikerules;

import com.wrike.annotation.TestCaseId;
import com.wrike.checkstyle.DuplicateTestCaseIdUtils;
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTSingleMemberAnnotation;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.util.Map;

public class CheckDuplicateTestIdRule extends AbstractJavaRule {

    @Override
    public Object visit(final ASTMethodDeclaration node, final Object data) {
        ASTAnnotation testCaseIdAnnotation = node.getAnnotation(TestCaseId.class.getCanonicalName());
        if (testCaseIdAnnotation != null) {
            Map<Integer, Integer> allIds = DuplicateTestCaseIdUtils.findAllIds();
            ASTSingleMemberAnnotation memberAnnotation = (ASTSingleMemberAnnotation) testCaseIdAnnotation.getChild(0);
            memberAnnotation.findDescendantsOfType(ASTLiteral.class).stream()
                    .map(ASTLiteral::getValueAsInt)
                    .filter(it -> allIds.get(it) > 1)
                    .forEach(it -> addViolation(data, node, it.toString()));
        }

        return super.visit(node, data);
    }
}
