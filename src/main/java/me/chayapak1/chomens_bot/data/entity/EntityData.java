package me.chayapak1.chomens_bot.data.entity;

import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import org.cloudburstmc.math.vector.Vector3d;

public class EntityData {
    public PlayerEntry player;
    public Vector3d position;
    public Rotation rotation;

    public EntityData (final PlayerEntry player, final Vector3d position, final Rotation rotation) {
        this.player = player;
        this.position = position;
        this.rotation = rotation;
    }

    @Override
    public String toString () {
        return "EntityData{" +
                "player=" + player +
                ", position=" + position +
                ", rotation=" + rotation +
                '}';
    }
}
