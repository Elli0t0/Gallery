# Changelog

## [0.1.4]

- RELEASE A, Pictures tab only: big header is now the first LazyColumn item and scrolls away; a compact black 56dp bar with centered title fades in after scrolling.
- Fast scroller indexes shift +1 for the header item; year pills are a vertical column next to the thumb while dragging, plus the date bubble, both hiding 1s after drag ends.
- Pinch now reacts only to 2+ fingers and never consumes single-finger drags; video duration badge moved to bottom-end.

## [0.1.3]

- Round 1 (Pictures tab) fidelity pass against the reference: header is now a large centered title with the search and overflow actions on a row below it (white icons, same Select/Refresh menu); the reference shot shows no collapsed state, so this header stays static on scroll instead of collapsing.
- Year pills no longer sit above the grid permanently: they appear only while dragging the fast scroller on the right edge, and the grid's reserved top padding for them is gone.
- 1970 group fixed: a DATE_TAKEN below 100_000_000_000 ms is now treated as invalid and falls back to DATE_MODIFIED, then DATE_ADDED, and the list is sorted by the corrected date.
- Bottom pill nav now matches the reference width (62% of the screen, capped at 340 dp) with a 12 dp bottom margin, and the Albums icon is a stacked-photos outline instead of a disc.
- Launcher icon rebuilt as an adaptive vector: dark background, white outlined rounded square, blue mountain, yellow sun, plus a monochrome layer for themed icons.

## [0.1.2]

- Round 8: long-press an album and choose Merge into… to move all its photos into another album (the case-insensitive merge toggle in Settings is unchanged).
- Motion photos now autoplay in the viewer when the setting is on: JPEGs with an embedded video play it looping and silent; tap to pause or resume.
- Speed: grid rows now have stable keys so list updates do not rebuild every row, and grid thumbnails are sized to the current column count instead of a fixed 360 px.
- RTL pass: drag-select now mirrors the column in right-to-left layouts; other bars already use start/end and mirror automatically.

## [0.1.1]

- Round 7: Secure Folder in the Menu — first open sets a PIN, later opens ask for fingerprint first with a PIN fallback. Selection menu gains Move to Secure Folder: files are AES-256/GCM encrypted into app-private storage and the original is deleted only after the encrypted copy verifies; inside you can view, restore to Pictures/Restored, or delete. Leaving the folder locks it again.
- Reference fidelity: Menu is now a bottom sheet like the reference, the selection bar uses icons (Create, Share, Delete, More), and selected photos show a white circle with a black check.

## [0.1.0]

- Round 6: Create in the Menu — Collage (2–4 photos into one picture), Slideshow (auto-play every 3 s, tap to pause), and GIF (2–10 photos into a looping GIF, saved to Pictures/Gallery).
- Tags screen in the Menu: every tag with its photo count, tap to view its photos, rename a tag, or delete it from all photos.
- Albums: + button creates a new album (pick photos, then Create in the selection bar, name prefilled); long-press an album to rename it (photos move to the new name) or delete it (photos go to Trash, restorable for 30 days).

## [0.0.10]

- Round 5: video time is now "00:03 / 00:09", mute is an icon button, and the player still starts instantly on the current page.
- Scrub filmstrip now spans the full width with a white playhead line tracking playback and a white border on the current frame; tap a frame to seek.
- Fullscreen button hides the action pill, filmstrip, and top buttons (tap the video to bring controls back); rotate-screen button is unchanged.

## [0.0.9]

- Round 4: Details sheet now shows megapixels alongside resolution (date/time, file name, path, size, resolution, MP, and tags were already there).
- Filmstrip now follows the pager: swiping the viewer scrolls the filmstrip so the current thumb stays visible.
- Copy to clipboard, Set as wallpaper, and Print were already in the viewer overflow menu and are unchanged.

## [0.0.8]

- Round 3: collapsing large headers are now on all screens (Pictures, Albums, Search, Trash, Locations, Settings).
- Albums: covers never show a bare broken tile — a photo icon sits behind every cover thumb, including video covers.
- Fast scroller keeps its date bubble and gains year pills (tap a year to jump to it) plus a scroll-to-top button that appears once scrolled down.
- Tap-and-drag multi-select: long-press, then drag across the grid to add items to the selection.
- Pinch to change grid columns is unchanged and still works in Pictures.
- Viewer polish: round translucent top buttons (back on the left, details on the right) and a rounded overflow menu.

## [0.0.7]

- Round 2: selection More menu now also has Add tag, Edit date and time, and Edit location, alongside Copy/Move to album and favorites; date/time and location are written back to MediaStore, with a toast when an update is not allowed.
- Search now normalizes Persian/Arabic text: Arabic yeh/kaf unified to Persian, alef variants unified, tashkeel, tatweel and ZWNJ/ZWJ stripped, and Persian/Arabic digits converted to ASCII, with all query tokens required to match.
- Favorites, trash (30 days), Recent, Videos, Locations, Menu popup, and Settings (DataStore) are already in the app and unchanged in this round.

## [0.0.6]

- Round 1 bugfixes: viewer swipe via HorizontalPager with item keys, pinch-zoom that only consumes gestures when zoomed, and back handling (viewer to grid at the same scroll position, selection back clears selection, other tabs back to Pictures, Pictures back exits).
- First launch: media loads only after permission is granted, reloads on resume, and a MediaStore ContentObserver refreshes the grid when files change; a loading spinner shows until the first query finishes.
- Video: one ExoPlayer per viewer, tap to show controls that auto-hide after 3 s, play/pause toggles playWhenReady, audio pauses on ON_STOP and the player releases when the viewer closes, and the bottom layout is one column (video controls, action pill, filmstrip) with navigation bar padding.
- Filmstrip thumbnails are real Coil 128 px thumbs, including video frames via coil-video, instead of black squares.
- No internal photo editor: the edit button now launches ACTION_EDIT with read/write URI flags (generic chooser, toast if no editor is installed); the editor app's package was not named in the plan, so no explicit package or package-specific query was added yet.
- New adaptive vector launcher icon: dark background with a photo/mountain glyph.

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
