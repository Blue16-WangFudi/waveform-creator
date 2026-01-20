# Waveform Creator
Waveform Creator is a function signal generator built around an STM32F103 MCU and an AD9833 DDS module, controlled from an Android app over Bluetooth SPP.

## Features
- STM32F103 firmware generated with CubeMX + HAL and an AD9833 driver.
- Android app that sends waveform parameters over Bluetooth SPP (JDY-31-SPP).
- Proteus project for simulation and quick validation.

## Repository Layout
- `firmware-stm32f103/`: STM32F103 firmware (CubeMX + HAL, AD9833 driver).
- `android-app/`: Android app (Bluetooth SPP controller).
- `proteus-simulation/`: Proteus simulation project.

## Requirements
- STM32F103C8T6 (Blue Pill) and AD9833 module.
- Bluetooth SPP module (e.g., JDY-31-SPP).
- Android Studio for the app; STM32CubeMX/STM32CubeIDE or a CMake toolchain for firmware.

## Installation
### Firmware
1. Open `firmware-stm32f103/WaveFormCreator.ioc` in STM32CubeMX and generate code if needed.
2. Build the project with STM32CubeIDE, CLion + CMake, or your preferred ARM GCC toolchain.
3. Flash the firmware to the STM32F103C8T6.

### Android App
1. Open `android-app/` in Android Studio.
2. Sync Gradle and run the app on a device with Bluetooth.

## Usage
1. Pair the phone with the Bluetooth SPP module.
2. Open the app, connect, and send waveform parameters.

Example command (waveform 0, 1 kHz, 3300 mV, 0 deg):
`CMD000010003300000`

## Command Protocol
`CMD{waveform}{frequency}{amplitude}{phase}`
- `waveform`: waveform index (0-based, single digit).
- `frequency`: 7 digits, range 1..1000000 (Hz).
- `amplitude`: 4 digits, range 1..3300 (mV).
- `phase`: 3 digits, range 0..360 (degrees).

## License
Apache License 2.0. See `LICENSE`.
