package com.devoxx.genie.ui.editor;

import com.devoxx.genie.model.enumarations.AutoCompletionMode;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EditorStartupListener implements EditorFactoryListener {

    private final Map<Editor, Runnable> disposables = new ConcurrentHashMap<>();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {

        Editor editor = event.getEditor();

        AutoCompletionProvider completionProvider = AutoCompletionProvider.getInstance();
        Project project = editor.getProject();

        if (project == null) {
            return;
        }

        SettingsState settings = SettingsState.getInstance();
        FileEditorManager editorManager = FileEditorManager.getInstance(project);

        CaretListener caretListener = getCaretListener(completionProvider, editor, editorManager);
        editor.getCaretModel().addCaretListener(caretListener);

        DocumentListener documentListener = getDocumentListener(completionProvider, editor, settings, editorManager);
        editor.getDocument().addDocumentListener(documentListener);

        MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                completionProvider.clear();
            }
        });

        disposables.put(editor, () -> {
            editor.getCaretModel().removeCaretListener(caretListener);
            editor.getDocument().removeDocumentListener(documentListener);
            messageBusConnection.disconnect();
        });
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        disposables.computeIfPresent(event.getEditor(), (editor, disposable) -> {
            disposable.run();
            return null;
        });
    }

    private CaretListener getCaretListener(AutoCompletionProvider completionProvider, Editor editor, FileEditorManager editorManager) {
        return new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                FileEditor selectedEditor = editorManager.getSelectedEditor();
                if (selectedEditor != null && selectedEditor.equals(editor)) {
                    if (event.getEditor() != editor || event.getCaret() == null
                            || event.getCaret().getOffset() != editor.getCaretModel().getPrimaryCaret().getOffset()) {
                        completionProvider.clear();
                    }
                }
            }
        };
    }

    private DocumentListener getDocumentListener(AutoCompletionProvider completionProvider, Editor editor, SettingsState settings, FileEditorManager editorManager) {
        return new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (editorManager.getSelectedEditor() == editor && settings.getAutoCompletionMode() == AutoCompletionMode.AUTOMATIC) {
                    AutoCompletionProvider.AutoCompletionContext ongoingCompletion = completionProvider.getOngoingCompletion();
                    int primaryCaretOffset = editor.getCaretModel().getPrimaryCaret().getOffset();
                    if (ongoingCompletion == null || ongoingCompletion.offset() != primaryCaretOffset
                            || ongoingCompletion.editor() != editor) {
                        completionProvider.provideAutoCompletion(editor, primaryCaretOffset);
                    }
                }
            }
        };
    }
}
