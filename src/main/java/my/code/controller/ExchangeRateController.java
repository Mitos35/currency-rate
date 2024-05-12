package my.code.controller;

import lombok.RequiredArgsConstructor;
import my.code.database.entity.ExchangeRate;
import my.code.service.ExchangeRateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/current")
    public ResponseEntity<List<ExchangeRate>> getCurrentExchangeRates() {
        List<ExchangeRate> exchangeRates = exchangeRateService.getCurrentExchangeRates();
        return new ResponseEntity<>(exchangeRates, HttpStatus.OK);
    }

    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRate>> getExchangeRatesByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ExchangeRate> exchangeRates = exchangeRateService.getExchangeRatesByDate(date);
        return new ResponseEntity<>(exchangeRates, HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteExchangeRatesByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        exchangeRateService.deleteExchangeRatesByDate(date);
        return ResponseEntity.ok("Exchange rates for date " + date + " have been successfully deleted.");
    }
}
