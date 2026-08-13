package com.disabilityclaim.web;

import com.disabilityclaim.domain.entity.UserAccount;
import com.disabilityclaim.repository.UserAccountRepository;
import com.disabilityclaim.service.BeneficiaryService;
import com.disabilityclaim.web.dto.BeneficiaryDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserAccountRepository userAccountRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR','VIEWER')")
    public List<BeneficiaryDtos.BeneficiaryResponse> list() {
        return beneficiaryService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR','VIEWER')")
    public BeneficiaryDtos.BeneficiaryResponse get(@PathVariable UUID id) {
        return beneficiaryService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public BeneficiaryDtos.BeneficiaryResponse create(
            @Valid @RequestBody BeneficiaryDtos.BeneficiaryRequest request,
            Authentication authentication) {
        UserAccount actor = currentUser(authentication);
        return beneficiaryService.create(request, actor.getId(), actor.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public BeneficiaryDtos.BeneficiaryResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody BeneficiaryDtos.BeneficiaryRequest request,
            Authentication authentication) {
        UserAccount actor = currentUser(authentication);
        return beneficiaryService.update(id, request, actor.getId(), actor.getUsername());
    }

    @GetMapping("/{id}/certificates")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR','VIEWER')")
    public List<BeneficiaryDtos.CertificateResponse> certificates(@PathVariable UUID id) {
        return beneficiaryService.listCertificates(id);
    }

    @PostMapping("/{id}/certificates")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public BeneficiaryDtos.CertificateResponse addCertificate(
            @PathVariable UUID id,
            @Valid @RequestBody BeneficiaryDtos.CertificateRequest request,
            Authentication authentication) {
        UserAccount actor = currentUser(authentication);
        return beneficiaryService.addCertificate(id, request, actor.getId(), actor.getUsername());
    }

    private UserAccount currentUser(Authentication authentication) {
        return userAccountRepository.findByUsername(authentication.getName()).orElseThrow();
    }
}
