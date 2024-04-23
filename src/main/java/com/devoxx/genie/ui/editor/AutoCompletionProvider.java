package com.devoxx.genie.ui.editor;

import com.devoxx.genie.DevoxxGenieClient;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Service
public final class AutoCompletionProvider {

    private AutoCompletionContext ongoingCompletion;

    public static AutoCompletionProvider getInstance() {
        return ApplicationManager.getApplication().getService(AutoCompletionProvider.class);
    }

    public void provideAutoCompletion(Editor editor, int offset) {

        if (ongoingCompletion != null && !ongoingCompletion.task.isDone()) {
            clear();
        }

        InlineAutoCompletionService inlineCompletionService = ApplicationManager.getApplication().getService(InlineAutoCompletionService.class);

        try {
            PsiFile compute = ReadAction.<PsiFile, Throwable>compute(() -> {
                Project project = editor.getProject();
                if (project == null) {
                    return null;
                }
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                String text = editor.getDocument().getText();
                PsiElement elementAt = file.findElementAt(offset);
                String sourceCode = editor.getDocument().getText(new TextRange(0, offset));

                CompletableFuture<String> task = CompletableFuture.supplyAsync(
                        () -> DevoxxGenieClient.getInstance().executeGenieAutocompletionPrompt(sourceCode, "JAVA")
                );

                ongoingCompletion = new AutoCompletionContext(editor, offset, task);

                task.thenAccept(response -> {
                    if (response != null) {
                        inlineCompletionService.show(editor, offset, response);
                    }
                });

                return file;
            });
            System.out.println("Break");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {

        if (ongoingCompletion == null) {
            return;
        }

        if (!ongoingCompletion.task.isDone()) {
            ongoingCompletion.task.cancel(true);
        }

        ongoingCompletion = null;
    }

    public AutoCompletionContext getOngoingCompletion() {
        return ongoingCompletion;
    }

    public record AutoCompletionContext(Editor editor, int offset, Future<?> task) {

    }
}
