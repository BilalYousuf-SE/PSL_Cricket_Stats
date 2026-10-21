import { useLayoutEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useApi } from '../useApi.js';
import { pick, toArray } from '../fields.js';
import '../motion.css';

const LINKS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/teams', label: 'Teams' },
  { to: '/headtohead', label: 'Head-to-Head' },
  { to: '/matchups', label: 'Matchups' },
  { to: '/players', label: 'Players' },
  { to: '/matches', label: 'Matches' },
  { to: '/records', label: 'Records' },
];

// "Season" isn't a fixed route (the real page is /records/season/:year), so
// this jumps to the most recent season on record. The dropdown to switch
// seasons lives only on that page (SeasonStats.jsx) — not here.
function SeasonNavLink() {
  const { data: seasons } = useApi('/season');
  const seasonList = toArray(seasons);
  const latestYear = seasonList.length > 0
    ? Math.max(...seasonList.map((s) => Number(pick(s, ['year'], 0)) || 0))
    : null;

  if (!latestYear) {
    return <span className="nav-link" style={{ opacity: 0.35, cursor: 'default' }}>Season</span>;
  }

  return (
    <NavLink
      to={`/records/season/${latestYear}`}
      className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
    >
      Season
    </NavLink>
  );
}

// Slides a thin gold underline beneath whichever nav link is currently
// active, instead of the highlight just snapping into place on navigation.
function NavIndicator({ containerRef }) {
  const location = useLocation();
  const [style, setStyle] = useState({ left: 0, width: 0, opacity: 0 });

  useLayoutEffect(() => {
    const update = () => {
      const container = containerRef.current;
      if (!container) return;
      const activeEl = container.querySelector('.nav-link.active');
      if (activeEl) {
        setStyle({ left: activeEl.offsetLeft, width: activeEl.offsetWidth, opacity: 1 });
      } else {
        setStyle((s) => ({ ...s, opacity: 0 }));
      }
    };
    // A rAF gives the async SeasonNavLink a moment to resolve its own
    // active state before the indicator measures anything.
    const raf = requestAnimationFrame(update);
    window.addEventListener('resize', update);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', update);
    };
  }, [location.pathname, containerRef]);

  return <span className="nav-indicator" style={style} />;
}

export default function Navbar() {
  const navLinksRef = useRef(null);

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <NavLink to="/" className="brand">
          CoverDrive<span>.</span>
        </NavLink>
        <nav className="nav-links" ref={navLinksRef}>
          {LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                'nav-link' + (isActive ? ' active' : '')
              }
            >
              {link.label}
            </NavLink>
          ))}
          <SeasonNavLink />
          <NavIndicator containerRef={navLinksRef} />
        </nav>
      </div>
    </header>
  );
}
