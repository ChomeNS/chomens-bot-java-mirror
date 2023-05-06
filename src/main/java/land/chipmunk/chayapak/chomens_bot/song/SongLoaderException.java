package land.chipmunk.chayapak.chomens_bot.song;

import net.kyori.adventure.text.Component;
import lombok.Getter;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;

// Author: _ChipMC_ or hhhzzzsss?
public class SongLoaderException extends Exception {
  @Getter private final Component message;

  public SongLoaderException (Component message) {
    super();
    this.message = message;
  }

  public SongLoaderException (Component message, Throwable cause) {
    super(null, cause);
    this.message = message;
  }

  @Override
  public String getMessage () {
    return ComponentUtilities.stringify(message);
  }
}
