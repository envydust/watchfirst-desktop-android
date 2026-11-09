# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Brazilian WATCHFIRST members on the Básico and MAX plans, watching on their phones (and some Android tablets).
They are already members: the job is to find something to watch quickly and start it, not to evaluate the service.

## Product Purpose
WATCHFIRST is a members-only streaming app. A member enters with their name, e-mail and the single-use code
generated on their plan's member site; the app then loads only the addons (catalogs and sources) released for
that plan. Success: the member finds a film, series or channel in a few taps and playback starts.

## Positioning
Access is tied to the member and the plan: content comes from addons the WATCHFIRST administrator manages per
plan, not from sources the member installs. MAX adds live TV channels, kept in their own "TV ao vivo" tab.

## Operating Context
- Login screen: name + e-mail, then the code from the member site. Members open the site on their own; the app
  has no shortcut to it.
- Tabs: Início, TV ao vivo (MAX only), Busca, Biblioteca, Perfil/Configurações.
- Live addons are marked "ao vivo" by the administrator; their channels never appear on the home screen.
- The phone app follows the WATCHFIRST Windows (PC) app's look and settings structure.

## Capabilities and Constraints
- Kotlin/Compose Multiplatform (Android shipped; iOS target exists). Built and published by GitHub Actions.
- Live addons send channel catalogs (name, logo/poster, genre, description) but no programme guide: there is no
  real "now playing" programme or progress. Live cards show channel, category and addon only.
- Search "popular" rows come from trending titles in the member's catalogs (with a "Novidade" badge for recent
  releases); the app has no data on other people's searches.
- Live highlights appear in Search for MAX members only.
- No cloud library on mobile; no member-installed addons (content discovery and integrations are hidden).

## Brand Commitments
- Name WATCHFIRST; logo is the W with a play mark plus the "WATCH" (white) "FIRST" (green) wordmark.
- Interface in Brazilian Portuguese by default; the PC app is the reference for look and structure.

## Evidence on Hand
- Logos and icons in `composeApp/src/commonMain/composeResources/drawable/`.
- No testimonials, ratings or usage data; do not fabricate any.

## Product Principles
1. Find and play fast: every screen should lead to playback in few taps.
2. Show only what the plan really includes; never invent programme data or content.
3. Same WATCHFIRST experience on phone and PC.
4. Keep the member's attention on the artwork and titles, not on chrome.
