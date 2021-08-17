package com.wrike.checkstyle.wrikerules;

import net.sourceforge.pmd.lang.java.ast.ASTImportDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import org.junit.jupiter.api.RepeatedTest;

public class ShouldNotImportRepeatedTest extends AbstractJavaRule {

    private static final String REPEATED_TEST = RepeatedTest.class.getCanonicalName();

    @Override
    public Object visit(final ASTImportDeclaration node, final Object data) {
        String nodeImportedName = node.getImportedName();
        if (nodeImportedName.contains(REPEATED_TEST)) {
            addViolation(data, node);
            return data;
        }

        return super.visit(node, data);
    }

}
