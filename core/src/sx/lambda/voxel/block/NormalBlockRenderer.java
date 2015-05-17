package sx.lambda.voxel.block;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import groovy.transform.CompileStatic;
import sx.lambda.voxel.VoxelGameClient;
import sx.lambda.voxel.render.NotInitializedException;
import sx.lambda.voxel.util.Vec3i;
import sx.lambda.voxel.util.gl.SpriteBatcher;
import sx.lambda.voxel.world.chunk.IChunk;

import java.nio.FloatBuffer;

@CompileStatic
public class NormalBlockRenderer implements IBlockRenderer {

    protected static final float TEXTURE_PERCENTAGE = 0.03125f;

    private static Texture blockMap;

    protected final float u, v;
    protected final int blockID;

    private static boolean initialized;

    final int BLOCKS_PER_WIDTH = 1024/32;

    public NormalBlockRenderer(int blockID) {
        this.blockID = blockID;
        u = ((blockID%BLOCKS_PER_WIDTH)*TEXTURE_PERCENTAGE);
        v = ((blockID/BLOCKS_PER_WIDTH)*TEXTURE_PERCENTAGE);
    }

    @Override
    public void render2d(SpriteBatcher batcher, int x, int y, int width) {
        if(!initialized) {
            initialize();
        }
        float u2 = u+TEXTURE_PERCENTAGE-.001f;
        float v2 = v+TEXTURE_PERCENTAGE-.001f;
        batcher.drawTexturedRect(x, y, x+width, y+width, u, v, u2, v2);
    }

    @Override
    public Mesh renderNorth(int x1, int y1, int x2, int y2, int z, float lightLevel, MeshBuilder builder) {
        // POSITIVE Z
        builder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
        builder.setColor(lightLevel, lightLevel, lightLevel, 1);
        builder.setUVRange(u, v, u + TEXTURE_PERCENTAGE, v + TEXTURE_PERCENTAGE);
        builder.rect(x1, y1, z,
                x2, y1, z,
                x2, y2, z,
                x1, y2, z,
                0, 0, 1);
        return builder.end();
    }

    @Override
    public Mesh renderSouth(int x1, int y1, int x2, int y2, int z, float lightLevel, MeshBuilder builder) {
        // NEGATIVE Z
        builder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
        builder.setColor(lightLevel, lightLevel, lightLevel, 1);
        builder.setUVRange(u, v, u + TEXTURE_PERCENTAGE, v + TEXTURE_PERCENTAGE);
        builder.rect(x1, y2, z,
                x2, y2, z,
                x2, y1, z,
                x1, y1, z,
                0, 0, -1);
        return builder.end();
    }

    @Override
    public Mesh renderWest(int z1, int y1, int z2, int y2, int x, float lightLevel, MeshBuilder builder) {
        // NEGATIVE X
        builder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
        builder.setColor(lightLevel, lightLevel, lightLevel, 1);
        builder.setUVRange(u, v, u + TEXTURE_PERCENTAGE, v + TEXTURE_PERCENTAGE);
        builder.rect(x, y1, z2,
                x, y2, z2,
                x, y2, z1,
                x, y1, z1,
                -1, 0, 0);
        return builder.end();
    }

    @Override
    public Mesh renderEast(int z1, int y1, int z2, int y2, int x, float lightLevel, MeshBuilder builder) {
        // POSITIVE X
        builder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
        builder.setColor(lightLevel, lightLevel, lightLevel, 1);
        builder.setUVRange(u, v, u + TEXTURE_PERCENTAGE, v + TEXTURE_PERCENTAGE);
        builder.rect(x, y1, z1,
                x, y2, z1,
                x, y2, z2,
                x, y1, z2,
                1, 0, 0);
        return builder.end();
    }

    @Override
    public Mesh renderTop(int x1, int z1, int x2, int z2, int y, float lightLevel, MeshBuilder builder) {
        // POSITIVE Y
        builder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
        builder.setColor(lightLevel, lightLevel, lightLevel, 1);
        builder.setUVRange(u, v, u + TEXTURE_PERCENTAGE, v + TEXTURE_PERCENTAGE);
        builder.rect(x1, y, z2,
                x2, y, z2,
                x1, y, z1,
                x1, y, z1,
                0, 1, 0);
        return builder.end();
    }

    @Override
    public Mesh renderBottom(int x1, int z1, int x2, int z2, int y, float lightLevel, MeshBuilder builder) {
        // NEGATIVE Y
        builder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
        builder.setColor(lightLevel, lightLevel, lightLevel, 1);
        builder.setUVRange(u, v, u + TEXTURE_PERCENTAGE, v + TEXTURE_PERCENTAGE);
        builder.rect(x1, y, z1,
                x2, y, z1,
                x2, y, z2,
                x1, y, z2,
                0, -1, 0);
        return builder.end();
    }

    private static void initialize() {
        try {
            blockMap = VoxelGameClient.getInstance().getBlockTextureAtlas();
        } catch (NotInitializedException e) {
            System.err.println("Error getting block texture atlas!");
            e.printStackTrace();
        }
        initialized = true;
    }

    public static Texture getBlockMap() {
        if(blockMap == null) {
            initialize();
        }
        return blockMap;
    }
}
