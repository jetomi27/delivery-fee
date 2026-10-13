package deliveryfee.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_observation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherObservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stationName;

    private String wmoCode;

    private Double airTemperature;

    private Double windSpeed;

    private String phenomenon;

    @Column(nullable = false)
    private Instant observedAt;

    @Builder
    public WeatherObservation(String stationName, String wmoCode, Double airTemperature,
                              Double windSpeed, String phenomenon, Instant observedAt) {
        this.stationName = stationName;
        this.wmoCode = wmoCode;
        this.airTemperature = airTemperature;
        this.windSpeed = windSpeed;
        this.phenomenon = phenomenon;
        this.observedAt = observedAt;
    }
}
