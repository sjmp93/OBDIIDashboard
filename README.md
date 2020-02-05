obdii-dashboard
========================

## NOTICE

**This project is going to be enhanced in the future by enhancing pires OBD API.**

 This project is based on pires's android-obd-reader project: https://github.com/pires/android-obd-reader
 
![screenshot](/caption.png)

The latest release can be found [here](https://github.com/sjmp93/OBDII-Dashboard/).

## Prerequisites ##
- JDK 8
- Android Studio 1.5.x or newer
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

## Troubleshooting ##


Please refer to https://github.com/pires/android-obd-reader troubleshooting section
```

## Building with custom `obd-java-api`

This project depends on a [pure-Java OBD library](https://github.com/pires/obd-java-api/). For testing with a custom version of it, do the following:

* Clone obd-java-api it into your project folder:

```
git clone https://github.com/pires/obd-java-api.git
```

* Create `obd-java-api/build.gradle` with the following content:

```
apply plugin: 'java'
```

* Edit `main build.gradle` and change:

```
compile 'com.github.pires:obd-java-api:1.0-RC14'`
```

to

```
compile project(':obd-java-api')
```

* Edit `settings.gradle` and add:

```
include ':obd-java-api'
```

## Tested on ##

* Xiaomi mi 8
* Xiaomi mi A1
