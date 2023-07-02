package land.chipmunk.chayapak.chomens_bot.data.chat;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import net.kyori.adventure.text.Component;

import java.security.PublicKey;

public class MutablePlayerListEntry {
    public GameProfile profile;
    public GameMode gamemode;
    public int latency;
    public Component displayName;
    public long expiresAt;
    public PublicKey publicKey;
    public byte[] keySignature;
    public boolean listed;

    public MutablePlayerListEntry (
            GameProfile profile,
            GameMode gamemode,
            int latency,
            Component displayName,
            long expiresAt,
            PublicKey publicKey,
            byte[] keySignature,
            boolean listed
    ) {
        this.profile = profile;
        this.gamemode = gamemode;
        this.latency = latency;
        this.displayName = displayName;
        this.expiresAt = expiresAt;
        this.publicKey = publicKey;
        this.keySignature = keySignature;
        this.listed = listed;
    }

    public MutablePlayerListEntry (PlayerListEntry entry) {
        this(entry.getProfile(), entry.getGameMode(), entry.getLatency(), entry.getDisplayName(), entry.getExpiresAt(), entry.getPublicKey(), entry.getKeySignature(), entry.isListed());
    }
}
