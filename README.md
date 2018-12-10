# Skitter Video

Skitter Video is a video component for React Native.  
It bridges Java code and JNI native code to React Native.  
It uses the VisualOn OnStream MediaPlayer for Verimatrix encrpytion.  

# How to use

Run ```react-native link``` to link the skitter-video library.
In the top level build.gradle file of the project add under repositories:  
```flatDir { dirs "$rootDir/../node_modules/skitter-video/android/libs" }```

*To link files manually, add the following to the given files:*

**android/settings.gradle**

```gradle
include ':skitter-video'
project('skitter-video').projectDir = new File(rootProject.projectDir, '../node_modules/skitter-video/android')
```

**android/settings.gradle**

```gradle
dependencies {
    ...
    compile project(':skitter-video')
}
```

**MainApplication.java**

Add to your import statement

```java
import import com.sungjkim.react.ReactVideoPackage; //TODO: change namespace to skitter
```

Add the ``ReactVideoPackage`` class to your list of exported packages.

```java
@Override
protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new ReactVideoPackage()
    );
}
```

