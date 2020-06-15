# Gleipnir - Attack POC

## Introduction

Gleipnir introduces a new Attack which is able to execute third party Apps dynamically at Runtime. Instead of extract the victim App, modify it and deploy with a different signing key, this threat loads the full unmodified victim App in a compromised process owned by the attacker. This offers the chance to apply reflection or hooking actions without the need having root access.
Real users could install such an App tainted as classical Launcher App for example and the full potential of this threat could be applied. Obtain user's personal information, credentials, manipulate transactions or Advertising identifiers to let the Attacker earn the clicks produced by the user playing Games or familiar Apps are some of the goals possible to reach.
This work figures out the possibility how to perform this attack conceptional as well as experimental.

The Gleipnir threat brings most benefits of the repackaging Attack, no root needed for example, and extends it by the following benefits:
* *Dynamically* -  The victim App(s) must not be known to the attacker before and will be compromised at runtime
* *Apk digest* -  The original App is not touched means the Apk digest of the Apk doesn't change.
* *Distribution* - No need for having a installation per App. Just one malicious App including the Gleipnir code is able to attack App's installed on the users Device. Best way to do this would be a Launcher App (a home screen replacement).

But some disadvantages have to be mentioned:
* *Runtime only* - The Gleipnir Attack is a Runtime only attack and thus depends on the Android implementation. New Android versions may require migration.
* *No code changes - Potentially it's not possible to just modify code of the victim App. Hooking or Reflection can be used to manipulate the behaviour.

## Requirements
Android Studio: 3.6.1

Android Versions: 26 - 30 (preview 3)

## Compile

#### Gradle
```
gradle :gleipnir:assemble
```
#### Android Studio

Launch gleipnir Run Configuration

## Test

Along with this project the sumbodul testVictim is provided.
This App can be used to test the gleipnir attack.

1. Build the gleipnir module
2. Install the gleipnir apk
3. Build the testVictim module
4. Install the testVictim apk
5. Launch the Gleipnir App
6. Select the Test Victim App

## Create Plug-Ins

The Interface IPlugin.kt can be implemented in order to create
extensions for the Gleipnir App.

First create a Kotlin class in package:
```
org.gleipnir.app.plugins
```
and implement the IPlugin interface.

Next and last step is to register the Plug-In in the list of Plug-Ins
located in the Plugtivity class. Just add the Plug-In constructor to
 the field:
```
pluginsList
```

## Enable the frida Plug-In (OPTIONAL)

In order to enable the frida Plug-In you have to insert frida gadget
libraries into the
```
gleipnir/src/main/jniLibs/[arch]/libfrida-gadget.so
```
folder(s). You can rename this file just have a look at FridaPlugin.kt