import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import MatchupFilters from '../components/MatchupFilters.jsx';
import { PlayerComparison, TeamComparison } from '../components/MatchupViews.jsx';
import { EMPTY_MATCHUP_FILTERS } from '../matchups.js';

export default function Matchups() {
  const [params] = useSearchParams();
  const [mode, setMode] = useState(params.get('mode') === 'teams' ? 'teams' : 'players');
  const [filters, setFilters] = useState({ ...EMPTY_MATCHUP_FILTERS });
  return (
    <div>
      <h1 className="page-title">Matchups</h1>
      <p className="page-subtitle">Explore batter–bowler encounters across every team they represented.</p>
      <div className="matchup-mode-controls" aria-label="Matchup view">
        <button type="button" className="matchup-button" aria-pressed={mode === 'players'} onClick={() => setMode('players')}>Player vs player</button>
        <button type="button" className="matchup-button" aria-pressed={mode === 'teams'} onClick={() => setMode('teams')}>Team matrix</button>
      </div>
      <MatchupFilters value={filters} onChange={setFilters} />
      {mode === 'players'
        ? <PlayerComparison filters={filters} initialBatterId={params.get('batterId') || ''} initialBowlerId={params.get('bowlerId') || ''} />
        : <TeamComparison filters={filters} initialBattingTeamId={params.get('battingTeamId') || ''} initialBowlingTeamId={params.get('bowlingTeamId') || ''} />}
    </div>
  );
}
