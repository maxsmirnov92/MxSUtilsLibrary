# usage

In your root build.gradle add my bintray repository:

``` groovy
allprojects {
    repositories {
        ...
        maven {
            url 'https://dl.bintray.com/maxsmirnov92/maven'
        }
    }
}
```

In your project build.gradle (usually app/build.gralde or mobile/build.gralde):

``` groovy
dependencies {
    ...
    compile 'net.maxsmr:commonutils:1.0'
    compile 'net.maxsmr:customcontentprovider:1.0'
    compile 'net.maxsmr:devicewatchers:1.0'
    compile 'net.maxsmr:networkutils:1.0'
    compile 'net.maxsmr:recyclerview-lib:1.0'
    compile 'net.maxsmr:tasksutils:1.0'
    compile 'net.maxsmr:jugglerhelper:1.0'
}
```

You can find out which version of each module is latest here: https://bintray.com/maxsmirnov92/maven/MxSUtilsLibrary