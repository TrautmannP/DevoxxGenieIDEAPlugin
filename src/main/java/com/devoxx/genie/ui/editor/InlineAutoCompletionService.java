package com.devoxx.genie.ui.editor;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.RangeHighlighter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

@Service
public final class InlineAutoCompletionService {

    public void show(Editor editor, int offset, String completionResponse) {

        // Dismiss any existing inlays
        dismiss();

        if(completionResponse == null || completionResponse.isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {



        });
    }

    public void accept() {

    }

    public void dismiss() {

    }

    private Inlay<?> createInlay() {

    }

    private RangeHighlighter placeholderReplaceTest() {

    }

}
