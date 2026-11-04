package com.cricket.stats.service;

import com.cricket.stats.ID.MatchSquadId;
import com.cricket.stats.dto.PartnershipDTO;
import com.cricket.stats.dto.PartnershipRequestDTO;
import com.cricket.stats.entity.Match;
import com.cricket.stats.entity.MatchSquad;
import com.cricket.stats.entity.Partnership;
import com.cricket.stats.mapper.PartnershipMapper;
import com.cricket.stats.repository.MatchRepository;
import com.cricket.stats.repository.MatchSquadRepository;
import com.cricket.stats.repository.PartnershipRepository;
import com.cricket.stats.repository.PlayerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnershipService {
    private final PartnershipRepository partnershipRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchSquadRepository squadRepository;

    public PartnershipDTO getById(Long id) {
        return PartnershipMapper.toDTO(find(id));
    }

    public List<PartnershipDTO> getForMatch(Integer matchId, Integer innings) {
        requirePositive(innings, "innings");
        if (!matchRepository.existsById(matchId)) {
            throw new ResponseStatusException(NOT_FOUND, "Match not found");
        }
        return partnershipRepository.findForMatch(matchId, innings).stream()
                .map(PartnershipMapper::toDTO).toList();
    }

    public Page<PartnershipDTO> search(Integer matchId, Integer innings, Integer seasonYear,
                                       Long playerId, Long partnerId, int page, int size) {
        requirePositive(matchId, "matchId");
        requirePositive(innings, "innings");
        requirePositive(seasonYear, "seasonYear");
        requirePositive(playerId, "playerId");
        requirePositive(partnerId, "partnerId");
        if (partnerId != null && (playerId == null || partnerId.equals(playerId))) {
            throw new ResponseStatusException(BAD_REQUEST, "partnerId requires a different playerId");
        }
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be non-negative and size between 1 and 200");
        }
        return partnershipRepository.search(matchId, innings, seasonYear, playerId, partnerId,
                PageRequest.of(page, size)).map(PartnershipMapper::toDTO);
    }

    @Transactional
    public PartnershipDTO create(Integer matchId, PartnershipRequestDTO request) {
        Match match = lockMatch(matchId);
        Partnership partnership = new Partnership();
        partnership.setMatch(match);
        apply(partnership, request);
        return PartnershipMapper.toDTO(partnershipRepository.save(partnership));
    }

    @Transactional
    public PartnershipDTO update(Integer matchId, Long id, PartnershipRequestDTO request) {
        lockMatch(matchId);
        Partnership partnership = findForMatch(matchId, id);
        apply(partnership, request);
        return PartnershipMapper.toDTO(partnershipRepository.save(partnership));
    }

    @Transactional
    public void delete(Integer matchId, Long id) {
        lockMatch(matchId);
        partnershipRepository.delete(findForMatch(matchId, id));
    }

    private Partnership find(Long id) {
        return partnershipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Partnership not found"));
    }

    private Partnership findForMatch(Integer matchId, Long id) {
        Partnership partnership = find(id);
        if (!partnership.getMatch().getMatchId().equals(matchId)) {
            throw new ResponseStatusException(NOT_FOUND, "Partnership not found in this match");
        }
        return partnership;
    }

    private Match lockMatch(Integer matchId) {
        return matchRepository.findForUpdate(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Match not found"));
    }

    private void apply(Partnership partnership, PartnershipRequestDTO request) {
        if (request.getBatter1Id().equals(request.getBatter2Id())) {
            throw new ResponseStatusException(BAD_REQUEST, "A partnership requires two different batters");
        }
        var batter1 = playerRepository.findById(request.getBatter1Id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "First batter not found"));
        var batter2 = playerRepository.findById(request.getBatter2Id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Second batter not found"));
        Match match = partnership.getMatch();
        MatchSquad first = squadMember(match.getMatchId(), batter1.getId());
        MatchSquad second = squadMember(match.getMatchId(), batter2.getId());
        Integer teamId = first.getTeam().getTeamId();
        if (!teamId.equals(second.getTeam().getTeamId())
                || (!teamId.equals(match.getTeamA().getTeamId()) && !teamId.equals(match.getTeamB().getTeamId()))
                || Boolean.FALSE.equals(first.getIsPlaying()) || Boolean.FALSE.equals(second.getIsPlaying())) {
            throw new ResponseStatusException(BAD_REQUEST, "Both batters must play for the same team in this match");
        }
        partnership.setBatter1(batter1);
        partnership.setBatter2(batter2);
        partnership.setInnings(request.getInnings());
        partnership.setRuns(request.getRuns());
        partnership.setBallsFaced(request.getBallsFaced());
        partnership.setWicketNumber(request.getWicketNumber());
    }

    private MatchSquad squadMember(Integer matchId, Long playerId) {
        MatchSquadId id = new MatchSquadId();
        id.setMatch(matchId);
        id.setPlayer(playerId);
        return squadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Batter is not in this match's squad"));
    }

    private void requirePositive(Number value, String field) {
        if (value != null && value.longValue() < 1) {
            throw new ResponseStatusException(BAD_REQUEST, field + " must be positive");
        }
    }
}
