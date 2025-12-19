/**
 * v-reveal: scroll reveal via IntersectionObserver
 * Usage: <div v-reveal>...</div>
 * Options: v-reveal="{ once: true, class: 'is-revealed', threshold: 0.12, rootMargin: '0px 0px -10% 0px' }"
 */
export default {
  mounted(el, binding) {
    const opts = binding?.value || {};
    const once = opts.once !== false;
    const cls = opts.class || "is-revealed";

    el.classList.add("reveal");

    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting) {
            el.classList.add(cls);
            if (once) io.unobserve(el);
          }
        }
      },
      {
        threshold: opts.threshold ?? 0.12,
        rootMargin: opts.rootMargin ?? "0px 0px -10% 0px",
      }
    );

    io.observe(el);
    el.__reveal_io__ = io;
  },
  unmounted(el) {
    const io = el.__reveal_io__;
    if (io) io.disconnect();
    delete el.__reveal_io__;
  },
};
