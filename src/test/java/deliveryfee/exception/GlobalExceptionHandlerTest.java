package deliveryfee.exception;

import static org.assertj.core.api.Assertions.assertThat;

import deliveryfee.controller.DeliveryFeeController;
import deliveryfee.dto.ErrorResponse;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidEnumValueWithPath_listsAllowedValues() {
        InvalidFormatException cause = InvalidFormatException.from(null, "bad value", "MAARDU", City.class);
        cause.prependPath(new Object(), "city");

        assertThat(messageFor(cause))
                .isEqualTo("Invalid value 'MAARDU' for field 'city'. Allowed values: [TALLINN, TARTU, PARNU]");
    }

    @Test
    void invalidValueWithoutTargetType_hasNoAllowedValues() {
        InvalidFormatException cause = InvalidFormatException.from(null, "bad value", "abc", null);
        cause.prependPath(new Object(), "fee");

        assertThat(messageFor(cause)).isEqualTo("Invalid value 'abc' for field 'fee'");
    }

    @Test
    void invalidValueWithoutPath_returnsGeneralMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "bad value", "abc", BigDecimal.class);

        assertThat(messageFor(cause)).isEqualTo("Request body is missing or has invalid format");
    }

    @Test
    void mismatchedInputWithPath_namesField() {
        MismatchedInputException cause = MismatchedInputException.from(null, BigDecimal.class, "wrong structure");
        cause.prependPath(new Object(), "fee");

        assertThat(messageFor(cause)).isEqualTo("Missing or invalid value for field 'fee'");
    }

    @Test
    void mismatchedInputWithoutPath_returnsGeneralMessage() {
        MismatchedInputException cause = MismatchedInputException.from(null, BigDecimal.class, "wrong structure");

        assertThat(messageFor(cause)).isEqualTo("Request body is missing or has invalid format");
    }

    @Test
    void typeMismatchWithEnum_listsAllowedValues() throws Exception {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "BERLIN", City.class, "city", anyControllerParameter(), null);

        assertThat(handler.handleTypeMismatch(exception).getBody().message())
                .isEqualTo("Invalid value 'BERLIN' for parameter 'city'. Allowed values: [TALLINN, TARTU, PARNU]");
    }

    @Test
    void typeMismatchWithoutRequiredType_hasNoAllowedValues() throws Exception {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", null, "id", anyControllerParameter(), null);

        assertThat(handler.handleTypeMismatch(exception).getBody().message())
                .isEqualTo("Invalid value 'abc' for parameter 'id'");
    }

    // The exception needs a method parameter, its content does not matter for the message
    private static MethodParameter anyControllerParameter() throws NoSuchMethodException {
        return new MethodParameter(
                DeliveryFeeController.class.getMethod("getDeliveryFee", City.class, VehicleType.class, OffsetDateTime.class),
                0);
    }

    private String messageFor(Throwable cause) {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Unreadable body", cause, new MockHttpInputMessage(new byte[0]));
        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(exception);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        return response.getBody().message();
    }
}