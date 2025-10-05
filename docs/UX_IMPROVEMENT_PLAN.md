# UX Improvement Implementation Plan

## Overview
This document outlines the plan to fix 4 major UX issues identified by the user.

---

## Issue 1: Map Auto-Zoom & Non-Functional FABs 🗺️

### Problem
- Map keeps auto-zooming back to current location after panning
- "My Location" FAB doesn't work
- "Compass" FAB doesn't do anything

### Root Cause
**File**: `MapScreen.kt` (lines 217-224)
```kotlin
LaunchedEffect(state.currentLocation) {
    state.currentLocation?.let { location ->
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(location, 15f),
            durationMs = 1000
        )
    }
}
```
This `LaunchedEffect` triggers **every time** `state.currentLocation` changes (which happens continuously with GPS updates), causing the map to snap back.

### Solution
1. **Remove auto-follow behavior** - Only animate to location on explicit user request
2. **Track user interaction** - Add `var hasUserInteracted by remember { mutableStateOf(false) }`
3. **Implement proper My Location FAB** - Actually center camera on click
4. **Implement Compass FAB** - Reset map bearing to north + zoom to user location

### Files to Modify
- `MapScreen.kt` - Fix LaunchedEffect, pass camera control to FABs
- `MapControlFabs.kt` - Wire up proper onClick handlers with camera control
- `MapViewModel.kt` - Add event handlers for camera control

---

## Issue 2: Route Card Positioning & Swipe Feel 📍

### Problem
- Route info card is at bottom (needs to be at top)
- Swipe distance too short (current: 80dp, needs ~200dp for good feel)
- Swipe physics don't feel polished

### Current Implementation
**File**: `SwipeableRouteInfoCard.kt`
- Position: Bottom of screen via `Modifier.align(Alignment.BottomStart)`
- Swipe reveal: Only 80dp (`targetValue = if (isRevealed) -80f else 0f`)
- Basic drag gesture without physics

### Solution
1. **Move to top** - Change alignment to `TopCenter` in MapScreen.kt
2. **Increase swipe distance** - Change reveal distance from 80dp to 200dp
3. **Add spring physics** - Use `spring()` animation spec for bouncy feel
4. **Improve gesture handling**:
   - Add velocity tracking
   - Implement fling detection
   - Add haptic feedback on reveal/snap
   - Smooth interpolation during drag

### Files to Modify
- `MapScreen.kt` - Change card alignment from BottomStart to TopCenter
- `SwipeableRouteInfoCard.kt` - Rewrite swipe mechanics with spring physics
- Update padding logic for other elements

---

## Issue 3: Remove FAB X Button ❌

### Problem
- Close (X) FAB button is redundant since swipe-to-close exists
- Takes up screen real estate

### Current Implementation
**File**: `MapControlFabs.kt` (lines 35-46)
Shows conditional Close button when route exists

### Solution
1. **Remove Close FAB** - Delete the conditional button rendering
2. **Update MapControlFabs signature** - Remove `showCloseButton` parameter
3. **Update MapScreen.kt** - Remove `showCloseButton` prop
4. **Adjust spacing** - FAB stack will be shorter (2 buttons instead of 3)

### Files to Modify
- `MapControlFabs.kt` - Remove Close FAB
- `MapScreen.kt` - Remove showCloseButton prop

---

## Issue 4: Draggable Waypoint Reordering with Physics 🎨

### Problem
- Waypoints are static bubbles
- Can't reorder them
- No fun physics interaction

### Current Implementation
**File**: `WaypointTimeline.kt`
- Static Row layout with hardcoded order
- Click only removes waypoint
- No drag capability

### Solution - Full Rewrite Required
Implement drag-and-drop with LazyRow + modifier chains:

#### New Features
1. **Long-press to enter drag mode**
   - Haptic feedback on long press
   - Scale up selected bubble (1.2x)
   - Elevate with shadow
   
2. **Physics-based dragging**
   - Use `Modifier.draggable()` or `pointerInput` for gestures
   - Spring animation when released
   - Other bubbles "squeeze" and make room (physics simulation)
   
3. **Reorder on drop**
   - Calculate new position based on drag offset
   - Animate all bubbles to new positions
   - Call `viewModel.onEvent(MapEvent.ReorderWaypoints(...))`
   
4. **Visual feedback**
   - Dragged item: larger, elevated, semi-transparent
   - Other items: slide smoothly with spring animation
   - Drop zones: subtle background highlight
   
5. **Wiggle animation**
   - Add continuous subtle rotation (±3°) to dragged item
   - Makes it feel "alive" and grabbable

### Technical Implementation
- Use `remember { mutableStateOf<Int?>(null) }` for draggedIndex
- Track drag offset with `Offset` state
- Use `animateFloatAsState` with `spring()` for smooth transitions
- Consider using `LazyRow` instead of `Row` for better performance
- Add `Modifier.graphicsLayer { }` for rotation/scale effects

### Files to Modify
- `WaypointTimeline.kt` - Complete rewrite of layout and interaction
- `MapViewModel.kt` - Already has `ReorderWaypoints` event (verify implementation)
- `MapState.kt` - Verify waypoint reordering logic

---

## Implementation Order (Recommended)

### Phase 1: Quick Wins (30 min)
1. ✅ Fix auto-zoom issue (Issue #1 - part A)
2. ✅ Remove X FAB (Issue #3)
3. ✅ Move route card to top (Issue #2 - part A)

### Phase 2: Polish (45 min)
4. ✅ Implement proper FAB actions (Issue #1 - part B)
5. ✅ Improve swipe physics (Issue #2 - part B)

### Phase 3: Complex Feature (60-90 min)
6. ✅ Draggable waypoint reordering (Issue #4)

**Total Estimated Time**: 2-3 hours

---

## Testing Checklist

### Issue #1 - Map Zoom
- [ ] Pan around map - stays put after 5 seconds
- [ ] My Location FAB centers camera on user
- [ ] Compass FAB resets bearing and centers on user
- [ ] Initial load still centers on user location

### Issue #2 - Route Card
- [ ] Card appears at top of screen below waypoints
- [ ] Swipe left ~200dp to fully reveal close button
- [ ] Spring animation feels bouncy/natural
- [ ] Fling gesture works (fast swipe)
- [ ] Tap card when revealed to hide button

### Issue #3 - Remove X FAB
- [ ] No X button in FAB stack
- [ ] Only 2 FABs: Compass + My Location
- [ ] Swipe-to-close still works on route card

### Issue #4 - Waypoint Dragging
- [ ] Long-press waypoint bubble enters drag mode
- [ ] Haptic feedback on long-press
- [ ] Bubble scales up and elevates
- [ ] Other bubbles make room while dragging
- [ ] Drop reorders waypoints
- [ ] Route recalculates with new order
- [ ] Spring animations feel natural
- [ ] Wiggle animation during drag

---

## Code Quality Notes

### Dependencies (Already Available)
- ✅ Compose animation APIs
- ✅ `Modifier.pointerInput()` for gestures
- ✅ `hapticFeedback.performHapticFeedback()`
- ✅ Spring animation specs

### No New Dependencies Required
All features can be implemented with existing Compose APIs.

### Architecture Compatibility
- ✅ Fits existing MVVM pattern
- ✅ Event-driven design (MapEvent sealed class)
- ✅ Unidirectional data flow preserved
- ✅ Proper state hoisting

---

## Next Steps

Ready to implement? I'll start with Phase 1 (quick wins) and work through each phase systematically.

**Confirm to proceed** or let me know if you want to adjust priorities.

