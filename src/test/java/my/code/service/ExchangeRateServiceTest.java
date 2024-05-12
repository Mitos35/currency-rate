package my.code.service;

import my.code.annotation.IT;
import my.code.database.entity.ExchangeRate;
import my.code.database.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@IT
@Sql(scripts = {"classpath:sql/data.sql"})
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @MockBean
    private NbuCurrencyApiClient nbuCurrencyApiClient;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    void getCurrentExchangeRates() {
        var currentExchangeRates = exchangeRateService.getCurrentExchangeRates();
        assertThat(currentExchangeRates).hasSize(2);
    }

    @Test
    void testGetCurrentExchangeRatesWhenNoDataInDatabase() {
        when(exchangeRateRepository.findLatestRates()).thenReturn(Collections.emptyList());
        String mockApiResponse = "mock response from NBU API";
        when(nbuCurrencyApiClient.getLatestExchangeRates()).thenReturn(mockApiResponse);

        List<ExchangeRate> result = exchangeRateService.getCurrentExchangeRates();
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result).hasSizeGreaterThan(0)
        );
    }

    @Test
    @Sql(scripts = {"classpath:sql/empty_database.sql"})
    void getCurrentExchangeRatesWhenEmptyDatabase() {
        when(nbuCurrencyApiClient.getLatestExchangeRates()).thenReturn(getMockApiResponse());

        List<ExchangeRate> currentExchangeRates = exchangeRateService.getCurrentExchangeRates();

        assertThat(currentExchangeRates).hasSize(3);
    }

    @Test
    void getCurrentExchangeRatesWhenApiError() {
        when(nbuCurrencyApiClient.getLatestExchangeRates()).thenThrow(new RestClientException("API error"));

        assertAll(
                () -> {
                    var exception = assertThrows(RestClientException.class, () -> nbuCurrencyApiClient.getLatestExchangeRates());
                    assertThat(exception.getMessage()).isEqualTo("API error");
                },
                () -> assertThrows(RestClientException.class, () -> nbuCurrencyApiClient.getLatestExchangeRates())
        );
    }

    @Test
    void getExchangeRatesByDateWhenRecordsExist() {
        LocalDate date = LocalDate.of(2024, 5, 11);
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
        var mockExchangeRates = ExchangeRate.builder()
                .baseCurrencyCode("Єна")
                .targetCurrencyCode("JPY")
                .rate(BigDecimal.valueOf(0.25506))
                .dateTime(LocalDateTime.of(2024, 5, 11, 13, 29, 36))
                .build();

        List<ExchangeRate> exchangeRates = exchangeRateService.getExchangeRatesByDate(date);

        assertAll(
                () -> assertThat(exchangeRates).hasSize(4),
                () -> {
                    assert exchangeRates != null;
                    assertThat(exchangeRates.get(2).getTargetCurrencyCode()).isEqualTo(mockExchangeRates.getTargetCurrencyCode());
                }
        );
    }

    @Test
    void getExchangeRatesByDateWhenNoRecords() {
        LocalDate date = LocalDate.of(2024, 5, 10);
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        List<ExchangeRate> exchangeRates = exchangeRateService.getExchangeRatesByDate(date);

        assertThat(exchangeRates).isEmpty();
    }

    @Test
    void deleteExchangeRatesByDate() {
        LocalDate date = LocalDate.of(2024, 5, 11);
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        exchangeRateService.deleteExchangeRatesByDate(date);
        List<ExchangeRate> exchangeRates = exchangeRateService.getExchangeRatesByDate(date);

        assertAll(
                () ->  assertThat(exchangeRates).hasSize(0),
                () -> assertThat(exchangeRates).isEmpty()
        );
    }

    @Test
    void deleteExchangeRatesByDateWithNullDate() {

        assertThrows(NullPointerException.class, () -> exchangeRateService.deleteExchangeRatesByDate(null));
    }

    @Test
    void deleteExchangeRatesByDateWithNoRecords() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        exchangeRateService.deleteExchangeRatesByDate(date);

        verify(exchangeRateRepository, never()).deleteByDateTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    private String getMockApiResponse() {
        return "[{\"txt\":\"Australian Dollar\",\"cc\":\"AUD\",\"rate\":\"26.2394\"}," +
               "{\"txt\":\"Canadian Dollar\",\"cc\":\"CAD\",\"rate\":\"29.0302\"}," +
               "{\"txt\":\"Danish Krone\",\"cc\":\"DKK\",\"rate\":\"5.7387\"}]";
    }


}