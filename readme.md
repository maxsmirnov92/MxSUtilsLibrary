# usage

In your root build.gradle add my bintray repository:

``` groovy
allprojects {
    repositories {
        ...
        maven {
           url 'https://dl.bintray.com/maxsmirnov92/maven' }
    }
}
```

In your project build.gradle (usually app/build.gradle or mobile/build.gradle):

``` groovy
dependencies {
    ...
    api 'net.maxsmr:commonutils:1.0'
    api 'net.maxsmr:customcontentprovider:1.0'
    api 'net.maxsmr:devicewatchers:1.0'
    api 'net.maxsmr:networkutils:1.0'
    api 'net.maxsmr:recyclerview-lib:1.0'
    api 'net.maxsmr:tasksutils:1.0'
    api 'net.maxsmr:jugglerhelper:1.0'
}
```

You can find out which version of each module is latest here: https://bintray.com/maxsmirnov92/maven/MxSUtilsLibrary

## Build signed release AAR (left one module in settings.gradle first): "gradlew buildRelease"
## Upload to bintray after: "gradlew bintrayUpload"