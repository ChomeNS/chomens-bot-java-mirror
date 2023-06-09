# ChomeNS Bot Java
A Java verison of ChomeNS Bot.

Also you will see that the exploits plugin is missing because I gitignored it to prevent exploit leaks

To make this compile just make `ExploitsPlugin.java` in the plugins folder and add this code:

```java
package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.UUID;

public class ExploitsPlugin {
    private final Bot bot;

    public ExploitsPlugin (Bot bot) {
        this.bot = bot;
    }

    public void kick (UUID uuid) {}
}
```

And it should compile.
