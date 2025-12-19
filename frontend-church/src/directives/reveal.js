/**
 * v-reveal: scroll reveal via IntersectionObserver
 *
 * Goals:
 * - Simple: <section class="section" v-reveal>...</section>
 * - Advanced:
 *   - stagger / delay (via binding or data-reveal / data-reveal-delay / data-reveal-stagger)
 *   - mobile (<=768px): disable ALL reveal + route transitions (CSS uses .reveal-init gate)
 *   - section auto apply: section reveals + children auto stagger
 *
 * Data attributes:
 * - data-reveal="off"        => disable reveal for this element
 * - data-reveal="page"       => immediate reveal (no scroll wait) + low-key stagger
 * - data-reveal-delay="120"  => ms
 * - data-reveal-stagger="80" => ms
 */
const MOBILE_MAX = 768;

function isMobile() {
  if (typeof window === 'undefined') return false;
  return window.matchMedia && window.matchMedia(`(max-width:${MOBILE_MAX}px)`).matches;
}

function toInt(v, fallback) {
  const n = parseInt(v, 10);
  return Number.isFinite(n) ? n : fallback;
}

function applyStagger(el, opts) {
  // Children auto stagger: direct children first, fallback to common blocks
  const selector =
    opts.childrenSelector ||
    el.getAttribute('data-reveal-children') ||
    ':scope > *';

  const children = Array.from(el.querySelectorAll(selector))
    .filter((c) => c && c.nodeType === 1)
    .filter((c) => !c.hasAttribute('data-reveal') || c.getAttribute('data-reveal') !== 'off')
    .filter((c) => !c.classList.contains('no-reveal'));

  const baseDelay = toInt(opts.delay ?? el.getAttribute('data-reveal-delay'), 0);
  const stagger = toInt(opts.stagger ?? el.getAttribute('data-reveal-stagger'), 80);

  children.forEach((child, i) => {
    // mark as reveal-item so CSS can target transition-delay via var
    child.classList.add('reveal-item');
    child.style.setProperty('--reveal-delay', `${baseDelay + i * stagger}ms`);
  });
}

function setupObserver(el, opts) {
  const cls = opts.class || 'is-revealed';
  const once = opts.once !== false;

  const threshold = opts.threshold ?? 0.1;
  const rootMargin = opts.rootMargin ?? '0px 0px -12% 0px';

  const io = new IntersectionObserver(
    (entries) => {
      for (const e of entries) {
        if (e.isIntersecting) {
          el.classList.add(cls);
          if (opts.staggerChildren !== false) applyStagger(el, opts);
          if (once) io.unobserve(el);
        }
      }
    },
    { threshold, rootMargin }
  );

  io.observe(el);
  el.__reveal_io__ = io;
}

export default {
  mounted(el, binding) {
    const opts = binding?.value || {};

    // Mobile: completely disable reveal (keep elements visible)
    if (isMobile()) {
      el.classList.add('reveal-disabled');
      return;
    }

    const mode = opts.mode || el.getAttribute('data-reveal') || 'auto';
    if (mode === 'off') return;

    // Gate CSS so we don't accidentally hide content on mobile
    document.documentElement.classList.add('reveal-init');

    // Section auto: any ".section" becomes a container reveal with child stagger
    const isSection = el.classList.contains('section') || mode === 'section';

    el.classList.add('reveal');

    // Page reveal: immediate, no scroll wait (used for About narrative page)
    if (mode === 'page' || opts.immediate) {
      // low-key stagger by default
      const immediateOpts = { ...opts };
      if (immediateOpts.stagger === undefined) immediateOpts.stagger = 60;
      requestAnimationFrame(() => {
        el.classList.add(opts.class || 'is-revealed');
        if (immediateOpts.staggerChildren !== false) applyStagger(el, immediateOpts);
      });
      return;
    }

    // Minimal: reveal container only, no child staggering unless requested
    if (mode === 'minimal') {
      setupObserver(el, { ...opts, staggerChildren: false, threshold: opts.threshold ?? 0.16 });
      return;
    }

    // Default: observe on scroll
    const defaultOpts = { ...opts };
    if (isSection) {
      // Section: reveal itself + auto stagger children
      if (defaultOpts.stagger === undefined) defaultOpts.stagger = 70;
      if (defaultOpts.rootMargin === undefined) defaultOpts.rootMargin = '0px 0px -14% 0px';
    }
    setupObserver(el, defaultOpts);
  },
  unmounted(el) {
    const io = el.__reveal_io__;
    if (io) io.disconnect();
    delete el.__reveal_io__;
  },
};
