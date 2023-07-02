package land.chipmunk.chayapak.chomens_bot.song;

import net.kyori.adventure.text.Component;

// Author: _ChipMC_ or hhhzzzsss?
public class SongLoaderException extends Exception {
  public final Component message;

  public SongLoaderException (Component message) {
    super();
    this.message = message;
  }
}
