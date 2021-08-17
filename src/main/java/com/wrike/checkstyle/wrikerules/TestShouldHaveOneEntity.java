package com.wrike.checkstyle.wrikerules;

import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TestShouldHaveOneEntity extends AbstractJavaRule {

    private static final String TEST_METHOD_JUNIT4_OR_JUNIT5 = "Test";
    private final String entityAnnotation;
    private final String multipleEntityAnnotation;
    private final boolean mustHaveAnnotation;

    public TestShouldHaveOneEntity(String annotationEntity, String multipleEntityAnnotation, boolean mustHaveAnnotation) {
        super();
        this.entityAnnotation = annotationEntity;
        this.multipleEntityAnnotation = multipleEntityAnnotation;
        this.mustHaveAnnotation = mustHaveAnnotation;
    }

    @Override
    public Object visit(final ASTMethodDeclaration node, final Object data) {
        Set<String> annotationNameSet = node
                .getDeclaredAnnotations()
                .stream()
                .map(ASTAnnotation::getAnnotationName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (annotationNameSet.contains(TEST_METHOD_JUNIT4_OR_JUNIT5)) {
            Set<String> classAnnotationNameSet = node
                    .getFirstParentOfAnyType(ASTClassOrInterfaceDeclaration.class)
                    .getDeclaredAnnotations()
                    .stream()
                    .map(ASTAnnotation::getAnnotationName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (annotationNameSet.stream().anyMatch(annotation -> annotation.equals(multipleEntityAnnotation))
                    || classAnnotationNameSet.stream().anyMatch(annotation -> annotation.equals(multipleEntityAnnotation))) {
                addViolation(data, node);
                return super.visit(node, data);
            }
            boolean methodHasEntityAnnotation = annotationNameSet.stream().anyMatch(annotation -> annotation.equals(entityAnnotation));
            boolean classHasEntityAnnotation = classAnnotationNameSet.stream().anyMatch(annotation -> annotation.equals(entityAnnotation));
            if (mustHaveAnnotation && methodHasEntityAnnotation == classHasEntityAnnotation) {
                addViolation(data, node);
                return super.visit(node, data);
            }
            if (!mustHaveAnnotation && methodHasEntityAnnotation && classHasEntityAnnotation) {
                addViolation(data, node);
            }
        }
        return super.visit(node, data);
    }

}
