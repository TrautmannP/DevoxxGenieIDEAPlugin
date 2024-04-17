package com.devoxx.genie.ui.editor;

import com.devoxx.genie.DevoxxGenieIcons;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GenerateTestsLineMarkerProvider implements LineMarkerProvider {

    Function<PsiElement, String> tooltipProvider = (element) -> {
        if (element.getParent() instanceof PsiMethod) {
            return "Generate test for method";
        } else if (element.getParent() instanceof PsiClass) {
            return "Generate test for class";
        } else {
            throw new IllegalStateException("Unsupported element type: " + element.getClass());
        }
    };

    private static boolean isClassIdentifier(@NotNull PsiElement element) {
        return element instanceof PsiIdentifier && element.getParent() instanceof PsiClass;
    }

    private static boolean isMethodIdentifier(@NotNull PsiElement element) {
        return element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod;
    }

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {

        if (!SettingsState.getInstance().isShouldShowGenerateTestGutterIcon()) {
            return null;
        } else {
            boolean classIdentifier = isClassIdentifier(element);
            boolean methodIdentifier = isMethodIdentifier(element);
            if (!classIdentifier && !methodIdentifier) {
                return null;
            } else {
                // todo: Check if current file is a test file

                return new GenerateTestLineMarkerInfo(element, DevoxxGenieIcons.DevoxxHead, this.tooltipProvider);
            }
        }
    }

    static class GenerateTestLineMarkerInfo extends LineMarkerInfo<PsiElement> {
        public GenerateTestLineMarkerInfo(@NotNull PsiElement element, Icon icon, Function<PsiElement, String> tooltipProvider) {
            super(element, element.getTextRange(), icon, tooltipProvider, null, GutterIconRenderer.Alignment.LEFT, () -> tooltipProvider.apply(element));
        }
    }
}
