---
version: 1
slug: "nuvio-app-features-search-searchscreen-kt-238cd1b4"
primary_target: "composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt"
related_targets: ["composeApp/src/commonMain/kotlin/com/nuvio/app/features/livetv/LiveTvScreen.kt"]
---

# Busca e TV ao vivo (mobile)

Scope: the Search tab and the "TV ao vivo" tab of the WATCHFIRST Android app. Mode: Operate.
Audience and job: members on a phone who want to find a title or a channel and start it in a few taps.
Content: titles from the plan's catalogs (trending = home hero + first catalog rows), recent searches on this
device, live channels (MAX only: name, image, genre/category, addon). No programme guide exists: nothing says
"Agora:" and no progress bar is drawn. Requested reference: Disney+ mobile search and its "Canais Ao Vivo" row.
Constraints: established WATCHFIRST world (PC palette, JetBrains Sans, green accent bar row titles); Material
navigation and system Back; 48 dp targets.

## Direction contract

THESIS: Search opens as a place to browse, not an empty form: the field is the page title, and below it the
plan's own trending titles and live channels are already playing their artwork. Refuses the boxed field over a
bare poster grid.

OWN-WORLD: WATCHFIRST dark ground #080C10, surfaces #111820/#17212B, hairline white 6.5% borders, green
#39FF14 for focus line, row bars and selection; live red #E5173F only on the "⚡ AO VIVO" badge. Landscape 16:9
artwork cards with 10 dp corners; channel cards are near-square (Disney+'s own ~0.96 proportion), tinted by the
channel's own logo colour with an oversized, rotated logo watermark (the Disney ESPN treatment) and a white 2 dp
frame when focused or pressed.

STORY: the member sees what is hot in their plan and, on MAX, what is live, taps a card and lands on details
(titles) or straight in the player (channels; long-press opens the channel details); typing switches the same
screen to results without leaving it.

FIRST VIEWPORT: status bar, then an underlined search field at 22 sp with a 26 dp search glyph, full width;
recent-search chips when focused; "Em alta no WATCHFIRST" row of 16:9 cards (two and a peek on a phone) with a
"Novidade" tag on this year's titles; then "Destaques ao vivo" (MAX) with badge-on-art cards; Descobrir below.
Live tab: title, live summary line, the same field, filter chips, then one horizontal row of channel cards
per category.

FORM: user-pinned reference (Disney+ search and live channel rows) translated into the WATCHFIRST world. The
surface roll was run for the record; the owner's pinned direction beats the dealt structures ("a user- or
brief-pinned decision beats the roll, always").
seed:191e4395ab1a495e

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
