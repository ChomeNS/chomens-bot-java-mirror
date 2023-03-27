package land.chipmunk.chayapak.chomens_bot.chatParsers.data;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;

import java.security.PublicKey;

@Data
@AllArgsConstructor
public class MutablePlayerListEntry {
    private GameProfile profile;
    private GameMode gamemode;
    private int latency;
    private Component displayName;
    private long expiresAt;
    private PublicKey publicKey;
    private byte[] keySignature;

    public MutablePlayerListEntry (PlayerListEntry entry) {
        this(entry.getProfile(), entry.getGameMode(), entry.getPing(), entry.getDisplayName(), entry.getExpiresAt(), entry.getPublicKey(), entry.getKeySignature());
    }
}
