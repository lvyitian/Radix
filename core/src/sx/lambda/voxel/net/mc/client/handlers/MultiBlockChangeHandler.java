package sx.lambda.voxel.net.mc.client.handlers;

import org.spacehq.mc.protocol.data.game.values.world.block.BlockChangeRecord;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import sx.lambda.voxel.VoxelGameClient;

public class MultiBlockChangeHandler implements PacketHandler<ServerMultiBlockChangePacket> {

    private final VoxelGameClient game;

    public MultiBlockChangeHandler(VoxelGameClient game) {
        this.game = game;
    }

    @Override
    public void handle(ServerMultiBlockChangePacket packet) {
        for(BlockChangeRecord r : packet.getRecords()) {
            int x = r.getPosition().getX();
            int y = r.getPosition().getY();
            int z = r.getPosition().getZ();
            if(r.getBlock() > 0) {
                game.getWorld().addBlock(r.getBlock(), x, y, z);
            } else {
                game.getWorld().removeBlock(x, y, z);
                game.getWorld().getChunkAtPosition(x, z).setMeta((short)0, x, y, z);
            }
        }
    }

}
