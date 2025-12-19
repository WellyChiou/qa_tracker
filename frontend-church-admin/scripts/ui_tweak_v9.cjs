const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const pencil = `<span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span>`;
const trash = `<span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span>`;

function walk(dir, exts) {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...walk(p, exts));
    else if (exts.some(e => entry.name.endsWith(e))) out.push(p);
  }
  return out;
}

function replaceAllInFile(file, replacers) {
  let s = fs.readFileSync(file, 'utf8');
  let changed = false;
  for (const { re, fn } of replacers) {
    const ns = s.replace(re, (...args) => fn(...args));
    if (ns !== s) {
      s = ns;
      changed = true;
    }
  }
  if (changed) fs.writeFileSync(file, s, 'utf8');
  return changed;
}

const vueFiles = walk(path.join(ROOT, 'src'), ['.vue']);

const replacers = [
  // Add icon + wrap label for edit
  {
    re: /<button([^>]*?)class=\"([^\"]*\bbtn-edit\b[^\"]*)\"([^>]*)>\s*編輯\s*<\/button>/g,
    fn: (m, a, cls, b) => `<button${a}class="${cls}"${b}>${pencil}<span>編輯</span></button>`
  },
  // delete
  {
    re: /<button([^>]*?)class=\"([^\"]*\bbtn-delete\b[^\"]*)\"([^>]*)>\s*刪除\s*<\/button>/g,
    fn: (m, a, cls, b) => `<button${a}class="${cls}"${b}>${trash}<span>刪除</span></button>`
  },
  // Wrap actions cell content in table-actions when it contains btn-edit/btn-delete and isn't already wrapped
  {
    re: /<td>\s*((?:<button[^>]*\bbtn\b[^>]*>.*?<\/button>\s*){2,})\s*<\/td>/gs,
    fn: (m, inner) => {
      if (inner.includes('btn-edit') || inner.includes('btn-delete')) {
        if (inner.includes('table-actions')) return m;
        return `<td><div class="table-actions">${inner.trim()}</div></td>`;
      }
      return m;
    }
  }
];

let n = 0;
for (const f of vueFiles) {
  if (replaceAllInFile(f, replacers)) n++;
}

console.log(`Updated ${n} Vue files`);
