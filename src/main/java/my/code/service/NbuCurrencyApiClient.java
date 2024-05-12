package my.code.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Setter
@Getter
@Slf4j
public class NbuCurrencyApiClient {

    @Value("${nbu.api.url}")
    private String nbuApiUrl;

    private final RestTemplate restTemplate;

    public String getLatestExchangeRates() {
        try {
            String response = restTemplate.getForObject(nbuApiUrl, String.class);
            log.info("Response from NBU API: {}", response);
            return response;
        } catch (RestClientException e) {
            log.error("Error while fetching data from NBU API: {}", e.getMessage());
            throw e;
        }
    }
}
