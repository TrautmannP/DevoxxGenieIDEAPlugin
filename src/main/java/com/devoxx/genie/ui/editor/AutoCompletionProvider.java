package com.devoxx.genie.ui.editor;

import com.devoxx.genie.DevoxxGenieClient;
import com.devoxx.genie.ui.util.Debouncer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import java.util.concurrent.CompletableFuture;

@Service
public final class AutoCompletionProvider {

    private final Debouncer<AutoCompletionContext> completionDebouncer = new Debouncer<>(this::executeAutoCompletion, 500);

    private AutoCompletionContext ongoingCompletion;

    public static AutoCompletionProvider getInstance() {
        return ApplicationManager.getApplication().getService(AutoCompletionProvider.class);
    }

    public void provideAutoCompletion(Editor editor, int offset) {

        if (ongoingCompletion != null && !ongoingCompletion.task.isDone()) {
            clear();
        }

        try {
            ReadAction.<PsiFile, Throwable>compute(() -> {
                Project project = editor.getProject();
                if (project == null) {
                    return null;
                }
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                String sourceCode = editor.getDocument().getText(new TextRange(0, offset));

                CompletableFuture<String> task = CompletableFuture.supplyAsync(
                        () -> DevoxxGenieClient.getInstance().executeGenieAutocompletionPrompt(sourceCode, "JAVA")
                );

                ongoingCompletion = new AutoCompletionContext(editor, offset, task);
                completionDebouncer.call(ongoingCompletion);

                return file;
            });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void executeAutoCompletion(AutoCompletionContext context) {
        context.task.thenAccept(response -> {
            if (response != null) {
                InlineAutoCompletionService inlineCompletionService = ApplicationManager.getApplication()
                        .getService(InlineAutoCompletionService.class);
                inlineCompletionService.show(context.editor, context.offset, response);
            }
        });
    }

    public void clear() {

        if (ongoingCompletion == null) {
            return;
        }

        completionDebouncer.clear();
        if (!ongoingCompletion.task.isDone()) {
            ongoingCompletion.task.cancel(true);
        }

        ongoingCompletion = null;
    }

    public AutoCompletionContext getOngoingCompletion() {
        return ongoingCompletion;
    }

    public record AutoCompletionContext(Editor editor, int offset, CompletableFuture<String> task) {

    }
}
