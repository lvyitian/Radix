package sx.lambda.voxel.entity

import com.badlogic.gdx.graphics.g3d.Model
import groovy.transform.CompileStatic
import sx.lambda.voxel.tasks.MovementHandler
import sx.lambda.voxel.world.IWorld
import sx.lambda.voxel.world.chunk.IChunk

@CompileStatic
public abstract class LivingEntity extends Entity implements Serializable {

    private synchronized transient float yVelocity
    private boolean onGround
    private final float width, height

    //Needed for serialization
    public LivingEntity() {
        this(getDefaultModel(), new EntityPosition(0, 0, 0), new EntityRotation())
    }

    public LivingEntity(Model model, EntityPosition pos, EntityRotation rot) {
        this(model, pos, rot, 1, 1);
    }

    public LivingEntity(Model model, EntityPosition pos, EntityRotation rot, float width, float height) {
        super(model, pos, rot)
        this.width = width
        this.height = height
    }

    public float getYVelocity() { this.yVelocity }

    public boolean isOnGround() { this.onGround }

    public void updateMovement(MovementHandler handler) {
        if (this.onGround) {
            this.yVelocity = Math.max(this.yVelocity, 0)
        }
        if (!handler.checkCollision(this, 0, yVelocity, 0)) {
            this.getPosition().offset(0, this.yVelocity, 0)
        } else {
            yVelocity = 0
        }
    }

    public void setYVelocity(float velocity) {
        synchronized (this) {
            this.yVelocity = velocity
        }
    }

    public int getWidth() { this.width }

    public float getHeight() { this.height }

    public void setOnGround(boolean onGround) {
        synchronized (this) {
            this.onGround = onGround
        }
    }

    /**
     * Called 20 times a second
     */
    public void onUpdate() {}

    public int getBlockInFeet(IWorld world) {
        IChunk chunk = world.getChunkAtPosition((int)Math.floor(position.x), (int)Math.floor(position.z))
        if (chunk != null) {
            return chunk.getBlockIdAtPosition((int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(position.z))
        } else {
            return 0
        }
    }

}