package org.jabref.logic.formatter.casechanger;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Converts all characters of the given string to upper case, but does not change words starting with "{"
 */
public class UpperCaseFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("UPPER CASE");
    }

    @Override
    public String getKey() {
        return "upper_case";
    }

    @Override
    public String format(String input) {
        Title title = new Title(input);

        title.getWords().stream().forEach(Word::toUpperCase);

        return title.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "CHANGES ALL LETTERS TO UPPER CASE.");
    }

    @Override
    public String getExampleInput() {
        return "Kde {Amarok}";
    }
}
