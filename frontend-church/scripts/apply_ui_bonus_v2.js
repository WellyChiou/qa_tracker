/**
 * UI Bonus Patcher (frontend-church-ui-refresh-v2 base)
 * - page-bg fixed background (avoid footer seam)
 * - router-view transition (no router/composables/API changes)
 * - v-reveal scroll reveal directive
 * - Home.vue common broken section / duplicate attribute fix
 *
 * Safe to run multiple times. Creates *.bak backups the first time.
 */
const fs = require("fs");
const path = require("path");

const root = process.cwd();

function exists(p) { return fs.existsSync(p); }
function read(p) { return fs.readFileSync(p, "utf8"); }
function write(p, s) { fs.writeFileSync(p, s, "utf8"); }
function backup(p) {
  const bak = p + ".bak";
  if (!exists(bak) && exists(p)) write(bak, read(p));
}

function walkFind(dir, filename, depth = 0, maxDepth = 6) {
  if (depth > maxDepth) return null;
  let items = [];
  try { items = fs.readdirSync(dir, { withFileTypes: true }); } catch { return null; }
  for (const it of items) {
    if (it.name === "node_modules" || it.name === ".git" || it.name === "dist") continue;
    const p = path.join(dir, it.name);
    if (it.isFile() && it.name === filename) return p;
  }
  for (const it of items) {
    if (!it.isDirectory()) continue;
    if (it.name === "node_modules" || it.name === ".git" || it.name === "dist") continue;
    const found = walkFind(path.join(dir, it.name), filename, depth + 1, maxDepth);
    if (found) return found;
  }
  return null;
}

function ensureAppendCss(cssPath, marker, cssBlock) {
  if (!exists(cssPath)) return false;
  let s = read(cssPath);
  if (s.includes(marker)) return true;
  backup(cssPath);
  s = s.trimEnd() + "\n\n" + cssBlock.trim() + "\n";
  write(cssPath, s);
  return true;
}

function patchAppVue() {
  const app = walkFind(path.join(root, "src"), "App.vue");
  if (!app) { console.log("⚠️ App.vue not found, skip"); return; }
  backup(app);
  let s = read(app);

  // page-bg node: insert right under #app
  if (!s.includes('class="page-bg"')) {
    s = s.replace(/(<div\s+id="app"\s*>)/, '$1\n    <div class="page-bg" aria-hidden="true"></div>');
    console.log("✅ App.vue: added page-bg node");
  } else {
    console.log("ℹ️ App.vue: page-bg already present");
  }

  // router-view transition wrap
  if (!s.includes('transition name="page"')) {
    s = s.replace(/<router-view\s*\/>/i, `
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>`.trim());
    console.log("✅ App.vue: wrapped router-view with transition");
  } else {
    console.log("ℹ️ App.vue: transition already present");
  }

  write(app, s);
}

function patchVReveal() {
  const directivesDir = path.join(root, "src", "directives");
  if (!exists(directivesDir)) fs.mkdirSync(directivesDir, { recursive: true });

  const revealFile = path.join(directivesDir, "reveal.js");
  if (!exists(revealFile)) {
    write(revealFile, `
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
`.trim() + "\n");
    console.log("✅ created: src/directives/reveal.js");
  } else {
    console.log("ℹ️ reveal.js already exists");
  }

  const main = exists(path.join(root, "src", "main.js")) ? path.join(root, "src", "main.js")
            : exists(path.join(root, "src", "main.ts")) ? path.join(root, "src", "main.ts")
            : null;
  if (!main) { console.log("⚠️ main entry not found, skip register"); return; }

  backup(main);
  let s = read(main);
  if (!s.includes("directives/reveal")) {
    s = "import reveal from './directives/reveal'\n" + s;
  }

  // Normalize to const app style to register directive safely
  if (s.match(/createApp\(App\)\.use\(router\)\.mount/)) {
    s = s.replace(
      /createApp\(App\)\.use\(router\)\.mount\((['"])#app\1\)/,
      "const app = createApp(App)\napp.use(router)\napp.directive('reveal', reveal)\napp.mount('#app')"
    );
  } else if (!s.includes("app.directive('reveal'") && !s.includes('app.directive("reveal"')) {
    s = s.replace(/(app\.use\(router\)\s*)/, "$1app.directive('reveal', reveal)\n");
  }

  write(main, s);
  console.log("✅ registered: v-reveal");
}

function patchHomeVue() {
  const home = walkFind(path.join(root, "src"), "Home.vue");
  if (!home) { console.log("⚠️ Home.vue not found, skip"); return; }
  backup(home);
  let s = read(home);

  // Fix the known broken line variants
  s = s.replace(
    /<section class="section section--tight"\s+v-if="upcomingActivities\s*&&\s*upcomingActivities\.length\s+v-reveal>\s*0"\s*>/g,
    '<section class="section section--tight" v-if="upcomingActivities && upcomingActivities.length > 0" v-reveal>'
  );

  // Add v-reveal to the 聚會時間 section (first match)
  s = s.replace(
    /(<section class="section")(?![^>]*v-reveal)(\s+v-if="churchInfo"[^>]*>)/,
    "$1 v-reveal$2"
  );

  // Add v-reveal to 最新活動 section
  s = s.replace(
    /(<section class="section section--tight")(?![^>]*v-reveal)(\s+v-if="upcomingActivities[^>]*>)/,
    "$1 v-reveal$2"
  );

  // Conservative duplicate attribute normalization
  s = s.replace(/(\s)class="[^"]*"\s+class="/g, '$1class="');
  s = s.replace(/(\s)v-if="[^"]*"\s+v-if="/g, '$1v-if="');

  write(home, s);
  console.log("✅ patched: Home.vue");
}

function patchGlobalCss() {
  const cssCandidates = [
    path.join(root, "src", "style.css"),
    path.join(root, "src", "assets", "main.css"),
    path.join(root, "src", "assets", "styles.css"),
  ];
  const css = cssCandidates.find(exists);
  if (!css) { console.log("⚠️ global css not found, skip"); return; }

  ensureAppendCss(css, "UI bonus: fixed page background", `
/* --- UI bonus: fixed page background (avoid footer seam) --- */
.page-bg{
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    radial-gradient(1200px 600px at 20% 0%, rgba(31, 157, 106, 0.10), transparent 60%),
    radial-gradient(900px 500px at 90% 10%, rgba(244, 180, 0, 0.08), transparent 55%),
    linear-gradient(180deg, rgba(15,23,42,.02), rgba(15,23,42,.00) 40%);
}
`);

  ensureAppendCss(css, "UI bonus: router page transition", `
/* --- UI bonus: router page transition --- */
.page-enter-active, .page-leave-active { transition: opacity .18s ease, transform .18s ease; }
.page-enter-from { opacity: 0; transform: translateY(6px); }
.page-leave-to   { opacity: 0; transform: translateY(-4px); }
`);

  ensureAppendCss(css, "UI bonus: v-reveal", `
/* --- UI bonus: v-reveal scroll reveal --- */
.reveal{ opacity: 0; transform: translateY(10px); transition: opacity .35s ease, transform .35s ease; will-change: opacity, transform; }
.reveal.is-revealed{ opacity: 1; transform: translateY(0); }
@media (prefers-reduced-motion: reduce){
  .reveal, .reveal.is-revealed{ transition: none; transform: none; opacity: 1; }
}
`);
  console.log("✅ patched: global css");
}

function main() {
  if (!exists(path.join(root, "package.json"))) {
    console.error("❌ Please run in project root (package.json).");
    process.exit(1);
  }
  console.log("=== UI BONUS PATCH (v2 base) ===");
  patchAppVue();
  patchVReveal();
  patchHomeVue();
  patchGlobalCss();
  console.log("✅ done");
}

main();
