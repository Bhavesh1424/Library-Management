---
name: Athenaeum Quietude
colors:
  surface: '#fbf9f5'
  surface-dim: '#dbdad6'
  surface-bright: '#fbf9f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ef'
  surface-container: '#efeeea'
  surface-container-high: '#eae8e4'
  surface-container-highest: '#e4e2de'
  on-surface: '#1b1c1a'
  on-surface-variant: '#434843'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f0ed'
  outline: '#747873'
  outline-variant: '#c4c8c1'
  surface-tint: '#566157'
  primary: '#4d594e'
  on-primary: '#ffffff'
  primary-container: '#657166'
  on-primary-container: '#e8f5e7'
  inverse-primary: '#bdcabd'
  secondary: '#31666f'
  on-secondary: '#ffffff'
  secondary-container: '#b4e8f4'
  on-secondary-container: '#366a74'
  tertiary: '#704d40'
  on-tertiary: '#ffffff'
  tertiary-container: '#8b6557'
  on-tertiary-container: '#ffeee8'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d9e6d8'
  primary-fixed-dim: '#bdcabd'
  on-primary-fixed: '#131e16'
  on-primary-fixed-variant: '#3e4a40'
  secondary-fixed: '#b7ebf7'
  secondary-fixed-dim: '#9bcfda'
  on-secondary-fixed: '#001f25'
  on-secondary-fixed-variant: '#144e57'
  tertiary-fixed: '#ffdbce'
  tertiary-fixed-dim: '#ebbcab'
  on-tertiary-fixed: '#2e150b'
  on-tertiary-fixed-variant: '#603f32'
  background: '#fbf9f5'
  on-background: '#1b1c1a'
  surface-variant: '#e4e2de'
typography:
  display-lg:
    fontFamily: Literata
    fontSize: 48px
    fontWeight: '400'
    lineHeight: 58px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Literata
    fontSize: 34px
    fontWeight: '400'
    lineHeight: 42px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Literata
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Literata
    fontSize: 26px
    fontWeight: '500'
    lineHeight: 34px
  headline-md:
    fontFamily: Literata
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  headline-sm:
    fontFamily: Literata
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 3rem
  margin-mobile: 1.25rem
  space-xs: 0.375rem
  space-sm: 0.75rem
  space-md: 1.25rem
  space-lg: 2rem
  space-xl: 3.5rem
---

## Brand & Style

This design system establishes a quiet, tranquil digital reading room for higher education libraries. Tailored for undergraduate researchers, graduate scholars, and faculty seeking mental clarity amid rigorous academic demands, the interface functions as an antidote to information fatigue. 

The aesthetic is **Warm Minimalist Editorial**, marrying the physical tactile charm of archival paper and linen bindings with the effortless clarity of contemporary digital navigation. Generous margins, soft natural light tones, rounded architectural pill shapes, and quiet micro-interactions induce focus and a relaxed cognitive rhythm. It balances institutional authority with pastoral calm, transforming library search, physical shelf discovery, and campus courier delivery into a rejuvenating experience.

## Colors

The color palette directly references the soothing, grounded mineral tones of historical reading halls and botanical bookplates:

- **Primary (`#657166` - Muted Slate Forest):** Provides grounding contrast, legibility, and refined structure for primary headlines, high-priority buttons, and critical status markers.
- **Secondary (`#99CDD8` - Soft Sky Blue):** Adds gentle clarity, signaling interaction points, links, active category tabs, and real-time delivery status tracking.
- **Tertiary (`#F3C3B2` - Gentle Blush Terracotta):** Introduces warmth for reservation alerts, favorite bookmarks, pickup notices, and delicate focal points.
- **Background & Canvas:**
  - Base canvas: `#FDFBF7` (Warm Pearl Vellum)
  - Card & sheet surfaces: `#DAEBE3` (Airy Powder Sage) and `#FDE8D3` (Delicate Cream)
  - Subtle architectural tints: `#CFD6C4` (Sage Mist) for quiet dividers, pill track backings, and inactive indicators.
- **Text & Contrast:** Primary text rests at deep forest slate `#2D352E`, while secondary annotations adopt `#657166` to maintain soft, human-scale contrast without harsh pitch black.

## Typography

The type system pairs **Literata**—a bookish, refined variable serif developed for long-form reading—with **Plus Jakarta Sans**, a clean, warmly proportioned geometric sans-serif that retains legibility across density-rich search filters and metadata pills.

- **Literata (Headlines & Editorial Titles):** Imbues book titles, collection curations, and author highlights with classical collegiate prestige without stiffness.
- **Plus Jakarta Sans (Body, Labels, & Data):** Manages call numbers, availability indicators, reservation forms, and campus drop-off locations cleanly.
- Leading is deliberately relaxed throughout body copy to preserve the unhurried atmosphere of an open-stack library reading alcove.

## Layout & Spacing

A 12-column responsive grid on desktop transitions to 8 columns on tablet and 4 columns on mobile. The system prioritizes breathing room: wide outer gutters frame search results like the balanced margins of an illuminated folio.

- **Desktop (1200px+):** Max-width reading container capped at 1320px to prevent visual sprawl, supported by a 3rem outer margin and 1.5rem gutters.
- **Tablet (768px - 1199px):** 2rem margins with 1.25rem gutters. Split catalog cards fold from 3 columns down to 2 columns.
- **Mobile (under 768px):** 1.25rem margin, 1rem gutter. Quick actions and category chips collapse into horizontal smooth-scrolling pill ribbons with edge fades.
- Generous component gaps (`space-lg` to `space-xl`) between catalog shelves and circulation queues keep complex academic information uncluttered.

## Elevation & Depth

This system avoids heavy drop shadows, instead using **tonal layering and ambient soft feathering**:

- **Surface Layering:** Depth is conveyed by floating `#FFFFFF` and `#FDFBF7` cards over tinted beds of `#DAEBE3` (Airy Powder Sage) and `#CFD6C4` (Sage Mist).
- **Soft Ambient Light:** When elevation is required for modals, hover sheets, or delivery track cards, use an ultra-diffused shadow tinted with forest slate:
  - Resting elevation: `0 2px 12px -2px rgba(101, 113, 102, 0.06)`
  - Active hover: `0 8px 24px -4px rgba(101, 113, 102, 0.10)`
- **Subtle Borders:** Micro-borders of 1px solid `rgba(101, 113, 102, 0.12)` provide clean edges without drawing harsh boundaries.

## Shapes

Directly drawn from the horizontal stacked pill visual language, the shape framework utilizes smooth full-pill geometry (`roundedness: 3`) for all interactive elements, tags, search queries, and status chips. 

Cards, collection containers, and sheet panels adopt `rounded-lg` (2rem) and `rounded-xl` (3rem), echoing comfortable reading stones and soft furniture ergonomics. The silhouette is gentle, welcoming, and entirely devoid of harsh 90-degree corners.

## Components

- **Buttons:**
  - *Primary ("Request Delivery"):* Filled in `#657166` (Slate Forest) with `#FDFBF7` text, 9999px pill radius, padded with `1rem 2rem`. Subtle lift on hover.
  - *Secondary ("Hold Book"):* Filled in `#99CDD8` (Soft Sky Blue) with `#2D352E` text.
  - *Tertiary / Ghost ("Read Abstract"):* Transparent pill with a 1px border of `rgba(101, 113, 102, 0.25)` and text in `#657166`.
- **Chips & Filter Pills:**
  - Full-pill geometry (`border-radius: 9999px`), styled with pastel base fills (`#DAEBE3`, `#FDE8D3`, or `#CFD6C4`).
  - Active states swap to `#657166` text with `#99CDD8` accent dots.
- **Search & Input Fields:**
  - Floating pill-form search bar with a `#FFFFFF` core, framed by a soft `#CFD6C4` rim. 
  - Generous internal padding (`0.875rem 1.75rem`), with placeholder text set in muted slate and accompanied by quiet glass search iconography.
- **Book & Material Cards:**
  - Built with generous 2rem corner radii on a `#FFFFFF` substrate.
  - Book covers sit within gentle recessed wells tinted with `#FDE8D3` (Delicate Cream).
  - Metadata is arranged hierarchically: Title in Literata 20px, author and shelf code in Plus Jakarta Sans 13px, anchored by a delivery pill chip.
- **Delivery Timeline & Status Modules:**
  - Horizontal pill-segmented progress trackers indicating: *Requested* → *Retrieved from Stacks* → *In Transit to Campus Locker* → *Ready for Collection*.
  - Uses `#99CDD8` for active transit and `#F3C3B2` for final locker collection alerts.
- **Checkboxes & Radios:**
  - Softly rounded checks and pill toggle sliders tinted in `#99CDD8` when selected, resting on a `#DAEBE3` track.