package com.lexho.payment.exception;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentExceptionHandlerTest {

    private static final LocalValidatorFactoryBean VALIDATOR = validator();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new PaymentExceptionHandler())
            .setValidator(VALIDATOR)
            .build();

    @AfterAll
    static void closeValidator() {
        VALIDATOR.close();
    }

    @Test
    void mapsExpiredAlreadyPaidAndNotFoundMessagesToSpecificCodes() throws Exception {
        mockMvc.perform(get("/test/exceptions/payment").param("message", "Payment expired"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXPIRED"));

        mockMvc.perform(get("/test/exceptions/payment").param("message", "Booking is already paid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_PAID"));

        mockMvc.perform(get("/test/exceptions/payment").param("message", "Payment transaction not found"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    void mapsUnknownPaymentExceptionMessagesToGenericFailureCode() throws Exception {
        mockMvc.perform(get("/test/exceptions/payment").param("message", "Gateway timeout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.message").value("Gateway timeout"));
    }

    @Test
    void mapsUnexpectedExceptionsToInternalServerError() throws Exception {
        mockMvc.perform(get("/test/exceptions/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.message").value("Payment service is temporarily unavailable"));
    }

    @Test
    void validationFailuresStillReturnBadRequest() throws Exception {
        mockMvc.perform(get("/test/exceptions/validated")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }

    @Validated
    @RestController
    @RequestMapping("/test/exceptions")
    static class ThrowingController {

        @GetMapping("/payment")
        String payment(@RequestParam String message) {
            throw new PaymentException(message);
        }

        @GetMapping("/generic")
        String generic() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/validated")
        String validated(@RequestParam @jakarta.validation.constraints.NotBlank String value) {
            return value;
        }
    }
}
