// Shared motion toolkit used across the app so every page gets the same
// feel (and so the logic for it lives in exactly one place):
//   - AnimatedNumber   -> a stat that ticks up to its value like a scoreboard
//   - Reveal           -> fades a block up the first time it scrolls into view
//   - StickyHeader     -> the sliding condensed bar shown on detail pages
//   - TabRow           -> a tab strip with a sliding underline indicator
//   - useScrolledPast  -> tells a page when its hero has scrolled out of view
import { useEffect, useLayoutEffect, useRef, useState } from 'react';

// Counts a number up to its target whenever the target changes.
export function useCountUp(value, duration = 700) {
  const [display, setDisplay] = useState(0);
  const fromRef = useRef(0);

  useEffect(() => {
    const target = Number(value) || 0;
    const from = fromRef.current;
    if (from === target) {
      setDisplay(target);
      return undefined;
    }
    const start = performance.now();
    let raf;
    const tick = (now) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      setDisplay(Math.round(from + (target - from) * eased));
      if (t < 1) {
        raf = requestAnimationFrame(tick);
      } else {
        fromRef.current = target;
      }
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value, duration]);

  return display;
}

// A drop-in replacement for a raw number. Safe to use inside a .map() —
// it's a component, not a bare hook, so there's no "hooks in a loop" issue.
// Renders `fallback` (default an em dash) for null/undefined/non-numeric.
export function AnimatedNumber({ value, duration = 700, fallback = '—' }) {
  const numeric = Number(value);
  const isNumeric = value !== null && value !== undefined && value !== '' && Number.isFinite(numeric);
  const animated = useCountUp(isNumeric ? numeric : 0, duration);
  if (!isNumeric) return <>{fallback}</>;
  return <>{animated}</>;
}

// Fades an element up into place the first time it scrolls into view, then
// stops watching — it never re-triggers if you scroll back past it.
export function useRevealOnScroll(threshold = 0.15) {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.unobserve(el);
        }
      },
      { threshold }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [threshold]);

  return [ref, visible];
}

// Convenience wrapper for the above — wrap any block in <Reveal> and it
// will fade up the first time it's scrolled into view.
export function Reveal({ as: Tag = 'div', delay = 0, className = '', children, ...rest }) {
  const [ref, visible] = useRevealOnScroll();
  return (
    <Tag
      ref={ref}
      className={`reveal ${visible ? 'reveal-visible' : ''} ${className}`}
      style={{ transitionDelay: visible ? `${delay}ms` : '0ms' }}
      {...rest}
    >
      {children}
    </Tag>
  );
}

// Tracks whether a sentinel element (placed at the end of a page's hero)
// has scrolled past the top of the viewport — drives a condensed header.
export function useScrolledPast(ref, ready) {
  const [past, setPast] = useState(false);

  useEffect(() => {
    if (!ready) return undefined;
    const el = ref.current;
    if (!el) return undefined;
    const observer = new IntersectionObserver(
      ([entry]) => setPast(!entry.isIntersecting),
      { threshold: 0 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [ref, ready]);

  return past;
}

// Generic sliding-in fixed shell. Pages supply their own condensed content
// (team names, a key stat, a mini tab strip — whatever makes sense there).
export function StickyHeader({ visible, children }) {
  return (
    <div className={`condensed-bar ${visible ? 'visible' : ''}`} aria-hidden={!visible}>
      <div className="condensed-bar-inner">{children}</div>
    </div>
  );
}

// A tab row with a sliding underline indicator instead of an instant swap.
export function TabRow({ tabs, active, onChange, compact = false }) {
  const containerRef = useRef(null);
  const [indicator, setIndicator] = useState({ left: 0, width: 0 });

  useLayoutEffect(() => {
    const update = () => {
      const container = containerRef.current;
      if (!container) return;
      const btn = container.querySelector(`[data-tab="${CSS.escape(active)}"]`);
      if (btn) setIndicator({ left: btn.offsetLeft, width: btn.offsetWidth });
    };
    update();
    window.addEventListener('resize', update);
    return () => window.removeEventListener('resize', update);
  }, [active, compact]);

  return (
    <div className={`tab-row has-indicator ${compact ? 'tab-row-compact' : ''}`} ref={containerRef}>
      {tabs.map((tab) => (
        <button
          key={tab}
          type="button"
          data-tab={tab}
          className={`tab-btn ${active === tab ? 'active' : ''}`}
          onClick={() => onChange(tab)}
        >
          {tab}
        </button>
      ))}
      <span className="tab-indicator" style={{ left: indicator.left, width: indicator.width }} />
    </div>
  );
}
