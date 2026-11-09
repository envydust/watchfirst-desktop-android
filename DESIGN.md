---
name: WATCHFIRST (Android)
description: Members-only streaming app for phone and tablet, sharing the WATCHFIRST PC app's dark ground, neon-green signal and JetBrains Sans.
colors:
  signal-green: "#39FF14"
  signal-green-deep: "#20C987"
  signal-green-glow: "#7CFF64"
  on-signal: "#071108"
  focus-moss: "#153523"
  night-ground: "#080C10"
  slate-surface: "#111820"
  slate-card: "#17212B"
  border-default: "#2C3843"
  border-subtle: "rgba(255, 255, 255, 0.065)"
  text-primary: "#F5F7F8"
  text-secondary: "#B8BEC5"
  text-muted: "#969CA3"
  live-red: "#E5173F"
  favorite-gold: "#FFC107"
  error-rose: "#E36A8A"
typography:
  display:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "38sp"
    fontWeight: 700
    lineHeight: "42sp"
    letterSpacing: "-1.2sp"
  headline:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "26sp"
    fontWeight: 600
    lineHeight: "30sp"
    letterSpacing: "-0.8sp"
  search-title:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "22sp"
    lineHeight: "28.6sp"
  title:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "18sp"
    fontWeight: 600
    lineHeight: "24sp"
  title-medium:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "20sp"
  body:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "15sp"
    fontWeight: 400
    lineHeight: "22sp"
  body-medium:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  label:
    fontFamily: "JetBrains Sans, sans-serif"
    fontSize: "12sp"
    fontWeight: 600
    lineHeight: "14sp"
    letterSpacing: "0.8sp"
rounded:
  xs: "4dp"
  sm: "6dp"
  md: "8dp"
  tile: "10dp"
  lg: "12dp"
  xl: "16dp"
  xxl: "24dp"
  dialog: "28dp"
  full: "999dp"
spacing:
  s4: "4dp"
  s8: "8dp"
  s12: "12dp"
  s14: "14dp"
  s16: "16dp"
  s18: "18dp"
  s24: "24dp"
  s32: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.signal-green}"
    textColor: "{colors.on-signal}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.xl}"
    height: "54dp"
  button-hero:
    backgroundColor: "{colors.signal-green}"
    textColor: "{colors.on-signal}"
    typography: "{typography.title-medium}"
    padding: "0 28dp"
    height: "54dp"
  chip-filter:
    backgroundColor: "{colors.slate-card}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.full}"
    padding: "10dp 16dp"
    height: "40dp"
  chip-filter-selected:
    backgroundColor: "{colors.signal-green}"
    textColor: "{colors.on-signal}"
    rounded: "{rounded.full}"
    padding: "10dp 16dp"
    height: "40dp"
  chip-dropdown:
    backgroundColor: "{colors.slate-surface}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.lg}"
  card-poster:
    backgroundColor: "{colors.slate-surface}"
    rounded: "{rounded.lg}"
  card-landscape-tile:
    backgroundColor: "{colors.slate-surface}"
    rounded: "{rounded.tile}"
  card-channel:
    backgroundColor: "{colors.slate-card}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.lg}"
    padding: "14dp"
  badge-live:
    backgroundColor: "{colors.live-red}"
    textColor: "{colors.text-primary}"
    rounded: "5dp"
    padding: "3dp 7dp 3dp 5dp"
  badge-new:
    backgroundColor: "{colors.text-primary}"
    textColor: "{colors.night-ground}"
    rounded: "{rounded.xs}"
    padding: "2dp 6dp"
  input-search:
    textColor: "{colors.text-primary}"
    typography: "{typography.search-title}"
    height: "56dp"
  view-all-pill:
    textColor: "{colors.signal-green}"
    rounded: "{rounded.xl}"
    size: "40dp"
---

# Design System: WATCHFIRST (Android)

## Overview

**Creative North Star: "The Night Console"**

WATCHFIRST on Android is the same console as the WATCHFIRST PC (Windows) app, which is the reference for the shared look, sized down for a phone or tablet. A near-black ground with a cool blue tint sits under slate surfaces. Artwork carries the colour. The chrome speaks in a single neon-green signal: a short bar beside each row title, the line under a focused field, the selected chip, the primary button. Everything else stays quiet so the posters, channel logos and titles hold the member's attention.

Density is a streaming shelf. Rows of artwork run horizontally under green-barred titles, with 16 dp gutters and 12 dp gaps between cards. Every card has the same hairline frame. Depth comes from tonal steps and scrims over artwork, not from shadows. Type is one family, JetBrains Sans, from the bold page display down to the small semibold labels. The interface language is Brazilian Portuguese ("Busca", "TV ao vivo", "Em alta no WATCHFIRST", "Ver detalhes"), and new copy follows it.

Live TV is the one place a second accent appears. A red "AO VIVO" badge marks live content, and nothing else uses that red.

**Key Characteristics:**
- Dark, cool, tonal ground (#080C10 → #111820 → #17212B); no light theme ships for this world.
- One neon-green signal for focus, selection, section bars and primary actions.
- Artwork-first cards with a 1 dp white-6.5% hairline and bottom-up black scrims for legibility.
- JetBrains Sans for every role; weight (400/600/700), not a second family, builds the hierarchy.
- Live red only on the "AO VIVO" badge; favourite gold only on the favourite star.

## Colors

A night-blue ground with slate surfaces, lit by one neon green and, on live content only, one red.

### Primary
- **Signal Green** (`signal-green`): the theme's `accent` and Material `primary`. Use it for the 3 dp row-title bar, the focused search-field underline and cursor, the selected filter chip and view toggle, the primary and hero buttons ("Ver detalhes", "Assistir"), the "ver tudo" pill icon (on a 10% tint of itself), and progress spinners. Text and icons on it use **On-Signal** (`on-signal`), a near-black green.
- **Deep Signal Green** (`signal-green-deep`): `accentStrong` / Material `secondary`, and the end of the green accent gradient. It is the quieter companion to Signal Green, never a replacement for it.
- **Signal Glow** (`signal-green-glow`): the focus ring and focus border colour. **Focus Moss** (`focus-moss`) is the focus background and `primaryContainer`.

### Secondary
- **Live Red** (`live-red`): fill of the "AO VIVO" badge with its drawn bolt. Used nowhere else.

### Tertiary
- **Favourite Gold** (`favorite-gold`): the filled favourite star on channel cards and the star in the "Favoritos" chip. Used nowhere else.

### Neutral
- **Night Ground** (`night-ground`): screen background, nav chrome, and the text colour of the "Novidade" tag.
- **Slate Surface** (`slate-surface`): `surface`, sheets, dialogs, the dropdown chip, and the backing behind artwork that is still loading.
- **Slate Card** (`slate-card`): `surfaceCard`, used by filter and recent-search chips, the view toggle, and the channel-card fallback before a logo tint is read.
- **Hairline** (`border-subtle`, white at 6.5%): the 1 dp frame on every card, tile, chip and toggle.
- **Border Default** (`border-default`): stronger dividers where a hairline would disappear.
- **Text Primary / Secondary / Muted** (`text-primary`, `text-secondary`, `text-muted`): titles and values; subtitles, placeholders and summary lines; tertiary icons such as history and close glyphs in chips.
- **Error Rose** (`error-rose`): Material `error`, used for error states.

### Named Rules
**The One Signal Rule.** Green means "here, now, this one": focus, selection, the row bar, the primary action. Don't use it for decoration, for body text or as a large fill behind content.

**The Live Red Rule.** Live Red appears only as the fill of the "AO VIVO" badge. Live channel cards take their colour from the channel's own logo, never from the badge red.

**The Artwork Carries Colour Rule.** Card surfaces stay slate. Colour on a card comes from its artwork, or for channels without art, from a gradient tinted by the logo's dominant hue, darkening toward Night Ground.

## Typography

**Display Font:** JetBrains Sans (with platform sans-serif fallback)
**Body Font:** JetBrains Sans
**Label Font:** JetBrains Sans

**Character:** One technical humanist sans, the same as the PC app, used at three weights (Regular 400, SemiBold 600, Bold 700; only these three are bundled). Tight negative tracking on the large sizes and a little positive tracking on small labels give the console its voice.

### Hierarchy
- **Display** (700, 38 sp / 42 sp, −1.2 sp): page titles such as "TV ao vivo".
- **Headline** (600, 26 sp / 30 sp, −0.8 sp): large section and detail headings.
- **Search Title** (22 sp / ×1.3 line height): the search field when it is the page title (Busca). Placeholder in Text Secondary. The TV ao vivo field uses the same component at 18 sp.
- **Title** (600, 18 sp / 24 sp): row headers next to the green bar ("Em alta no WATCHFIRST", "Destaques ao vivo", "Descobrir", channel categories).
- **Title Medium** (600, 16 sp / 20 sp; Bold on buttons): button labels and action labels.
- **Body** (400, 15 sp / 22 sp): app body copy. **Body Medium** (400, 14 sp / 20 sp): titles under posters and chip text.
- **Label** (600, 12 sp / 14 sp, +0.8 sp): small labels and metadata. Card subtitles (category · addon) use 12 sp Regular.

Font scale is pinned to 1.0 by the theme, so these sizes render as written.

### Named Rules
**The One Family Rule.** Every role is JetBrains Sans. Build hierarchy with size and weight, never with a second display face or a system font.

**The Three Weights Rule.** Use only 400, 600 and 700. Medium (500) and Black (900) are not bundled and will not render as asked.

## Layout

Screens are a single vertical scroll of horizontal shelves. The screen gutter is 16 dp on phones (`screenHorizontal`). Rows are separated by about 32 dp (`sectionGap`). A row header sits 10–12 dp above its cards, and cards in a row are 12 dp apart (the home shelf default is 16 dp). Chips sit 8 dp apart.

- **Phone:** landscape (16:9) tiles are sized so that two cards and a peek of a third fit: (width − 32 − 12) / 2.15. Channel cards take 62% of the inner width, so one card shows with a strong peek of the next. The bottom navigation is a floating rounded bar.
- **Tablet (≥ 600 dp):** landscape tiles and channel cards are a fixed 264 dp. At ≥ 768 dp the navigation moves to a floating top bar, and the search page gets 72 dp of top space beneath it.
- **Breakpoints:** 420 / 600 / 840 / 1024 dp (large phone, tablet, large tablet, desktop). The home hero also changes height and alignment at 600, 840 and 1200 dp.
- Touch targets are at least 40–48 dp. Chips and the view toggle are 40 dp; the search field row is at least 56 dp.

## Elevation & Depth

The system is flat and tonal. Depth is the step from Night Ground to Slate Surface to Slate Card, plus a hairline frame and scrims over artwork. Text on artwork always sits on a bottom-up black scrim: transparent to about 42–58% of the card height, then black at 75–88%. Overlays use a black scrim at 56%. Elevation tokens exist (raised 2 dp, modal 8 dp, overlay 12 dp) for sheets and dialogs, but cards do not cast shadows.

### Named Rules
**The Scrim Not Shadow Rule.** Lift text off artwork with a vertical black gradient, never with a drop shadow or a text shadow.

**The Press Is Scale Rule.** A pressed card scales to 0.97. A focused or pressed live card swaps its hairline for a 2 dp white frame. There is no hover glow and no elevation change.

## Shapes

Corners are soft and consistent. Artwork is clipped to 10–12 dp: posters 12 dp by default (the member can change this in settings), landscape tiles 10 dp, channel cards 12 dp. Pills and chips are fully rounded (20 dp on 40 dp chips). Buttons use 14–16 dp, small tags 4–5 dp, sheets 24 dp, dialogs 28 dp, and the home hero has 28 dp bottom corners. Every artwork container has a 1 dp hairline. The section bar is a 3 × 20 dp rod with 2 dp corners.

## Components

### Buttons
Solid and confident: one green slab per screen region.
- **Shape:** gently rounded (16 dp `button` token; the hero uses 14 dp and the channel panel 12 dp).
- **Primary:** Signal Green fill, On-Signal Bold label, 54 dp tall, full width in forms.
- **Hero ("Ver detalhes"):** the same green slab, 54 dp tall with 28 dp side padding, as on the PC hero.
- **Disabled:** fill and label at 38% opacity.
- **Ver tudo pill:** a 40 dp (32 dp compact) rounded square, Signal Green at 10% with a green chevron. It ends each row header.

### Chips
- **Filter chip (TV ao vivo):** 40 dp pill on Slate Card with a hairline and 13 sp SemiBold text. When selected it fills with Signal Green, the text turns On-Signal and the border disappears. "Favoritos" carries a gold star.
- **Recent-search chip:** a Slate Card pill with a hairline, a muted history icon, 14 sp text and a 40 dp remove target.
- **Dropdown chip (Descobrir):** a Slate Surface block with 12 dp corners and a chevron.
- **View toggle:** a segmented Slate Card capsule (12 dp corners, 3 dp inset). The selected 40 dp segment is Signal Green with an On-Signal icon.

### Cards / Containers
- **Poster card:** 2:3 (0.675) artwork, 12 dp corners, hairline, title below in Body Medium. A bottom scrim appears only when a logo or text overlays the art.
- **Landscape tile (Em alta):** 16:9, 10 dp corners, hairline, bottom scrim (to black 82%) with the title's logo bottom-left. If there is no logo, the name is shown in 14 sp Bold white. A "Novidade" tag (Night Ground on Text Primary, 11 sp Bold, 4 dp corners) marks titles from the current year only.
- **Background:** Slate Surface behind the art while it loads. No shadow.

### Inputs / Fields
- **Search field as title:** no box. It is a search icon (26 dp at ≥ 20 sp, otherwise 22 dp), large text, and a full-width underline. At rest the underline is 1 dp white at 22% and the icon is Text Secondary.
- **Focus:** the underline animates to 2 dp Signal Green, the cursor is green, and the icon brightens to Text Primary. A clear button appears once there is text.

### Navigation
A floating rounded bar: at the bottom with icon and label on phones, at the top with icons only on tablets. The active tab sits in a green-tinted capsule with a green icon and label. Inactive tabs are muted. Tabs are Início, TV ao vivo (MAX), Busca, Biblioteca and Perfil. System Back is honoured.

### Row Header (signature)
A 3 × 20 dp Signal Green rod, 8 dp gap, a Title-style heading in Text Primary, and an optional "ver tudo" pill at the end. Every shelf on every surface opens with this header.

### Live Channel Card (signature)
- **Shape:** near-square (0.96), 12 dp corners, hairline. When focused or pressed it gets a 2 dp white frame and scales to 0.97.
- **Art:** the channel's own landscape art if one is sent. Otherwise a diagonal gradient tinted by the logo's dominant colour and darkening toward the ground, with the logo oversized (115% of width), rotated −18° at 16% opacity and bleeding off the top-right corner.
- **Content:** the "AO VIVO" badge top-left, an optional favourite star in a 32 dp black-45% circle top-right, then bottom-left the logo (42 dp high), the name (17 sp Bold white) and "category · addon" (12 sp, white 74%). No programme or progress data is shown.
- **Compact tile (Destaques ao vivo):** 16:9 with 10 dp corners, the compact badge bottom-left, no watermark, and the name and subtitle below the art.

### Live Badge
A Live Red pill with 5 dp corners, a drawn bolt icon (13–15 dp) and "AO VIVO" in 10–11 sp Bold white with +0.4 sp tracking. It appears on live cards and next to the TV ao vivo channel count.

## Do's and Don'ts

### Do:
- **Do** open every shelf with the green-rod row header and keep its title in Brazilian Portuguese.
- **Do** frame every artwork container with the 1 dp white-6.5% hairline and clip it to 10–12 dp.
- **Do** put text on artwork over a bottom-up black scrim (75–88% at the base).
- **Do** use Signal Green for focus, selection and the primary action, with On-Signal (#071108) for content on it.
- **Do** match the WATCHFIRST PC app when choosing between options: same palette, same JetBrains Sans, same green "Ver detalhes" hero button.
- **Do** keep touch targets at 40 dp or more; chips and toggles are 40 dp tall.

### Don't:
- **Don't** use Live Red for anything except the "AO VIVO" badge, and don't use Favourite Gold for anything except the favourite star.
- **Don't** draw programme titles, "Agora:" lines or progress bars on live cards; the live addons send no guide.
- **Don't** add drop shadows to cards or text; depth is tonal plus scrims.
- **Don't** introduce a second typeface or a font weight that is not bundled (500, 900).
- **Don't** box the search field when it serves as the page title; it is an underline field.
