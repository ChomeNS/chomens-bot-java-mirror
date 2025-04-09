package me.chayapak1.chomens_bot.util;

import com.google.common.collect.Ordering;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// stolen from https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/utils/Utils.java#L253
// and https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java#L170
public class LevenshteinUtilities {
    public static List<String> searchTitles (final String text, final Collection<String> texts) {
        final Map<String, Integer> output = new ValueComparableMap<>(Ordering.natural());

        for (final String eachText : texts) {
            final int score = searchLevenshteinDefault(text, eachText, false);
            output.put(eachText, output.getOrDefault(eachText, 0) + score);
        }

        return output.keySet().stream().toList();
    }

    public static int searchLevenshteinDefault (final String text, final String filter, final boolean caseSensitive) {
        return levenshteinDistance(caseSensitive ? filter : filter.toLowerCase(), caseSensitive ? text : text.toLowerCase(), 1, 8, 8);
    }

    public static int searchInWords (String text, final String filter) {
        if (filter.isEmpty()) return 1;

        int wordsFound = 0;
        text = text.toLowerCase();
        final String[] words = filter.toLowerCase().split(" ");

        for (final String word : words) {
            if (!text.contains(word)) return 0;
            wordsFound += StringUtils.countMatches(text, word);
        }

        return wordsFound;
    }

    public static int levenshteinDistance (final String from, final String to, final int insCost, final int subCost, final int delCost) {
        final int textLength = from.length();
        final int filterLength = to.length();

        if (textLength == 0) return filterLength * insCost;
        if (filterLength == 0) return textLength * delCost;

        // Populate matrix
        final int[][] d = new int[textLength + 1][filterLength + 1];

        for (int i = 0; i <= textLength; i++) {
            d[i][0] = i * delCost;
        }

        for (int j = 0; j <= filterLength; j++) {
            d[0][j] = j * insCost;
        }

        // Find best route
        for (int i = 1; i <= textLength; i++) {
            for (int j = 1; j <= filterLength; j++) {
                final int sCost = d[i - 1][j - 1] + (from.charAt(i - 1) == to.charAt(j - 1) ? 0 : subCost);
                final int dCost = d[i - 1][j] + delCost;
                final int iCost = d[i][j - 1] + insCost;
                d[i][j] = Math.min(Math.min(dCost, iCost), sCost);
            }
        }

        return d[textLength][filterLength];
    }
}
