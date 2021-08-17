package com.wrike.checkstyle.wrikerules;

import com.wrike.steps.ScreenshotSteps;
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.AccessNode;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.symboltable.MethodNameDeclaration;
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ScreenshotTestMustHaveScreenshotTag extends AbstractJavaRule {

    private static final String SCREENSHOT_TAG = "SCREENSHOT_TEST";

    private Map<ASTMethodDeclaration, Set<ASTMethodDeclaration>> screenshotMethodUsages;
    private Map<ASTMethodDeclaration, Set<ASTMethodDeclaration>> methodUsages;
    private final Set<ASTMethodDeclaration> visitedMethods = new HashSet<>();

    @Override
    public Object visit(final ASTClassOrInterfaceDeclaration node, final Object data) {

        if (node.isInterface()) {
            return super.visit(node, data);
        }

        if (nodeHasScreenshotTag(node)) {
            return super.visit(node, data);
        }

        Optional<Entry<VariableNameDeclaration, List<NameOccurrence>>> screenshotStepsDeclaration = getScreenshotStepsDeclaration(node);
        if (screenshotStepsDeclaration.isEmpty()) {
            return super.visit(node, data);
        }

        Set<ASTMethodDeclaration> screenshotMethods =
                screenshotStepsDeclaration.get().getValue().stream()
                        .map(nameOcc -> nameOcc.getLocation().getFirstParentOfType(ASTMethodDeclaration.class))
                        .collect(Collectors.toSet());

        methodUsages = node.getScope().getDeclarations(MethodNameDeclaration.class).entrySet().stream()
                .map(this::getDependedMethods)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        screenshotMethodUsages = methodUsages.entrySet().stream()
                .filter(entry -> screenshotMethods.contains(entry.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        Set<ASTMethodDeclaration> allScreenshotMethods = dfsTree();

        allScreenshotMethods.forEach(method -> {
            Set<? extends Class<?>> annotationSet = method.getParent()
                    .findChildrenOfType(ASTAnnotation.class)
                    .stream()
                    .map(ASTAnnotation::getType)
                    .collect(Collectors.toSet());
            if ((annotationSet.contains(ParameterizedTest.class) || annotationSet.contains(Test.class))
                    && !nodeHasScreenshotTag(method)) {
                addViolation(data, method);
            }
        });

        return super.visit(node, data);
    }

    private boolean nodeHasScreenshotTag(AccessNode node) {
        return node.getParent().findChildrenOfType(ASTAnnotation.class).stream()
                .filter(a -> Tag.class.equals(a.getType()) || Tags.class.equals(a.getType()))
                .flatMap(e -> e.findDescendantsOfType(ASTName.class).stream())
                .anyMatch(name -> SCREENSHOT_TAG.equals(name.getImage()));
    }

    private Set<ASTMethodDeclaration> dfsTree() {
        screenshotMethodUsages.keySet().forEach(this::dfsNode);
        return visitedMethods;
    }

    private void dfsNode(ASTMethodDeclaration methodDeclaration) {
        if (visitedMethods.contains(methodDeclaration)) {
            return;
        }
        visitedMethods.add(methodDeclaration);
        methodUsages.get(methodDeclaration).forEach(this::dfsNode);
    }

    private Optional<Entry<VariableNameDeclaration, List<NameOccurrence>>> getScreenshotStepsDeclaration(final ASTClassOrInterfaceDeclaration node) {
        Map<VariableNameDeclaration, List<NameOccurrence>> variableDeclarations =
                node.getScope().getDeclarations(VariableNameDeclaration.class);
        return variableDeclarations.entrySet().stream()
                .filter(decl -> ScreenshotSteps.class.equals(decl.getKey().getType()))
                .findFirst();
    }

    private Entry<ASTMethodDeclaration, Set<ASTMethodDeclaration>> getDependedMethods(Entry<MethodNameDeclaration, List<NameOccurrence>> entry) {
        return Map.entry(entry.getKey().getNode().getFirstParentOfType(ASTMethodDeclaration.class),
                entry.getValue().stream()
                        .map(nameOcc -> nameOcc.getLocation().getFirstParentOfType(ASTMethodDeclaration.class))
                        .collect(Collectors.toSet()));
    }

}
