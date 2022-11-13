package com.dayone.scheduler;

import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.repository.CompanyRepository;
import com.dayone.persist.repository.DividendRepository;
import com.dayone.scraper.Scraper;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ScraperScheduler {

	private final CompanyRepository companyRepository;
	private final DividendRepository dividendRepository;
	private final Scraper yahooFinanceScraper;

	@CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
	@Scheduled(cron = "${scheduler.scrap.yahoo}")
	public void yahooFinanceScheduling() {
		log.info("scraping scheduler is started");
		List<CompanyEntity> companies = this.companyRepository.findAll();

		for (var company : companies) {

			log.info("scraping scheduler is started ->" + company.getName());
			ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(
				new Company(company.getTicker(),
					company.getName()));

			scrapedResult.getDividends().stream()
				.map(e -> new DividendEntity(company.getId(), e))
				.forEach(e -> {
					boolean exists = this.dividendRepository.existsByCompanyIdAndDate(
						e.getCompanyId(), e.getDate());
					if (!exists) {
						this.dividendRepository.save(e);
						log.info("insert new dividend ->" + e.toString());
					}
				});

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		}

	}

}
