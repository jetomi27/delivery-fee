package deliveryfee.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Weather phenomenon categories that affect the delivery fee.
 * 
 * Phenomena come from the Estonian Environment Agency observations feed
 * (full list: https://www.ilmateenistus.ee/teenused/ilmainfo/eesti-vaatlusandmed-xml/).
 * A phenomenon is classified by keywords, case insensitive.
 * 
 * Categories are checked in declaration order, so more specific ones must come first:
 * "Light snow shower" contains both "snow" and "shower" and must be classified as snow.
 * Phenomena without a category (clouds, mist, fog, empty value) do not affect the fee.
 */
public enum PhenomenonCategory {
    GLAZE("glaze"),
    HAIL("hail"),
    // Not in the feed's list, but the task requires thunder to be forbidden
    THUNDER("thunder"),
    SLEET("sleet"),
    SNOW("snow"),
    // Rain and rain showers of any intensity, checked last because snow showers also contain "shower"
    RAIN("rain", "shower");

    private final List<String> keywords;

    PhenomenonCategory(String... keywords) {
        this.keywords = List.of(keywords);
    }

    /**
     * Finds the category of a weather phenomenon description from the weather service.
     *
     * @param phenomenon description such as "Light snow shower", may be null
     * @return matching category, or empty if the phenomenon does not affect the fee
     */
    public static Optional<PhenomenonCategory> fromPhenomenon(String phenomenon) {
        if (phenomenon == null || phenomenon.isBlank()) {
            return Optional.empty();
        }
        String text = phenomenon.toLowerCase();
        return Arrays.stream(values())
                .filter(category -> category.keywords.stream().anyMatch(text::contains))
                .findFirst();
    }
}