package org.jabref.gui.fieldeditors;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

public abstract class OptionEditorViewModel<T> extends AbstractEditorViewModel {

    public OptionEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
    }

    public abstract StringConverter<T> getStringConverter();

    public abstract List<T> getItems();

    public abstract String convertToDisplayText(T object);
}
