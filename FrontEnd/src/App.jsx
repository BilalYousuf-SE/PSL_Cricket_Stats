import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import Dashboard from './pages/Dashboard.jsx';
import TeamsList from './pages/TeamsList.jsx';
import TeamDetail from './pages/TeamDetail.jsx';
import HeadToHead from './pages/HeadToHead.jsx';
import PlayersList from './pages/PlayersList.jsx';
import PlayerDetail from './pages/PlayerDetail.jsx';
import MatchesList from './pages/MatchesList.jsx';
import MatchDetail from './pages/MatchDetail.jsx';
import Records from './pages/Records.jsx';
import SeasonStats from './pages/SeasonStats.jsx';
import BattingInningsDetail from './pages/BattingInningsDetail.jsx';
import BowlingInningsDetail from './pages/BowlingInningsDetail.jsx';

export default function App() {
  return (
    <>
      <Navbar />
      <main className="app-shell">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/teams" element={<TeamsList />} />
          <Route path="/teams/:id" element={<TeamDetail />} />
          <Route path="/headtohead" element={<HeadToHead />} />
          <Route path="/players" element={<PlayersList />} />
          <Route path="/players/:id" element={<PlayerDetail />} />
          <Route path="/matches" element={<MatchesList />} />
          <Route path="/matches/:id" element={<MatchDetail />} />
          <Route path="/matches/:id/batting/:playerId" element={<BattingInningsDetail />} />
          <Route path="/matches/:id/bowling/:playerId" element={<BowlingInningsDetail />} />
          <Route path="/records" element={<Records />} />
          <Route path="/records/season/:year" element={<SeasonStats />} />
          <Route
            path="*"
            element={
              <div className="state-block">
                <p className="state-title">No such page in the scorebook</p>
                <p style={{ margin: 0 }}>That route doesn't exist. Use the nav above to get back on track.</p>
              </div>
            }
          />
        </Routes>
        <p className="footnote">CoverDrive · live data from your Spring Boot API · localhost:8080</p>
      </main>
    </>
  );
}
