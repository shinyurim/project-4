package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistCompanyException;
import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.repository.CompanyRepository;
import com.dayone.persist.repository.DividendRepository;
import com.dayone.scraper.Scraper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@AllArgsConstructor
public class CompanyService {

	private final Trie trie;
	private final Scraper yahooFinanceScraper;
	private final CompanyRepository companyRepository;
	private final DividendRepository dividendRepository;

	public Company save(String ticker) {
		boolean exists = this.companyRepository.existsByTicker(ticker);
		if (exists) {
			throw new AlreadyExistCompanyException();
		}
		return this.storeCompanyAndDividend(ticker);
	}

	public Page<CompanyEntity> getAllCompany(Pageable pageable) { // 모든 회사목록 반환
		return this.companyRepository.findAll(pageable);
	}

	private Company storeCompanyAndDividend(String ticker) {
		Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
		if (ObjectUtils.isEmpty(company)) {
			throw new RuntimeException("failed to scrap ticker -> " + ticker);
		}

		ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

		CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));

		List<DividendEntity> dividendEntityList =
			scrapedResult.getDividends().stream()
				.map(e -> new DividendEntity(companyEntity.getId(), e))
				.collect(Collectors.toList());

		this.dividendRepository.saveAll(dividendEntityList);
		return company;
	}

	public void addAutocompleteKeyword(String keyword) {
		this.trie.put(keyword, null);
	}

	public List<String> autocomplete(String keyword) {
		return (List<String>) this.trie.prefixMap(keyword).keySet() // prefix로 입력받음
			.stream()
			.limit(10) // 회사명list 중 10개 반환
			.collect(Collectors.toList());
	}

	public void deleteAutocompleteKeyword(String keyword) {
		this.trie.remove(keyword);
	}

	public String deleteCompany(String ticker) { // 배당금 정보, 캐시 삭제
		var company = this.companyRepository.findByTicker(ticker)
			.orElseThrow(() -> new NoCompanyException());

		this.dividendRepository.deleteAllByCompanyId(company.getId());
		this.companyRepository.delete(company);

		this.deleteAutocompleteKeyword(company.getName());
		return company.getName();
	}
}
