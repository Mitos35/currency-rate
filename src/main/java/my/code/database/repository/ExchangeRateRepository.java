package my.code.database.repository;

import my.code.database.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    @Query("SELECT e FROM ExchangeRate e WHERE e.dateTime = (SELECT MAX(dateTime) FROM ExchangeRate)")
    List<ExchangeRate> findLatestRates();

    List<ExchangeRate> findByDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    void deleteByDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
}