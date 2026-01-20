# Waveform Creator
This project is a function signal generator controlled over Android Bluetooth SPP and driven by an STM32F103 MCU.

## Repository Layout
- `WaveFormCreator/`: STM32F103 firmware (CubeMX + HAL, includes AD9833 driver).
- `WaveFormCreator_SPI/`: Android app (Bluetooth SPP to JDY-31-SPP, sends control commands).
- `stm32f103c8t6/`: Proteus simulation project and backups.

## Build & Run
### Firmware
- Open `WaveFormCreator/` with STM32CubeMX or CLion/CMake.
- Build and flash to STM32F103C8T6.

### Android App
- Open `WaveFormCreator_SPI/` with Android Studio.
- Pair with `JDY-31-SPP`, connect, and send parameters.

## Command Protocol
`CMD{waveform}{frequency}{amplitude}{phase}`
- `waveform`: waveform index (0-based).
- `frequency`: 7 digits, range 1..1000000.
- `amplitude`: 4 digits, range 1..3300 (mV).
- `phase`: 3 digits, range 0..360 (degrees).
