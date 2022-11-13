package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.repository.CompanyRepository;
import com.dayone.persist.repository.DividendRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

	private final CompanyRepository companyRepository;
	private final DividendRepository dividendRepository;


	@Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
	public ScrapedResult getDividendByCompanyName(String companyName) {

		log.info("search company -> " + companyName);
		CompanyEntity company = this.companyRepository.findByName(companyName)
			.orElseThrow(() -> new NoCompanyException()); // 잘못된 회사명이 입력으로 들어온경우

		List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(
			company.getId());

		List<Dividend> dividends = dividendEntities.stream()
			.map(e -> new Dividend(e.getDate(), e.getDividend()))
			.collect(Collectors.toList());

		return new ScrapedResult(new Company(company.getTicker(), company.getName())
			, dividends);

	}

}
