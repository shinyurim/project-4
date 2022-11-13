package com.dayone.web;

import com.dayone.model.Company;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.service.CompanyService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/company")
@AllArgsConstructor
public class CompanyController {

	private final CompanyService companyService;
	private final CacheManager redisCacheManager;

	@GetMapping("/autocomplete")
	public ResponseEntity<?> autocomplete(@RequestParam String keyword) {
		var result = this.companyService.autocomplete(keyword);
		return ResponseEntity.ok(result);
	}

	@GetMapping
	@PreAuthorize("hasRole('READ')")
	public ResponseEntity<?> searchCompany(Pageable pageable) { // 회사조회
		Page<CompanyEntity> companies = this.companyService.getAllCompany(pageable);
		return ResponseEntity.ok(companies);
	}


	@PostMapping
	@PreAuthorize("hasRole('WRITE')")
	public ResponseEntity<?> addCompany(@RequestBody Company request) {
		String ticker = request.getTicker().trim();
		if (ObjectUtils.isEmpty(ticker)) {
			throw new RuntimeException("ticker is empty");
		}
		Company company = this.companyService.save(ticker);
		this.companyService.addAutocompleteKeyword(company.getName());
		return ResponseEntity.ok(company);
	} // 배당금저장

	@DeleteMapping("/{ticker}")
	@PreAuthorize("hasRole('WRITE')")
	public ResponseEntity<?> deleteCompany(@PathVariable String ticker) {
		String companyName = this.companyService.deleteCompany(ticker);
		this.clearFinanceCache(companyName);
		return ResponseEntity.ok(companyName);
	}

	public void clearFinanceCache(String companyName) {
		this.redisCacheManager.getCache(CacheKey.KEY_FINANCE).evict(companyName);

	}
}

