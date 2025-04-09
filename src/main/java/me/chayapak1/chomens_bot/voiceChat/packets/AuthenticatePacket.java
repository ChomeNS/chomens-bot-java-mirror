package me.chayapak1.chomens_bot.voiceChat.packets;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

import java.util.UUID;

public class AuthenticatePacket implements Packet<AuthenticatePacket> {
    public UUID playerUUID;
    public UUID secret;

    public AuthenticatePacket () { }

    public AuthenticatePacket (
            final UUID playerUUID,
            final UUID secret
    ) {
        this.playerUUID = playerUUID;
        this.secret = secret;
    }

    @Override
    public AuthenticatePacket fromBytes (final FriendlyByteBuf buf) {
        final AuthenticatePacket packet = new AuthenticatePacket();
        packet.playerUUID = buf.readUUID();
        packet.secret = buf.readUUID();
        return packet;
    }

    @Override
    public void toBytes (final FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeUUID(secret);
    }
}
