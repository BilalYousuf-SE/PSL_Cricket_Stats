package com.cricket.stats.service;

import com.cricket.stats.ID.MatchSquadId;
import com.cricket.stats.dto.PartnershipRequestDTO;
import com.cricket.stats.entity.*;
import com.cricket.stats.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnershipServiceTest {
    @Mock PartnershipRepository partnerships;
    @Mock MatchRepository matches;
    @Mock PlayerRepository players;
    @Mock MatchSquadRepository squads;
    @InjectMocks PartnershipService service;

    Match match;
    Player first, second;
    MatchSquad firstSquad, secondSquad;
    PartnershipRequestDTO request;

    @BeforeEach
    void setup() {
        Team team = new Team();
        team.setTeamId(1);
        Team opposition = new Team();
        opposition.setTeamId(2);
        Season season = new Season();
        season.setYear(2026);
        match = new Match();
        match.setMatchId(10);
        match.setTeamA(team);
        match.setTeamB(opposition);
        match.setSeason(season);
        match.setMatchDate(LocalDate.of(2026, 3, 1));
        match.setVenue("Lahore");
        first = player(1L, "Batter A");
        second = player(2L, "Batter B");
        firstSquad = squad(first, team);
        secondSquad = squad(second, team);
        request = new PartnershipRequestDTO();
        request.setInnings(2);
        request.setBatter1Id(1L);
        request.setBatter2Id(2L);
        request.setRuns(45);
        request.setBallsFaced(30);
        request.setWicketNumber(1);
    }

    @Test
    void createsPartnershipWithExplicitInningsAndReadableMatchAndBatterDetails() {
        allowWrite();
        when(partnerships.save(any())).thenAnswer(invocation -> {
            Partnership saved = invocation.getArgument(0);
            saved.setPartnershipId(50L);
            return saved;
        });
        var dto = service.create(10, request);
        assertThat(dto.getPartnershipId()).isEqualTo(50L);
        assertThat(dto.getMatchId()).isEqualTo(10);
        assertThat(dto.getInnings()).isEqualTo(2);
        assertThat(dto.getRuns()).isEqualTo(45);
        assertThat(dto.getBallsFaced()).isEqualTo(30);
        assertThat(dto.getWicketNumber()).isEqualTo(1);
        assertThat(dto.getBatter1Name()).isEqualTo("Batter A");
        assertThat(dto.getBatter2Name()).isEqualTo("Batter B");
        assertThat(dto.getSeasonYear()).isEqualTo(2026);
        assertThat(dto.getMatchDate()).isEqualTo(match.getMatchDate());
        assertThat(dto.getVenue()).isEqualTo("Lahore");
    }

    @Test
    void rejectsTheSameBatterTwice() {
        when(matches.findForUpdate(10)).thenReturn(Optional.of(match));
        request.setBatter2Id(1L);
        assertThatThrownBy(() -> service.create(10, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(players, squads, partnerships);
    }

    @Test
    void rejectsBattersFromOpposingTeams() {
        allowWrite();
        secondSquad.setTeam(match.getTeamB());
        assertThatThrownBy(() -> service.create(10, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("same team");
        verify(partnerships, never()).save(any());
    }

    @Test
    void rejectsNonPlayingSquadMembers() {
        allowWrite();
        firstSquad.setIsPlaying(false);
        assertThatThrownBy(() -> service.create(10, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verify(partnerships, never()).save(any());
    }

    @Test
    void rejectsBattersOutsideTheMatchSquad() {
        when(matches.findForUpdate(10)).thenReturn(Optional.of(match));
        when(players.findById(1L)).thenReturn(Optional.of(first));
        when(players.findById(2L)).thenReturn(Optional.of(second));
        assertThatThrownBy(() -> service.create(10, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("not in this match's squad");
        verify(partnerships, never()).save(any());
    }

    @Test
    void missingMatchOrBatterReturnsNotFound() {
        assertThatThrownBy(() -> service.create(10, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        when(matches.findForUpdate(10)).thenReturn(Optional.of(match));
        assertThatThrownBy(() -> service.create(10, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("First batter not found");
        verify(partnerships, never()).save(any());
    }

    @Test
    void updatesAnExistingRecordWithoutChangingItsIdentityOrMatch() {
        allowWrite();
        Partnership existing = partnership();
        when(partnerships.findById(50L)).thenReturn(Optional.of(existing));
        when(partnerships.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        request.setRuns(60);
        request.setInnings(1);
        var updated = service.update(10, 50L, request);
        assertThat(updated.getPartnershipId()).isEqualTo(50L);
        assertThat(updated.getMatchId()).isEqualTo(10);
        assertThat(updated.getRuns()).isEqualTo(60);
        assertThat(updated.getInnings()).isEqualTo(1);
    }

    @Test
    void cannotUpdateOrDeleteAnotherMatchesPartnership() {
        Match other = new Match();
        other.setMatchId(11);
        when(matches.findForUpdate(11)).thenReturn(Optional.of(other));
        when(partnerships.findById(50L)).thenReturn(Optional.of(partnership()));
        assertThatThrownBy(() -> service.update(11, 50L, request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        assertThatThrownBy(() -> service.delete(11, 50L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(partnerships, never()).save(any());
        verify(partnerships, never()).delete(any());
    }

    @Test
    void deletesOnlyTheRequestedRecord() {
        Partnership existing = partnership();
        when(matches.findForUpdate(10)).thenReturn(Optional.of(match));
        when(partnerships.findById(50L)).thenReturn(Optional.of(existing));
        service.delete(10, 50L);
        verify(partnerships).delete(existing);
    }

    @Test
    void distinguishesAnEmptyMatchFromAMissingMatchAndPartnership() {
        assertThatThrownBy(() -> service.getForMatch(10, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        when(matches.existsById(10)).thenReturn(true);
        assertThat(service.getForMatch(10, null)).isEmpty();
        assertThatThrownBy(() -> service.getById(50L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }

    @Test
    void filtersMatchRetrievalByInningsAndPreservesLegacyUnknownInnings() {
        when(matches.existsById(10)).thenReturn(true);
        Partnership legacy = partnership();
        legacy.setInnings(null);
        when(partnerships.findForMatch(10, null)).thenReturn(List.of(legacy));
        assertThat(service.getForMatch(10, null).get(0).getInnings()).isNull();
        service.getForMatch(10, 2);
        verify(partnerships).findForMatch(10, 2);
        assertThatThrownBy(() -> service.getForMatch(10, 0))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void passesCombinedFiltersAndPaginationToTheRepository() {
        var pageable = PageRequest.of(1, 20);
        when(partnerships.search(10, 2, 2026, 1L, 2L, pageable))
                .thenReturn(new PageImpl<>(List.of(partnership()), pageable, 21));
        var result = service.search(10, 2, 2026, 1L, 2L, 1, 20);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBatter2Id()).isEqualTo(2L);
    }

    @Test
    void rejectsIncompletePairsInvalidFiltersAndUnboundedPages() {
        assertThatThrownBy(() -> service.search(null, null, null, null, 2L, 0, 50))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(null, null, null, 1L, 1L, 0, 50))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(null, 0, null, null, null, 0, 50))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(null, null, null, 0L, null, 0, 50))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(null, null, null, null, null, -1, 50))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.search(null, null, null, null, null, 0, 201))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(partnerships);
    }

    private void allowWrite() {
        when(matches.findForUpdate(10)).thenReturn(Optional.of(match));
        when(players.findById(1L)).thenReturn(Optional.of(first));
        when(players.findById(2L)).thenReturn(Optional.of(second));
        when(squads.findById(any(MatchSquadId.class))).thenAnswer(invocation -> {
            MatchSquadId id = invocation.getArgument(0);
            return Optional.of(id.getPlayer().equals(1L) ? firstSquad : secondSquad);
        });
    }

    private Partnership partnership() {
        Partnership partnership = new Partnership();
        partnership.setPartnershipId(50L);
        partnership.setMatch(match);
        partnership.setInnings(2);
        partnership.setBatter1(first);
        partnership.setBatter2(second);
        return partnership;
    }

    private Player player(Long id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setFullName(name);
        return player;
    }

    private MatchSquad squad(Player player, Team team) {
        MatchSquad squad = new MatchSquad();
        squad.setMatch(match);
        squad.setPlayer(player);
        squad.setTeam(team);
        return squad;
    }
}
