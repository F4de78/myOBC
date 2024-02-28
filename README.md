# myOBC: Android OBDII interface
Simple Android App written in Kotlin to communicate with your car ECU using the OBD interface.

# ⚠️ Disclaimer ⚠️
This project was made for the "Mobile Development" exam at my uni, I didn't have time and resources to expand and test it more, so I think that it's easy to find ~a lot~ some bugs and something could be done in a better way.

**Take this project as a starting point for similar apps or as an example of how those functionalities/libraries could be used.**

## Supported devices
I tested the app on Android 10 and Android 12, using a Bluetooth ELM327 OBDII adapter (the cheapest that I found on Amazon)

## Functionalities 
- Monitor:
  - speed
  - engine RPM
  - engine coolant temperature
  - oil temperature
  - intake air temperature
  - engine load
- Record the above parameters in CSV file

The [report](md-report.pdf) that I wrote for the exam can be used as some kind of documentation

## Libraries and Tools
For the communication with the ECU I used [kotlin-obd-api](https://github.com/eltonvs/kotlin-obd-api) and for the debugging, I used [ELM327-emulator](https://github.com/Ircama/ELM327-emulator).
Big thanks to the creators and contributors of those projects!


