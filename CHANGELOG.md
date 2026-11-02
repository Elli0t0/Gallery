# Changelog

## [0.0.5]

- Photo editor part 1, opened from the viewer pill's edit button: undo/redo, Revert, Save, and a menu with Save copy, Resize, Copy edits, Paste edits.
- Crop tab: aspect ratios (Original, 1:1, 4:3, 3:2, 16:9), rotate left/right, flip horizontal/vertical, straighten slider (-45 to +45 degrees).
- Filters tab: Original, Vivid, Warm, Cool, Mono, Fade, each with a live thumbnail preview.
- Adjust tab: Auto plus ruler sliders for light balance, brightness, exposure, contrast, highlights, shadows, saturation, tint, temperature, sharpness, vignette.
- Save writes back to the original when possible (otherwise saves a copy); Save copy always writes a new "_edited" image to Pictures/Gallery.

## [0.0.4]

- In-app video player (Media3): videos play inside the viewer, starting immediately on the current page, instead of opening an external player.
- Controls: play/pause, live time "current / total", mute/unmute, and a scrub slider.
- Scrub filmstrip: frame previews extracted from the video; tap a frame to jump to that moment.
- Rotate button switches portrait/landscape; the viewer is already fullscreen, and the player releases when you swipe away.

## [0.0.3]

- Viewer bottom pill: favorite, edit, share, delete (to trash), and a menu.
- Viewer menu: Details, Copy to clipboard, Set as wallpaper, Print.
- Filmstrip under the viewer pill; tap a thumb to jump to that item.
- Details sheet: date, file name, path, size, resolution, and tags with Add tag (tap a tag to remove it).
- Size work: R8 minify + shrink resources, arm64 only, en/fa locale filters, packaging excludes.
- Green auto-advance: a green report now starts the next phase without waiting for "next".

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
