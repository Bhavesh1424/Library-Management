# Brag Video Plan — Library Management System (27-Second Edition)

## Project Overview
- **Repository**: [https://github.com/Bhavesh1424/Library-Management](https://github.com/Bhavesh1424/Library-Management)
- **Application Type**: Multi-Branch Inter-Campus Library Management & Courier Dispatch Engine
- **Target Duration**: 27.00 seconds (810 frames @ 30fps)
- **Resolution**: 1920 × 1080 (16:9 Landscape Full HD)

## Verified Technical Stack
- **Backend**: Spring Boot 3.2.0, Java 21, Spring Data JPA, Spring Security (Stateless JWT)
- **Concurrency & Transaction Safety**: JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` on copy reservations
- **Scheduling**: Automated `@Scheduled` cron for 48h hold expiration and overdue fine assessments
- **Database**: H2 In-Memory Database with multi-entity relational schema
- **Frontend**: Single-Page App with Tailwind CSS, Material Symbols, and Vanilla JavaScript
- **Personas**: Student (`Aarav Sharma`), Faculty (`Dr. Elena Vance`), Librarian (`Central`, `CS Turing Stacks`, `Franklin Mech`)

## 5-Scene Script Architecture (0–27s)
1. **Scene 1 (0.0s – 4.0s) | The Hook**:
   - Dark background with subtle ambient blue lighting and animated headline.
   - Text: *"A smarter way to manage libraries."*
   - Smooth zoom transition with cinematic sound effect.
2. **Scene 2 (4.0s – 9.0s) | Product Reveal**:
   - Reveal actual running application interface with slow camera push-in.
   - Text: *"Meet your Library Management System."*
   - Genuine application capture (`screen-reveal.png`).
3. **Scene 3 (9.0s – 16.0s) | Feature Showcase**:
   - Quick, synchronized cuts displaying actual system workflows:
     - 9.0s – 11.3s: **“Organize.”** — Unified multi-branch catalog, category filters, and branch-level inventory.
     - 11.3s – 13.6s: **“Track.”** — Real-time 4-stage courier transit tracker (`Pending Courier` ➔ `In Transit` ➔ `Arrived` ➔ `Pickup Ready`).
     - 13.6s – 16.0s: **“Simplify.”** — Librarian desk barcode scanner, instant loans, and 48-hour pickup hold management.
4. **Scene 4 (16.0s – 23.0s) | Behind the Build**:
   - Transition to genuine source code (`BranchCopyRepository.java`) and GitHub repository card.
   - Text: *"Built with code. Designed for efficiency."*
   - Highlight verified tech stack: Java 21, Spring Boot 3.2, Spring Security JWT, JPA Pessimistic Locking.
5. **Scene 5 (23.0s – 27.0s) | Closing**:
   - Clean dark background with subtle blue glow.
   - Title: *"Library Management System"*
   - Subtitle: *"Explore the project on GitHub"*
   - URL: `https://github.com/Bhavesh1424/Library-Management`
   - Smooth fade-out to black at 27.0s.

## Audio Choreography
- **Music Bed**: `happy-beats-business-moves-vol-12-by-ende-dot-app.mp3` (110 BPM upbeat electronic groove, loop-free, fade-in at 0s, fade-out at 26.2s–27.0s).
- **SFX**:
  - 3.6s: Cinematic impact/drop (`impactSoft_medium_001.ogg`)
  - 9.0s: Crisp UI click (`click_002.ogg`)
  - 11.3s: Crisp UI click (`click_002.ogg`)
  - 13.6s: Resonance chime (`bong_001.ogg`)
  - 16.0s: Transition drop (`drop_001.ogg`)
