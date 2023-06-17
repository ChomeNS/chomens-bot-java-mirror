package land.chipmunk.chayapak.chomens_bot.song;

import lombok.Getter;
import net.kyori.adventure.text.Component;

// Author: _ChipMC_ or hhhzzzsss?
public class SongLoaderException extends Exception {
  @Getter private final Component message;

  public SongLoaderException (Component message) {
    super();
    this.message = message;
  }
}
