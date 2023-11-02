# mLog

- com.jakewharton.timber和com.elvishew:xlog结合使用,处理多种日志文件的情况,增加了日志的扩展性,无需繁琐的管理每种类型的日志;

1. 使用:

    - 在项目build.gradle中添加:
````groovy
pluginManagement {
    repositories {
        //...
        maven { url 'https://jitpack.io' }
    }
}
````
    - 在需要使用mLog的module的build.gradle中添加:
````groovy
dependencies {
    implementation 'com.github.Misaka-XXXXII:mLog:1.0.0'
}
````

