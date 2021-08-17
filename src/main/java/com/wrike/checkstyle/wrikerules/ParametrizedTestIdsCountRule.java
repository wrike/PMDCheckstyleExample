package com.wrike.checkstyle.wrikerules;

import com.wrike.annotation.TestCaseId;
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTMemberValue;
import net.sourceforge.pmd.lang.java.ast.ASTMemberValueArrayInitializer;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix;
import net.sourceforge.pmd.lang.java.ast.ASTReturnStatement;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.Set;

public class ParametrizedTestIdsCountRule extends AbstractJavaRule {

    private static final String PARAMETRIZED_TEST = ParameterizedTest.class.getCanonicalName();
    private static final String VALUE_SOURCE = ValueSource.class.getCanonicalName();
    private static final String CSV_SOURCE = CsvSource.class.getCanonicalName();
    private static final String METHOD_SOURCE = MethodSource.class.getCanonicalName();
    private static final String TASK_CASE_ID = TestCaseId.class.getCanonicalName();
    private static final String PROVIDERS_PACKAGE = MethodSource.class.getPackageName();

    @Override
    public Object visit(final ASTMethodDeclaration node, final Object data) {

        if (node.getAnnotation(PARAMETRIZED_TEST) == null) {
            return super.visit(node, data);
        }

        if (node.getAnnotation(TASK_CASE_ID) == null) {
            addViolationWithMessage(data, node, "Test should have TestCaseId annotation");
            return super.visit(node, data);
        }

        Optional<ASTAnnotation> providerAnnotation = node.getDeclaredAnnotations().stream()
                .filter(annotation -> PROVIDERS_PACKAGE.equals(annotation.getType().getPackageName()))
                .findFirst();

        if (providerAnnotation.isEmpty()) {
            addViolationWithMessage(data, node, "Parametrized test should have data provider");
            return super.visit(node, data);
        }

        if (!Set.of(VALUE_SOURCE, CSV_SOURCE, METHOD_SOURCE).contains(providerAnnotation.get().getType().getTypeName())) {
            addViolationWithMessage(data, node, "Parametrized test should have only ValueSource, CSVSource or MethodSource provider");
            return super.visit(node, data);
        }

        try {
            int expectedArgsCount = getAnnotationArgsCount(providerAnnotation.get());
            int actualArgsCount = getAnnotationArgsCount(node.getAnnotation(TASK_CASE_ID));
            if (actualArgsCount != expectedArgsCount) {
                addViolation(data, node, new Object[]{expectedArgsCount, actualArgsCount});
            }
        } catch (IllegalStateException e) {
            addViolationWithMessage(data, node, e.getMessage());
        }

        return super.visit(node, data);
    }

    private int getAnnotationArgsCount(ASTAnnotation annotation) {
        if (METHOD_SOURCE.equals(annotation.getType().getTypeName())) {
            return getMethodSourceArgsCount(annotation);
        }
        ASTMemberValueArrayInitializer annotationValue = annotation.getFirstDescendantOfType(ASTMemberValueArrayInitializer.class);
        if (annotationValue == null) {
            if (annotation.getFirstDescendantOfType(ASTMemberValue.class) != null) {
                return 1;
            }
            return 0;
        }
        return annotationValue.getNumChildren();
    }

    private Integer getMethodSourceArgsCount(ASTAnnotation annotation) {
        String methodName = annotation.getFirstDescendantOfType(ASTLiteral.class).getImage().replaceAll("\"", "");
        ASTMethodDeclaration providerMethod = getMethodByName(annotation.getFirstParentOfType(ASTClassOrInterfaceDeclaration.class), methodName);
        ASTReturnStatement returnStatement = providerMethod.getFirstDescendantOfType(ASTReturnStatement.class);
        ASTName returnPrefixName = returnStatement.getFirstDescendantOfType(ASTPrimaryPrefix.class).getFirstDescendantOfType(ASTName.class);
        if (returnPrefixName == null || !returnPrefixName.getImage().startsWith("List.of")) {
            throw new IllegalStateException("Provider method should return List.of() value");
        }
        return returnStatement.getFirstDescendantOfType(ASTArgumentList.class).size();
    }

    private ASTMethodDeclaration getMethodByName(ASTClassOrInterfaceDeclaration node, String methodName) {
        Optional<ASTMethodDeclaration> methodOptional = node.findDescendantsOfType(ASTMethodDeclaration.class).stream()
                .filter(methodDeclaration -> methodDeclaration.getName().equals(methodName))
                .findFirst();
        if (methodOptional.isEmpty()) {
            throw new IllegalStateException(String.format("Provider method %s doesn't exist", methodName));
        }
        return methodOptional.get();
    }

}
