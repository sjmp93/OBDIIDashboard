OBDIIDashboard
========================

## NOTICE

**This project is going to be enhanced in the future by enhancing pires OBD API.**

 This project is based on pires's android-obd-reader project: https://github.com/pires/android-obd-reader
 
![screenshot](/caption.png)

The latest release can be found [here](https://github.com/sjmp93/OBDIIDashboard/).

## Prerequisites ##
- JDK 8
- Android Studio 3.5.x or newer
- Android SDK (API 22, Build tools 29.0.2)
- [pires's OBD Java API](https://github.com/pires/obd-java-api/) (already included)

## Test with device ##

Be sure to have the device connected to your computer.

```
cd whatever_directory_you_cloned_this_repository
gradle clean build installDebug
```

## Test with OBD Server ##

If you want to upload data to a server, for now, check the following:
* [OBD Server](https://github.com/pires/obd-server/) - a simple implementation of a RESTful app, compiled into a runnable JAR.
* Enable the upload functionality in preferences
* Set proper endpoint address and port in preferences.


## Tested on ##

* Xiaomi mi 8
* Xiaomi mi A1
* Google Pixel 3