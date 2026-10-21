package com.cricket.stats.controller;

import com.cricket.stats.dto.PartnershipDTO;
import com.cricket.stats.dto.PartnershipRequestDTO;
import com.cricket.stats.service.PartnershipService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PartnershipController {
    private final PartnershipService partnershipService;

    @GetMapping("/matches/{matchId}/partnerships")
    public List<PartnershipDTO> getForMatch(@PathVariable Integer matchId,
                                           @RequestParam(required = false) Integer innings) {
        return partnershipService.getForMatch(matchId, innings);
    }

    @GetMapping("/partnerships/{id}")
    public PartnershipDTO getById(@PathVariable Long id) {
        return partnershipService.getById(id);
    }

    @GetMapping("/partnerships")
    public Page<PartnershipDTO> search(@RequestParam(required = false) Integer matchId,
                                       @RequestParam(required = false) Integer innings,
                                       @RequestParam(required = false) Integer seasonYear,
                                       @RequestParam(required = false) Long playerId,
                                       @RequestParam(required = false) Long partnerId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        return partnershipService.search(matchId, innings, seasonYear, playerId, partnerId, page, size);
    }

    @PostMapping("/matches/{matchId}/partnerships")
    @ResponseStatus(HttpStatus.CREATED)
    public PartnershipDTO create(@PathVariable Integer matchId,
                                  @Valid @RequestBody PartnershipRequestDTO request) {
        return partnershipService.create(matchId, request);
    }

    @PutMapping("/matches/{matchId}/partnerships/{id}")
    public PartnershipDTO update(@PathVariable Integer matchId, @PathVariable Long id,
                                  @Valid @RequestBody PartnershipRequestDTO request) {
        return partnershipService.update(matchId, id, request);
    }

    @DeleteMapping("/matches/{matchId}/partnerships/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer matchId, @PathVariable Long id) {
        partnershipService.delete(matchId, id);
    }
}
