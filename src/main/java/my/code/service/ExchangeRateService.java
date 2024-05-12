package my.code.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.code.database.entity.ExchangeRate;
import my.code.database.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final NbuCurrencyApiClient nbuCurrencyApiClient;

    @Transactional
    public List<ExchangeRate> getCurrentExchangeRates() {
        log.info("Sending GET request to NBU API for current exchange rates...");
        LocalDate currentDate = LocalDate.now();

        List<ExchangeRate> currentRates = exchangeRateRepository.findByDateTimeBetween(
                currentDate.atStartOfDay(),
                currentDate.atTime(LocalTime.MAX)
        );

        if (currentRates.isEmpty()) {
            String nbuApiResponse = nbuCurrencyApiClient.getLatestExchangeRates();
            return parseAndSaveRates(nbuApiResponse);
        }

        return currentRates;
    }

    public List<ExchangeRate> getExchangeRatesByDate(LocalDate date) {
        log.info("Sending GET request to NBU API for exchange rates by date: {}", date);
        LocalDateTime startOfDay = getStartOfDay(date);
        LocalDateTime endOfDay = getEndOfDay(date);
        return exchangeRateRepository.findByDateTimeBetween(startOfDay, endOfDay);
    }

    @Transactional
    public void deleteExchangeRatesByDate(LocalDate date) {
        LocalDateTime startOfDay = getStartOfDay(date);
        LocalDateTime endOfDay = getEndOfDay(date);
        exchangeRateRepository.deleteByDateTimeBetween(startOfDay, endOfDay);
        log.info("Sending DELETE request to NBU API to delete exchange rates for date: {}", date);
    }

    private static LocalDateTime getEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private static LocalDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private List<ExchangeRate> parseAndSaveRates(String nbuApiResponse) {
        List<ExchangeRate> exchangeRates = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(nbuApiResponse);

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    String currencyTxt = node.get("txt").asText();
                    String currencyCode = node.get("cc").asText();
                    BigDecimal rateValue = new BigDecimal(node.get("rate").asText());

                    ExchangeRate exchangeRate = new ExchangeRate();
                    exchangeRate.setTargetCurrencyCode(currencyCode);
                    exchangeRate.setBaseCurrencyCode(currencyTxt);
                    exchangeRate.setRate(rateValue);
                    exchangeRate.setDateTime(LocalDateTime.parse(getCurrentDateTimeInISO8601Format()));
                    exchangeRates.add(exchangeRate);
                }
                exchangeRateRepository.saveAll(exchangeRates);
            } else {
                log.error("Incorrect structure of the NBU's response.");
            }
        } catch (IOException e) {
            log.error("IOException " + e.getMessage());
        }

        return exchangeRates;
    }

    private String getCurrentDateTimeInISO8601Format() {
        LocalDateTime currentDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return currentDateTime.format(formatter);
    }
}