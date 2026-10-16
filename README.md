# Delivery Fee Calculator

A Spring Boot service that calculates the delivery fee for food couriers based on the city, the vehicle type and the current weather. Weather observations are imported on a schedule from the Estonian Environment Agency, and all fee rules (regional base fees and weather extra fees) can be managed through a REST API.

## Tech stack

- Java 21, Spring Boot 4, Gradle
- H2 database
- Jackson XML for parsing weather data
- MapStruct and Lombok
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito

## Running

Requirements: JDK 21. Gradle is downloaded automatically by the wrapper.

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

Run the tests:

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test  
```

On the first start the application loads the regional base fees and weather extra fee rules from the task description, and imports the current weather once. After that the weather is imported every hour at HH:15:00.

## URLs

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI description | http://localhost:8080/v3/api-docs |
| H2 console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/delivery-fee`, user `sa`, empty password) |

## API

### Delivery fee

| Method | Path | Description |
|---|---|---|
| GET | `/api/delivery-fee?city=TARTU&vehicleType=BIKE` | Fee based on the latest weather data |
| GET | `/api/delivery-fee?city=TARTU&vehicleType=BIKE&datetime=2026-10-14T12:00:00Z` | Fee based on the rules and weather valid at the given moment |

Cities: `TALLINN`, `TARTU`, `PARNU`. Vehicle types: `CAR`, `SCOOTER`, `BIKE`.

`datetime` is optional and uses ISO 8601 with an offset, for example `2026-10-14T12:00:00Z` or `2026-10-14T15:00:00+03:00` (encode `+` as `%2B` in a URL).

Example response:

```json
{
  "city": "TARTU",
  "vehicleType": "BIKE",
  "fee": 4.00,
  "warnings": []
}
```

### Regional base fees

| Method | Path | Description |
|---|---|---|
| GET | `/api/base-fees` | Current base fees |
| GET | `/api/base-fees/{id}` | One base fee, including expired versions |
| POST | `/api/base-fees` | Create a fee for a city and vehicle type that has no current fee |
| PUT | `/api/base-fees/{id}` | Change the amount (creates a new version) |
| DELETE | `/api/base-fees/{id}` | Expire the fee |

### Weather extra fee rules

| Method | Path | Description |
|---|---|---|
| GET | `/api/extra-fee-rules` | Current rules |
| GET | `/api/extra-fee-rules/{id}` | One rule, including expired versions |
| POST | `/api/extra-fee-rules` | Create a rule |
| PUT | `/api/extra-fee-rules/{id}` | Change bounds, fee or forbidden flag (creates a new version) |
| DELETE | `/api/extra-fee-rules/{id}` | Expire the rule |

Full request and response descriptions are available in Swagger UI.

### Response codes

| Code | When |
|---|---|
| 200 / 201 / 204 | Success |
| 400 | Missing or invalid parameter or request body |
| 404 | Unknown id, or no base fee or weather data for the requested moment |
| 409 | Fee already exists, rules overlap, or the fee or rule is already expired |
| 415 | Request body is not JSON |
| 422 | Weather conditions forbid the selected vehicle type |

## Configuration

`src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `weather.import.cron` | `0 15 * * * *` | Weather import schedule (Spring cron with seconds) |
| `weather.import.on-startup` | `true` | Import weather once when the application starts |
| `weather.api.url` | ilmateenistus observations URL | Weather data source |

## Architecture

```
deliveryfee
├── client       downloads and parses the weather XML
├── config       web configuration, default data, startup import
├── controller   REST endpoints
├── dto          request and response objects, XML objects
├── entity       JPA entities and enums
├── exception    exceptions and the global exception handler
├── mapper       MapStruct mappers between entities and DTOs
├── repository   Spring Data repositories
├── scheduler    scheduled weather import
└── service      business logic
```

**Startup:** `BaseFeeDataInitializer` and `ExtraFeeRuleDataInitializer` insert the default fees and rules if their tables are empty, and `WeatherImportOnStartup` runs the first weather import. All three are `ApplicationRunner` beans in the `config` package.

**Weather import:** `WeatherImportScheduler` → `WeatherImportService` → `WeatherClient` (XML) → only the stations of supported cities are kept → saved as new rows, history is never overwritten.

**Fee calculation:** `DeliveryFeeController` → `DeliveryFeeService` finds the base fee, the weather of the city's station and the extra fee rules valid at the requested moment → each rule checks itself against the weather (`ExtraFeeRule.appliesTo`) → a matching forbidding rule stops the calculation, other matching rules add their fee.

**Rules as data:** base fees and extra fee rules are stored in the database, not in code. Adding a rule, changing a threshold or giving a new vehicle type its own fees needs no code change.

**Versioned rules:** every base fee and rule has `validFrom` and `validTo`. A change expires the current version and creates a new one at the same moment, so a calculation for a past `datetime` uses the rules that were valid at that time.

## Design decisions

- **Range boundaries.** "Less than -10" excludes -10, so -10 °C gives 0.5 €. "Greater than 20" excludes 20, so 20 m/s gives 0.5 €. 10 m/s is included in "between 10 and 20". 0 °C gives no temperature fee. Because different rules include different boundaries, every range rule stores whether each bound is inclusive.
- **Weather phenomena** are classified by keywords (`snow`, `sleet`, `rain`, `shower`, `glaze`, `hail`, `thunder`). Snow is checked before rain, because "Light snow shower" contains both "snow" and "shower". `Thunder` is not in the source's phenomenon list but is supported because the task requires it. An empty phenomenon means no special weather.
- **Missing weather values.** If temperature or wind speed is missing, the related rules are not applied and the response contains a warning. The warning is only added for vehicle types that have rules of that kind.
- **Outdated weather.** If the latest observation is more than 2 hours older than the calculation moment, the response contains a warning.
- **Observation time.** The `timestamp` attribute of the XML is used as the observation time. The source regenerates the file on request, so this is the moment the data was fetched rather than the exact measurement time.
- **Import.** Only Tallinn-Harku, Tartu-Tõravere and Pärnu are stored. An observation already stored for the same station and time is skipped. A failed import is logged and does not stop the application.
- **Changing data never overwrites history.** PUT expires the current version and creates a new one with a new id. DELETE only expires. Expired fees and rules cannot be changed again.
- **Conflicts.** POST returns 409 if a current base fee already exists for the city and vehicle type (use PUT instead). Extra fee rules of the same type and vehicle type must not overlap, otherwise one weather value would be charged twice.
- **Status codes.** A forbidden vehicle type returns 422: the request is valid, but the business rules do not allow it. Missing data for the requested moment returns 404.
- **Default data** is loaded only if the corresponding table is empty, and is treated as valid since 1970, so calculations for any past moment find it.
- **Extra fee rules are not city specific.** The rules are the same for all cities, the city only selects the weather station.

## Testing

| Level | Examples | What it checks |
|---|---|---|
| Unit tests | `ExtraFeeRuleTest`, `PhenomenonCategoryTest`, request DTO validation | Domain logic and edge cases, no Spring |
| Unit tests with mocks | `DeliveryFeeServiceTest`, `BaseFeeServiceTest`, `WeatherImportServiceTest` | Services in isolation with Mockito |
| Repository tests | `BaseFeeRepositoryTest`, `ExtraFeeRuleRepositoryTest` | JPQL queries on a real in-memory database |
| Web tests | `DeliveryFeeControllerTest`, `BaseFeeControllerTest` | HTTP status codes, JSON, validation and error messages |
| Integration tests | `DeliveryFeeIntegrationTest` | Whole application: the example from the task (4 €), a forbidden vehicle, a fee change with a calculation before and after it |

Tests use a separate in-memory database and never call the real weather service.

## Data source

Weather data: Estonian Environment Agency (Keskkonnaagentuur), [ilmateenistus.ee](https://www.ilmateenistus.ee).
