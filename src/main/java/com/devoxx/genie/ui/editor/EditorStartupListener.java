package com.devoxx.genie.ui.editor;

import com.devoxx.genie.model.enumarations.AutoCompletionMode;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class EditorStartupListener implements EditorFactoryListener {

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

        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                FileEditor selectedEditor = editorManager.getSelectedEditor();
                if (selectedEditor != null && selectedEditor.equals(editor)) {

                    // Clear if the current caret position changed
                    if (event.getEditor() != editor || event.getCaret() == null
                            || event.getCaret().getOffset() != editor.getCaretModel().getPrimaryCaret().getOffset()) {
                        completionProvider.clear();
                    }
                }
            }
        });

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (editorManager.getSelectedEditor() == editor && settings.getAutoCompletionMode() == AutoCompletionMode.AUTOMATIC) {
                    if(completionProvider.getOngoingCompletion().get() != null ) {

                    }
                }
            }
        };

    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        EditorFactoryListener.super.editorReleased(event);
    }
}
