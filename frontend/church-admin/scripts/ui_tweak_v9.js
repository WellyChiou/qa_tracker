const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const pencil = `<span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span>`;
const trash = `<span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg></span>`;
const chevron = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>`;

function walk(dir, filelist = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, ent.name);
    if (ent.isDirectory()) walk(full, filelist);
    else filelist.push(full);
  }
  return filelist;
}

function updateFile(fp, transform) {
  const before = fs.readFileSync(fp, 'utf8');
  const after = transform(before);
  if (after !== before) {
    fs.writeFileSync(fp, after, 'utf8');
    return true;
  }
  return false;
}

let changed = 0;

// 1) Add icons + table-actions wrapper for edit/delete buttons in admin views
const adminViews = path.join(ROOT, 'src', 'views', 'admin');
for (const fp of walk(adminViews).filter(f => f.endsWith('.vue'))) {
  const did = updateFile(fp, (s) => {
    let out = s;

    // wrap the action buttons cell content to align nicely
    out = out.replace(
      /<td>\s*\n\s*(<button[^>]*class=\"btn btn-edit\"[\s\S]*?<\/button>\s*\n\s*<button[^>]*class=\"btn btn-delete\"[\s\S]*?<\/button>)\s*\n\s*<\/td>/g,
      (m, inner) => `<td><div class="table-actions">\n${inner}\n</div></td>`
    );

    // Add icon to edit button (only if it doesn't already contain btn__icon)
    out = out.replace(
      /<button([^>]*class=\"btn btn-edit\"[^>]*)>(\s*)編輯(\s*)<\/button>/g,
      (m, attrs) => `<button${attrs}>${pencil}<span>編輯</span></button>`
    );

    // Add icon to delete button
    out = out.replace(
      /<button([^>]*class=\"btn btn-delete\"[^>]*)>(\s*)刪除(\s*)<\/button>/g,
      (m, attrs) => `<button${attrs}>${trash}<span>刪除</span></button>`
    );

    return out;
  });
  if (did) changed++;
}

// 2) Make filter sections collapsible (details/summary) without touching logic
const filterFiles = [
  'AboutInfo.vue','Activities.vue','ChurchInfo.vue','ContactSubmissions.vue','Menus.vue','Permissions.vue','Persons.vue','Positions.vue','Roles.vue','ServiceSchedule.vue','SundayMessages.vue','UrlPermissions.vue','Users.vue'
].map(f => path.join(adminViews, f));

for (const fp of filterFiles) {
  if (!fs.existsSync(fp)) continue;
  const did = updateFile(fp, (s) => {
    // Only transform if it still uses <section class="filters"> (not already details)
    if (!s.includes('<section class="filters">')) return s;

    // Convert
    // <section class="filters">
    //   <h3>查詢條件</h3>
    //   <div class="filter-grid">...
    // </section>
    // to
    // <details class="filters filters--collapsible" open>
    //   <summary>...</summary>
    //   <div class="filters__content"> ...original content without h3... </div>
    // </details>

    let out = s;

    out = out.replace(/<section class=\"filters\">\s*\n\s*<h3>([^<]*)<\/h3>/g, (m, title) => {
      const t = title.trim() || '查詢條件';
      return `<details class="filters filters--collapsible" open>\n  <summary>\n    <div class="filters__title">\n      <h3>${t}</h3>\n      <span class="filters__badge">點擊收合 / 展開</span>\n    </div>\n    <span class="filters__chev" aria-hidden="true">${chevron}</span>\n  </summary>\n  <div class="filters__content">`;
    });

    out = out.replace(/\n\s*<\/section>/g, (m) => {
      // close the last filters section only (avoid converting other sections)
      // We'll be conservative: only when it belongs to filters block
      return m;
    });

    // Close the details block: replace the first occurrence of </section> that closes filters
    // by looking for the filters__content opening just inserted.
    if (out.includes('filters__content')) {
      // Find from filters__content to the next </section>
      const idx = out.indexOf('filters__content');
      const closeIdx = out.indexOf('</section>', idx);
      if (closeIdx !== -1) {
        out = out.slice(0, closeIdx) + `  </div>\n</details>` + out.slice(closeIdx + '</section>'.length);
      }
    }

    return out;
  });
  if (did) changed++;
}

// 3) Add skeleton blocks in the loading rows that already exist (no logic changes)
const scheduledJobs = path.join(adminViews, 'ScheduledJobs.vue');
if (fs.existsSync(scheduledJobs)) {
  const did = updateFile(scheduledJobs, (s) => {
    // Replace simple loading row with skeleton row if present
    return s.replace(/<tr v-if=\"loading\">\s*<td[^>]*>([\s\S]*?)<\/td>\s*<\/tr>/m, (m, inner) => {
      return `<tr v-if="loading" class="skeleton-row">\n  <td colspan="6">\n    <div style="display:grid; gap:10px">\n      <div class="skeleton" style="height:14px; width:42%"></div>\n      <div class="skeleton" style="height:14px; width:76%"></div>\n    </div>\n  </td>\n</tr>`;
    });
  });
  if (did) changed++;
}

const contactSub = path.join(adminViews, 'ContactSubmissions.vue');
if (fs.existsSync(contactSub)) {
  const did = updateFile(contactSub, (s) => {
    return s.replace(/<tr v-if=\"loading\">\s*<td[^>]*>([\s\S]*?)<\/td>\s*<\/tr>/m, () => {
      return `<tr v-if="loading" class="skeleton-row">\n  <td colspan="6">\n    <div style="display:grid; gap:10px">\n      <div class="skeleton" style="height:14px; width:46%"></div>\n      <div class="skeleton" style="height:14px; width:82%"></div>\n    </div>\n  </td>\n</tr>`;
    });
  });
  if (did) changed++;
}

console.log(`ui_tweak_v9: changed files count ~= ${changed}`);
