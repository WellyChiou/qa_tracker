# UI Guideline (for this project)

## Layout
- Use `.container` for page width, and `.section` / `.section--tight` for vertical rhythm.
- Prefer `grid grid-2` / `grid grid-3` for content lists.

## Typography
- Page title: `.h1`
- Section title: `.h2`
- Card title: `.h3` + `.card__title`
- Body: default; secondary text: `.muted`

## Components
- Card: `.card` (add `.card--hover` for lists)
- Media: `.media` wrapper with fixed height for images
- Primary action: `.btn btn-primary`
- Secondary action: `.btn btn-ghost`
- Tags: `.tags` + `.tag`
- Badges: `.badge` (use `badge--accent` for date/notice)

## Color
- Primary: `--primary` / `--primary-2`
- Accent: `--accent`

## Rule of thumb
- When adding a new block, start from a card + grid pattern; keep spacing consistent instead of page-specific CSS.
