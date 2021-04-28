# usage

In your root build.gradle add my bintray repository:

``` groovy
allprojects {
    repositories {
        maven {
           url 'https://dl.bintray.com/maxsmirnov92/maven' }
    }
}
```

In your project build.gradle (usually app/build.gradle or mobile/build.gradle):

``` groovy
dependencies {
    api 'net.maxsmr:commonutils:1.0'
    api 'net.maxsmr:commonutils-jre:1.0'
    api 'net.maxsmr:customcontentprovider:1.0'
    api 'net.maxsmr:devicewatchers:1.0'
    api 'net.maxsmr:networkutils:1.0'
    api 'net.maxsmr:tasksutils:1.0'
}
```

You can find out which version of each module is latest here: https://bintray.com/maxsmirnov92/maven/MxSUtilsLibrary
or build by yourself and include locally to your target project

## Build signed release AAR (include appropriate android-library module(s) in settings.gradle): "gradlew buildRelease"
## Build signed JAR (include appropriate java-library module(s) in settings.gradle): "gradlew jar"
## Upload to bintray after: "gradlew bintrayUpload"
