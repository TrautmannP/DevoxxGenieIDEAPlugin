package com.devoxx.genie.ui.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;

import java.util.concurrent.atomic.AtomicReference;

@Service
public final class AutoCompletionProvider {

    private final AtomicReference<AutoCompletionContext> ongoingCompletion = new AtomicReference<>();

    public static AutoCompletionProvider getInstance() {
        return ApplicationManager.getApplication().getService(AutoCompletionProvider.class);
    }

    public void provideAutoCompletion() {

    }

    public void clear() {
        AutoCompletionContext completionContext = ongoingCompletion.getAndSet(null);
        // todo: Chancel Ongoing Completion

    }

    public record AutoCompletionContext(Editor editor, int offset, Object task) { }

    public AtomicReference<AutoCompletionContext> getOngoingCompletion() {
        return ongoingCompletion;
    }
}
