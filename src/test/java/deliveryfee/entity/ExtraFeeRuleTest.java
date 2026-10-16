package deliveryfee.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExtraFeeRuleTest {

    // --- appliesTo ---

    @ParameterizedTest
    @CsvSource({
            "-10.1, false",
            "-10.0, true",
            "-0.1, true",
            "0.0, false"
    })
    void appliesTo_temperatureFromMinus10To0(double temperature, boolean expected) {
        ExtraFeeRule rule = rangeRule(ExtraFeeType.AIR_TEMPERATURE, -10.0, true, 0.0, false);

        assertThat(rule.appliesTo(weather(temperature, null, null))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "9.9, false",
            "10.0, true",
            "20.0, true",
            "20.1, false"
    })
    void appliesTo_windFrom10To20Inclusive(double windSpeed, boolean expected) {
        ExtraFeeRule rule = rangeRule(ExtraFeeType.WIND_SPEED, 10.0, true, 20.0, true);

        assertThat(rule.appliesTo(weather(null, windSpeed, null))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "20.0, false",
            "20.1, true",
            "35.0, true"
    })
    void appliesTo_windAbove20WithoutUpperBound(double windSpeed, boolean expected) {
        ExtraFeeRule rule = rangeRule(ExtraFeeType.WIND_SPEED, 20.0, false, null, false);

        assertThat(rule.appliesTo(weather(null, windSpeed, null))).isEqualTo(expected);
    }

    @Test
    void appliesTo_missingValue_doesNotApply() {
        ExtraFeeRule rule = rangeRule(ExtraFeeType.WIND_SPEED, 10.0, true, 20.0, true);

        assertThat(rule.appliesTo(weather(-5.0, null, "Light rain"))).isFalse();
    }

    @Test
    void appliesTo_phenomenonCategoryMatches() {
        ExtraFeeRule rule = phenomenonRule(PhenomenonCategory.SNOW);

        assertThat(rule.appliesTo(weather(null, null, "Light snow shower"))).isTrue();
        assertThat(rule.appliesTo(weather(null, null, "Light rain"))).isFalse();
        assertThat(rule.appliesTo(weather(null, null, "Overcast"))).isFalse();
        assertThat(rule.appliesTo(weather(null, null, null))).isFalse();
    }

    // --- overlaps ---

    @ParameterizedTest(name = "{0} vs {1} -> {2}")
    @CsvSource({
            "'[10, 20]',    '[15, 25]',  true",
            "'[10, 20]',    '(20, inf)', false",
            "'[10, 20]',    '[20, inf)', true",
            "'(-inf, -10)', '[-10, 0)',  false",
            "'[10, 20]',    '[0, 5)',    false",
            "'[10, 20]',    '[12, 15]',  true",
            "'(-inf, inf)', '[0, 1]',    true"
    })
    void overlaps_ranges(String first, String second, boolean expected) {
        ExtraFeeRule a = parseRange(first);
        ExtraFeeRule b = parseRange(second);

        assertThat(a.overlaps(b)).isEqualTo(expected);
        assertThat(b.overlaps(a)).isEqualTo(expected);
    }

    @Test
    void overlaps_differentVehicleOrType_isFalse() {
        ExtraFeeRule bikeWind = rangeRule(ExtraFeeType.WIND_SPEED, 10.0, true, 20.0, true);
        ExtraFeeRule bikeTemperature = rangeRule(ExtraFeeType.AIR_TEMPERATURE, 10.0, true, 20.0, true);
        ExtraFeeRule scooterWind = ExtraFeeRule.builder()
                .type(ExtraFeeType.WIND_SPEED)
                .vehicleType(VehicleType.SCOOTER)
                .minValue(10.0).minInclusive(true)
                .maxValue(20.0).maxInclusive(true)
                .fee(new BigDecimal("0.50"))
                .validFrom(Instant.EPOCH)
                .build();

        assertThat(bikeWind.overlaps(bikeTemperature)).isFalse();
        assertThat(bikeWind.overlaps(scooterWind)).isFalse();
    }

    @Test
    void overlaps_phenomenonRules_onlySameCategory() {
        assertThat(phenomenonRule(PhenomenonCategory.SNOW).overlaps(phenomenonRule(PhenomenonCategory.SNOW))).isTrue();
        assertThat(phenomenonRule(PhenomenonCategory.SNOW).overlaps(phenomenonRule(PhenomenonCategory.RAIN))).isFalse();
    }

    // --- isCurrent ---

    @Test
    void isCurrent_expiredRule_isFalse() {
        ExtraFeeRule rule = phenomenonRule(PhenomenonCategory.SNOW);
        assertThat(rule.isCurrent()).isTrue();

        rule.expire(Instant.parse("2026-10-15T12:00:00Z"));

        assertThat(rule.isCurrent()).isFalse();
    }

    // --- test data helpers ---

    // Parses ranges like "[10, 20]", "(20, inf)" or "(-inf, -10)" into a wind rule for bikes
    private static ExtraFeeRule parseRange(String range) {
        boolean minInclusive = range.startsWith("[");
        boolean maxInclusive = range.endsWith("]");
        String[] bounds = range.substring(1, range.length() - 1).split(",");
        Double min = parseBound(bounds[0]);
        Double max = parseBound(bounds[1]);
        return rangeRule(ExtraFeeType.WIND_SPEED, min, minInclusive, max, maxInclusive);
    }

    private static Double parseBound(String bound) {
        String value = bound.trim();
        return value.endsWith("inf") ? null : Double.valueOf(value);
    }

    private static ExtraFeeRule rangeRule(ExtraFeeType type, Double min, boolean minInclusive,
                                          Double max, boolean maxInclusive) {
        return ExtraFeeRule.builder()
                .type(type)
                .vehicleType(VehicleType.BIKE)
                .minValue(min)
                .minInclusive(minInclusive)
                .maxValue(max)
                .maxInclusive(maxInclusive)
                .fee(new BigDecimal("0.50"))
                .validFrom(Instant.EPOCH)
                .build();
    }

    private static ExtraFeeRule phenomenonRule(PhenomenonCategory category) {
        return ExtraFeeRule.builder()
                .type(ExtraFeeType.WEATHER_PHENOMENON)
                .vehicleType(VehicleType.BIKE)
                .phenomenonCategory(category)
                .fee(new BigDecimal("1.00"))
                .validFrom(Instant.EPOCH)
                .build();
    }

    private static WeatherObservation weather(Double temperature, Double windSpeed, String phenomenon) {
        return WeatherObservation.builder()
                .stationName("Tartu-Tõravere")
                .airTemperature(temperature)
                .windSpeed(windSpeed)
                .phenomenon(phenomenon)
                .observedAt(Instant.EPOCH)
                .build();
    }
}