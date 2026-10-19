import { Routes, Route, useLocation } from 'react-router-dom';
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
import InningsDetail from './pages/InningsDetail.jsx';
import Admin from './pages/Admin.jsx';


export default function App() {
  const { pathname } = useLocation();

  // Admin is a standalone page: no public navbar / shell
  if (pathname.startsWith('/admin')) {
    return (
      <Routes>
        <Route path="/admin" element={<Admin />} />
        <Route path="/admin/matches/:matchId" element={<Admin />} />
      </Routes>
    );
  }

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
          <Route path="/matches/:id/batting/:playerId" element={<InningsDetail type="batting" />} />
          <Route path="/matches/:id/bowling/:playerId" element={<InningsDetail type="bowling" />} />
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