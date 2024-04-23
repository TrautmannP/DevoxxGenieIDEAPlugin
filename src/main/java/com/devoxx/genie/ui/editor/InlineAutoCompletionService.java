package com.devoxx.genie.ui.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service
public final class InlineAutoCompletionService {

    private InlineCompletionContext shownInlineCompletion;

    public void show(Editor editor, int offset, String completionResponse) {

        // Dismiss any existing inlays
        dismiss();

        // Return if completion is empty
        if (completionResponse == null || completionResponse.isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {

            // Return if provided offset differs from editor caret offset
            if (editor.getCaretModel().getOffset() != offset) {
                return;
            }

            String text = completionResponse;

            int currentLineNumber = editor.getDocument().getLineNumber(offset);
            int currentLineEndOffset = editor.getDocument().getLineEndOffset(currentLineNumber);

            List<String> textLines = new ArrayList<>(List.of(text.split("\n")));
            List<Inlay<?>> inlays = new ArrayList<>();
            List<RangeHighlighter> markups = new ArrayList<>();

            // todo: Split response into valid completions

            Inlay<?> inlay = createInlay(editor, textLines.get(0), offset, 0);
            inlays.add(inlay);

            if (textLines.size() > 1) {
                for (int i = 1; i < textLines.size(); i++) {
                    inlay = createInlay(editor, textLines.get(i), offset, i);
                    inlays.add(inlay);
                }
            }

            long displayAt = System.currentTimeMillis();
            String id = "view-" + Math.random();
            shownInlineCompletion = new InlineCompletionContext(editor, offset, text, inlays, markups, id, displayAt);

        });
    }

    public void accept() {
        InlineCompletionContext inlineCompletion = shownInlineCompletion;
        if (inlineCompletion != null) {

        }
    }

    public void dismiss() {
        InlineCompletionContext inlineCompletion = shownInlineCompletion;
        if (inlineCompletion != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                inlineCompletion.inlays.forEach(Disposer::dispose);
                inlineCompletion.markups.forEach(inlineCompletion.editor.getMarkupModel()::removeHighlighter);
                shownInlineCompletion = null;
            });
        }
    }

    private Inlay<?> createInlay(Editor editor, String text, int offset, int lineOffset) {
        EditorCustomElementRenderer renderer = new EditorCustomElementRenderer() {

            @Override
            public @NonNls String getContextMenuGroupId(@NotNull Inlay inlay) {
                return "Devoxx.InlineCompletionContextMenu";
            }

            @Override
            public int calcWidthInPixels(@NotNull Inlay inlay) {
                return Math.max(getWidth(inlay.getEditor(), text), 1);
            }

            @Override
            public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
                g.setFont(getFont(inlay.getEditor()));
                g.setColor(JBColor.GRAY);
                g.drawString(text, targetRegion.x, targetRegion.y + inlay.getEditor().getAscent());
            }

            private Font getFont(Editor editor) {
                Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
                return UIUtil.getFontWithFallbackIfNeeded(font, text).deriveFont((float) editor.getColorsScheme().getEditorFontSize());
            }

            private int getWidth(Editor editor, String line) {
                Font font = getFont(editor);
                FontMetrics metrics = FontInfo.getFontMetrics(font, FontInfo.getFontRenderContext(editor.getContentComponent()));
                return metrics.stringWidth(line);
            }
        };

        if (lineOffset == 0) {
            return editor.getInlayModel().addInlineElement(offset, true, renderer);
        } else {
            return editor.getInlayModel().addBlockElement(offset, true, false, -lineOffset, renderer);
        }
    }

    private RangeHighlighter placeholderReplaceTest() {
        return null;
    }

    private record InlineCompletionContext(Editor editor, int offset, String completion, List<Inlay<?>> inlays,
                                           List<RangeHighlighter> markups, String id, Long displayAt) {
    }

}
