# Fix RouteCanvas Data Mixing Issue

## Goals
- Consolidate vehicle management into RouteCanvas only
- Remove duplicate vehicle management from RouteDetailsView
- Add proper cleanup to RouteCanvas when switching routes
- Ensure traffic light coordination works properly
- Test that route switching no longer mixes data

## Implementation Steps
- [x] Add proper cleanup method to RouteCanvas when switching routes
- [x] Remove duplicate vehicle management from RouteDetailsView (vehicleStopTimes, vehicleProgressions, etc.)
- [x] Update RouteDetailsView to use RouteCanvas as the primary vehicle management system
- [x] Ensure traffic light coordination works properly with consolidated system
- [x] Test that route switching no longer mixes data
