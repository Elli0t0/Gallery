# Changelog

## [0.0.2]

- Selection mode: long-press to select, "N selected" title, select-all checkbox, Cancel, bottom pill with Create, Share, Delete, and More.
- More menu: Copy to album, Move to album, Add to favorites, Remove from favorites; Create copies the selection into a new album.
- Delete moves items to Trash (30 days, with days-left and Restore); trash auto-deletes items older than 30 days, and can be emptied or deleted permanently.
- Menu destinations are real now: Videos, Favorites, Recent (last 30 days), Locations (items with GPS coordinates), Trash, Settings.
- Search: live search by file name and album, opening results in the viewer.
- Settings: Auto play motion photos, Essential albums, and Merge albums toggles (persisted), plus Privacy Policy, Permissions, and About.
- Releases are full releases, not pre-releases, and the publish step skips if the release already exists.

## [0.0.1]

- Baseline: project setup, CI + signed release pipeline, media permissions flow.
- Pictures tab: pure-black theme, large collapsing title, date sections (Today, Yesterday, date), tight 4-column grid, video duration badges, pinch to change columns, fast-scroll bar with date bubble.
- Floating pill bottom nav (Pictures, Albums, Menu) that floats above content; lists have bottom padding so the last row is never covered.
- Albums tab: collapsing title, Essential albums with View all, 3-column rounded (28dp) album grid with name and count.
- Menu popup: Videos, Favorites, Recent, Locations, Trash, Settings (Phase 2 placeholders for now).
- Basic viewer: swipe between items, pinch zoom, video opens in the system player.
- Only real media albums are shown; junk, empty, and cache folders are hidden by default.
- Full RTL layout support with English and Persian strings.
