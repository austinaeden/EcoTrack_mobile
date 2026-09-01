# Link Users to their Activity Data

This plan outlines the steps to modify the database and application logic so that each registered user has their own private set of activity logs.

## User Review Required

> [!IMPORTANT]
> This change requires a destructive database migration. **All existing activity logs will be deleted** during the upgrade to the new schema.

## Proposed Changes

### Data Layer

#### [MODIFY] [ActivityLog.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/data/ActivityLog.kt)
- Add `userId: Int` field to the `ActivityLog` data class.
- Define a `ForeignKey` relationship linking `ActivityLog.userId` to `User.id`.
- Add an index on `userId` for faster lookups.

#### [MODIFY] [ActivityLogDao.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/data/ActivityLog.kt)
- Update `getAllLogs()` to accept a `userId: Int` parameter and filter results accordingly.
- Ensure all queries are scoped to a specific user.

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/data/AppDatabase.kt)
- Increment the database version from `2` to `3`.

---

### UI / Logic Layer

#### [MODIFY] [LoginActivity.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/ui/LoginActivity.kt)
- Pass the logged-in user's ID to `MainActivity` via an Intent extra.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/MainActivity.kt)
- Receive the `userId` from the Intent.
- Initialize the `MainViewModel` with this `userId`.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/USER/AndroidStudioProjects/EcoTrack/app/src/main/java/com/example/ecotrack/ui/MainViewModel.kt)
- Add a `currentUserId` state.
- Update `allLogs` to be a `Transformations.switchMap` (or similar) that reacts to changes in `currentUserId`.
- Update `saveLog` to include the `currentUserId`.

---

## Verification Plan

### Manual Verification
1. Register a new user (User A).
2. Save a few logs.
3. Log out and register another user (User B).
4. Verify that User B sees **zero** logs.
5. Save a log for User B.
6. Log out and log back in as User A.
7. Verify that User A only sees their original logs.
