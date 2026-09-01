# Implementation Plan - Daily Step Reset & Real-Time Analytics

This plan introduces automatic daily step count resets and replaces the placeholder data in the Analytics screen with actual recorded activity logs.

## User Review Required

> [!IMPORTANT]
> The daily reset logic relies on storing the "step offset" in `SharedPreferences`. If the device is rebooted, the `TYPE_STEP_COUNTER` sensor resets to zero, so the app will detect this and recalibrate automatically.

## Proposed Changes

### Step Tracking & Persistence

#### [MODIFY] [StepSensorManager.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/data/StepSensorManager.kt)
- Add logic to store and retrieve the "base" step count for the current day using `SharedPreferences`.
- Implement a `checkDayChanged()` method to detect when a new day starts and reset the daily counter.
- Add a manual `reset()` function.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/ui/MainViewModel.kt)
- Add a utility function to process `ActivityLog` data into a format suitable for the bar chart (e.g., summing steps per day for the last 7 days).

### User Interface

#### [MODIFY] [HomeFragment.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/ui/HomeFragment.kt)
- Add a "Reset" button to the UI (xml update might be needed, or I'll just add the listener if a button exists/add one programmatically/via layout modification).
- *Correction*: I will update `fragment_home.xml` (if available) or just add a long-click listener to the step text for manual reset to keep it clean, or add a dedicated button if space permits.

#### [MODIFY] [AnalyticsFragment.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/ui/AnalyticsFragment.kt)
- Observe `viewModel.allLogs`.
- Update the `BarChart` dynamically whenever new logs are added or deleted.

## Verification Plan

### Automated Tests
- I will verify the logic via manual deployment and inspection of the logs.

### Manual Verification
- Deploy the app to the emulator/device.
- Save a few activity logs.
- Navigate to the Analytics tab and verify the bar chart reflects the saved logs.
- Trigger a manual reset and verify the counter goes to zero.
- (Simulation) Change the device date to tomorrow and verify the counter resets.
