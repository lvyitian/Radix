package sx.lambda.voxel.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.math.MathUtils;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import sx.lambda.voxel.VoxelGameClient;
import sx.lambda.voxel.api.BuiltInBlockIds;
import sx.lambda.voxel.entity.EntityPosition;
import sx.lambda.voxel.entity.EntityRotation;
import sx.lambda.voxel.entity.LivingEntity;
import sx.lambda.voxel.world.IWorld;
import sx.lambda.voxel.world.chunk.BlockStorage;
import sx.lambda.voxel.world.chunk.IChunk;

import java.io.Serializable;

public class Player extends LivingEntity implements Serializable {

    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    private static final float REACH = 4;
    private static Model playerModel;
    private transient boolean moved = false;
    private int itemInHand = BuiltInBlockIds.STONE_ID;

    public Player() {
        this(new EntityPosition(0, 0, 0), new EntityRotation());

        if (playerModel == null && VoxelGameClient.getInstance() != null) {
            playerModel = new ObjLoader().loadModel(Gdx.files.internal("entity/player.obj"));
        }

    }

    public Player(EntityPosition pos, EntityRotation rot) {
        super(playerModel, pos, rot, WIDTH, HEIGHT);
    }

    public float getEyeHeight() {
        return HEIGHT * 0.75f;
    }

    public float getReach() {
        return REACH;
    }

    public int getItemInHand() {
        return itemInHand;
    }

    public void setItemInHand(int block) {
        itemInHand = block;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (moved) {
            if (VoxelGameClient.getInstance().getMinecraftConn() != null) {
                VoxelGameClient.getInstance().getMinecraftConn().getClient().getSession().send(new ClientPlayerPositionRotationPacket(this.isOnGround(), this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ(), (float) 180 - this.getRotation().getYaw(), -this.getRotation().getPitch()));
            }
            moved = false;
        }
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    public boolean hasMoved() {
        return this.moved;
    }

    @Override
    public Model getDefaultModel() {
        return playerModel;
    }

    public int getBlockInHead(IWorld world) {
        int x = MathUtils.floor(getPosition().getX());
        int z = MathUtils.floor(getPosition().getZ());
        int y = MathUtils.floor(getPosition().getY() + HEIGHT);
        IChunk chunk = world.getChunk(x, z);
        if (chunk != null) {
            if (y >= world.getHeight()) return 0;
            try {
                return chunk.getBlockId(x & (world.getChunkSize() - 1), y, z & (world.getChunkSize() - 1));
            } catch (BlockStorage.CoordinatesOutOfBoundsException e) {
                e.printStackTrace();
                return 0;
            }
        } else {
            return 0;
        }
    }

}