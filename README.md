# ChomeNS Bot Java
A Java version of ChomeNS Bot.

# Compiling

To make this successfully compile, you will have to fix the missing `ExploitsPlugin.java` in the `plugins` package, and also some enums related to it. 

After that, you can now run `./gradlew build` to actually get the `.jar` file.

The .jar file will be at `build/libs`, to run the bot simply do `java -jar chomens_bot-rolling-all.jar` 

# Development

When commiting your changes, please use my code style.

In IntelliJ IDEA:

`Ctrl + Alt + S`, search `Code Style`, go to `Java`, click gear icon, `Import Scheme -> IntelliJ IDEA code style XML`,
use the `codestyle.xml` file in this repository

`Ctrl + Alt + Shift + H`, click `Configure Inspections...`, click gear icon, `Import Profile...`,
use the `inspections.xml` file in this repository
