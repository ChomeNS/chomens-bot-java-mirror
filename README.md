# ChomeNS Bot Java
A Java verison of ChomeNS Bot.

# Compiling
You will see that the exploits plugin is missing because I gitignored it to prevent exploit leaks

To make this successfully compile just make `ExploitsPlugin.java` in the plugins folder and add this code:

```java
package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;

import java.util.UUID;

public class ExploitsPlugin {
    public ExploitsPlugin (Bot bot) {}

    public void kick (UUID uuid) {}
    
    public void pcrash (UUID uuid) {}
}
```

Then at the root of the project run `./gradlew shadowJar` for Linux or `gradlew.bat shadowJar` for Windows

The .jar file will be at `build/libs`, to run the bot do `java -jar chomens_bot-rolling-all.jar` 
