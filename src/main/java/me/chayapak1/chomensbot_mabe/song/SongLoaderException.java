package me.chayapak1.chomensbot_mabe.song;

import net.kyori.adventure.text.Component;
import lombok.Getter;
import me.chayapak1.chomensbot_mabe.util.ComponentUtilities;

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
