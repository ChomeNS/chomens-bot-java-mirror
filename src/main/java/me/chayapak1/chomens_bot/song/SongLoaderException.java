package me.chayapak1.chomens_bot.song;

import net.kyori.adventure.text.Component;

// Author: hhhzzzsss
public class SongLoaderException extends Exception {
    public final Component message;

    public SongLoaderException (Component message) {
        super();
        this.message = message;
    }
}
