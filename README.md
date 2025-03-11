# Sofreh Messina

A modern Android application built with Jetpack Compose and Firebase.

## Project Overview

This application uses:
- Jetpack Compose for UI
- Firebase for backend services
- Hilt for dependency injection
- Material 3 design components
- KSP for annotation processing

## Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Gradle 8.10.2+
- Android SDK 34

### Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

## Windows Development

If you're developing on Windows, please refer to the [Windows Development Guide](README-WINDOWS.md) for specific instructions on handling file locking issues and other Windows-specific considerations.

## Project Structure

- `app/` - Main application module
- `app/src/main/java/com/example/sofrehmessina/` - Application source code
- `app/src/main/res/` - Resources (layouts, drawables, etc.)
- `gradle/libs.versions.toml` - Dependency versions and catalog

## Build Configuration

The project uses the following build configuration:

- Kotlin 1.9.22
- Compose 1.6.0
- Compose Compiler 1.5.8
- Material3 1.1.2
- Hilt 2.50
- KSP 1.9.22-1.0.17

## Troubleshooting

If you encounter build issues:

1. Check the [Windows Development Guide](README-WINDOWS.md) if you're on Windows
2. Try running `clean-windows.bat` to safely clean build directories
3. Ensure you have the correct JDK version (17)
4. Verify that all dependencies are properly resolved

## License

[License information goes here] 
