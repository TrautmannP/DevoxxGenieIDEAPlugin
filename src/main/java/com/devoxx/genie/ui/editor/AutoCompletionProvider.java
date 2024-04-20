package com.devoxx.genie.ui.editor;

import com.devoxx.genie.DevoxxGenieClient;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;

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

        CompletableFuture<String> task = CompletableFuture.supplyAsync(
                () -> DevoxxGenieClient.getInstance().executeGeniePrompt("", "", "")
        );

        ongoingCompletion = new AutoCompletionContext(editor, offset, task);
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
