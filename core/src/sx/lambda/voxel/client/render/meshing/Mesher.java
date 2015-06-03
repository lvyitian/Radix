package sx.lambda.voxel.client.render.meshing;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import sx.lambda.voxel.block.Block;

/**
 * Turns an array of voxels into OpenGL vertices
 */
public interface Mesher {

    /**
     * Meshes the specified voxels.
     *
     * @param builder     MeshBuilder to build the mesh onto
     * @param voxels      Voxels to mesh
     * @param lightLevels Light levels of voxels  @return Result of meshing the voxels.
     */
    Mesh meshVoxels(MeshBuilder builder, Block[][][] voxels, short[][][] metadata, float[][][] lightLevels);

}
