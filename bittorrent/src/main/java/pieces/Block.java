package pieces;

/**
 * Block class
 */
public class Block {
    private int id;
    private double blockSize;
    private byte[] blockData;
    private BlockState blockState;

    Block(int i, double length, BlockState s) {
        id = i;
        blockSize = length;
        blockState = s;
    }

    public synchronized double getBlockSize() {
        return blockSize;
    }

    public synchronized byte[] getBlockData() {
        return blockData;
    }

    synchronized void setBlockData(byte[] blockData) {
        this.blockData = blockData;
    }

    public synchronized BlockState getBlockState() { return blockState; }

    public synchronized void setBlockState(BlockState blockState) {
        this.blockState = blockState;
    }

    public synchronized int getId(){return id;}
}
