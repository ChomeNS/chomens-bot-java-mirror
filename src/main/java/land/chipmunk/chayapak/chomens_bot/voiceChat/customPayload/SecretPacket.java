package land.chipmunk.chayapak.chomens_bot.voiceChat.customPayload;

import land.chipmunk.chayapak.chomens_bot.data.voiceChat.Codec;
import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;

import java.util.UUID;

public class SecretPacket implements Packet<SecretPacket> {
    public UUID secret;
    public int serverPort;
    public UUID playerUUID;
    public Codec codec;
    public int mtuSize;
    public double voiceChatDistance;
    public int keepAlive;
    public boolean groupsEnabled;
    public String voiceHost;
    public boolean allowRecording;

    @Override
    public SecretPacket fromBytes(FriendlyByteBuf buf) {
        secret = buf.readUUID();
        serverPort = buf.readInt();
        playerUUID = buf.readUUID();
        codec = Codec.values()[buf.readByte()];
        mtuSize = buf.readInt();
        voiceChatDistance = buf.readDouble();
        keepAlive = buf.readInt();
        groupsEnabled = buf.readBoolean();
        voiceHost = buf.readUtf();
        allowRecording = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes (FriendlyByteBuf buf) {
        buf.writeUUID(secret);
        buf.writeInt(serverPort);
        buf.writeUUID(playerUUID);
        buf.writeByte(codec.ordinal());
        buf.writeInt(mtuSize);
        buf.writeDouble(voiceChatDistance);
        buf.writeInt(keepAlive);
        buf.writeBoolean(groupsEnabled);
        buf.writeUtf(voiceHost);
        buf.writeBoolean(allowRecording);
    }
}
