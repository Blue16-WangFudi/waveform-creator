# Contributing

Thanks for your interest in contributing to Waveform Creator. Contributions are welcome for firmware, Android app, documentation, and testing.

## Getting Started
- Read the project overview and build steps in `README.md`.
- Build the firmware and Android app once before making changes to confirm your environment.

## Development Workflow
- Create a feature branch from the default branch.
- Keep commits focused and descriptive.
- Avoid committing generated artifacts (for example: `build/`, `.gradle/`, `*.hex`).
- Update documentation when behavior, hardware requirements, or usage changes.

## Testing
- Firmware: build with STM32CubeIDE, CLion + CMake, or your ARM GCC toolchain.
- Android app: build from Android Studio or run `./gradlew assembleDebug`.

## License
By contributing, you agree that your contributions will be licensed under the Apache License 2.0.