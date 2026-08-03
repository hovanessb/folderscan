# APK Generation

You might find it useful to generate the APK locally you can use the following commands with docker. The APK will be otherwise available at every commit as an artifact, incrementing the APK version number and the version name will be the commit SHA short name.


### Create the APK locally

```bash
gradle assembleRelease
```
Test the docker container for CI
```bash
docker run -v /home/hovaness/Public/ignitefolderscan:/builds registry.docker.Cheesecake.org/build-android:android-31-jdk11-node16-focal gradle assembleRelease
```
 
