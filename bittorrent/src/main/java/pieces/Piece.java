package pieces;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Piece class
 */
public class Piece {
    public static final int BLOCK_SIZE = 16384;
    private int index;
    private double size;
    private byte[] hash;
    private List<Block> blocks = new ArrayList<>();
    private double nbBlocks;
    private boolean completed;

    /**
     * pieces.Piece Constructor
     * @param i index
     * @param length piece size
     */
    Piece(int i, double length, byte[] pieceHash){
        index = i;                          // pieces.Piece index field
        size = length;
        nbBlocks = Math.ceil(size / BLOCK_SIZE);  // piece size divided by block size = 2^14
        initBlocks();
        completed = false;
        hash = pieceHash;
    }

    /**
     * Initiate blocks list in a piece
     */
    private void initBlocks() {
        blocks = new ArrayList<>();
        // if I have more than 1 block
        if (nbBlocks > 1){
            for (int i = 0; i < nbBlocks; i++){
                blocks.add(new Block(i, BLOCK_SIZE, BlockState.EMPTY));
            }
            // last block can have a size less than a normal block size
            if ((size % BLOCK_SIZE) > 0){
                blocks.set((int) (nbBlocks - 1), new Block((int) (nbBlocks - 1),size % BLOCK_SIZE, BlockState.EMPTY));
            }
        }
        // lonely block with a block size less than the normal block size
        else
            blocks.add(new Block(0, size, BlockState.EMPTY));
    }

    /**
     * Add a completed block to a piece
     *
     * @param begin     offset
     * @param blockData data
     */
    public synchronized void addBlockToPiece(int begin, byte[] blockData) {
        int i = begin / 16384;
        if (!completed && blocks.get(i).getBlockState() != BlockState.FULL) {
            blocks.get(i).setBlockData(blockData);
            blocks.get(i).setBlockState(BlockState.FULL);
        }
    }

    /**
     * Verify hash of received data
     * @return true if hash is ok and else false
     * @throws NoSuchAlgorithmException SHA-1 exception
     */
    public synchronized boolean verifyIntegrity() throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        for (Block b : blocks){
            outputStream.write(b.getBlockData());
        }
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(outputStream.toByteArray());

        if (Arrays.equals(result, this.hash)){
            return true;
        }
        else {
            initBlocks();
            completed = false;
            return false;
        }
    }

    /**
     * check if piece has all of its blocks completed
     * @return true if piece completed else false
     */
    synchronized boolean checkCompleted() {
        for (Block b : blocks) {
            if (b.getBlockState() == BlockState.EMPTY || b.getBlockState() == BlockState.PENDING) {
                return false;
            }
        }
        this.completed = true;
        return true;
    }

    /**
     * Write piece in FileWriter
     *
     * @return byte array
     * @throws IOException exception
     */
    synchronized byte[] writePiece() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Block b : blocks) {
            outputStream.write(b.getBlockData());
        }
        return outputStream.toByteArray();
    }

    public synchronized int getIndex() {
        return index;
    }

    public synchronized List<Block> getBlocks() {
        return blocks;
    }

    public synchronized double getSize() {
        return size;
    }

    public boolean isCompleted() {
        return completed;
    }
}
