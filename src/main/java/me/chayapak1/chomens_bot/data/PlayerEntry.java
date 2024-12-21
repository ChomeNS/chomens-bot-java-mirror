package me.chayapak1.chomens_bot.data;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class PlayerEntry {
    public final GameProfile profile;
    public GameMode gamemode;
    public int latency;
    public Component displayName;
    public List<String> usernames = new ArrayList<>();
    public final long expiresAt;
    public PublicKey publicKey;
    public final byte[] keySignature;
    public boolean listed;

    public PlayerEntry(
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

        this.usernames.add(this.profile.getName());
    }

    public PlayerEntry (PlayerListEntry entry) {
        this(entry.getProfile(), entry.getGameMode(), entry.getLatency(), entry.getDisplayName(), entry.getExpiresAt(), entry.getPublicKey(), entry.getKeySignature(), entry.isListed());
    }

    @Override
    public String toString() {
        return "PlayerEntry{" +
                "gamemode=" + gamemode +
                ", latency=" + latency +
                ", listed=" + listed +
                ", displayName=" + displayName +
                '}';
    }
}
