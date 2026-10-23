# Leasing Companion

A JavaFX prototype for reviewing lease expirations and balancing renewal end dates
against a daily move-out capacity.

The app currently uses a deterministic mock property-management client; it does
not connect to a live property-management system.

## Requirements

- JDK 21

## Run

On Windows:

```powershell
.\gradlew.bat run
```

On macOS or Linux:

```sh
./gradlew run
```

JavaFX dependencies are resolved by Gradle.

## Test

On Windows:

```powershell
.\gradlew.bat test
```

On macOS or Linux:

```sh
./gradlew test
```
