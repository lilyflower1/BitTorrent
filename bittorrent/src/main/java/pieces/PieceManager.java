package pieces;

import message.PieceMessage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PieceManager class
 */
public class PieceManager extends ObservablePieceManager {
    private Torrent torrent;
    private int nbPieces;
    private List<Piece> pieces;
    private int maxPiecesToAskEachTime;
    private double completedPieces = 0;
    private List<Long> fileOffsets = new ArrayList<>();
    private Selection selection;
    private static final int HASH_SIZE = 20;
    public static final int MAX_SIZE = 1000000;
    private byte[] blockBitfield;
    private byte[] bitfield;
    private static AtomicInteger percentageDownloaded = new AtomicInteger(0);
    private final ConcurrentLinkedQueue queue;
    private final BlockingQueue conditionMet;

    Logger globalLogger = Logger.getLogger("global");

    /**
     * PieceManager class
     *
     * @param torrentObject Torrent
     * @param level         Level
     * @param conditionMet  BlockingQueue
     */
    public PieceManager(Torrent torrentObject, Level level, BlockingQueue conditionMet) {
        this.queue = new ConcurrentLinkedQueue<>();
        globalLogger.setLevel(level);
        torrent = torrentObject;
        nbPieces = (int) torrentObject.getNbPieces();
        pieces = initListPieces();
        selection = new SelectionRarestFirst(nbPieces);
        this.blockBitfield = new byte[this.getNbPieces()];
        maxPiecesToAskEachTime = (int) (MAX_SIZE / torrent.getPieceLength());
        if (maxPiecesToAskEachTime == 0)
            maxPiecesToAskEachTime = 1;
        this.conditionMet = conditionMet;
    }

    /**
     * Initiate empty list of pieces
     * @return an ordered array list of empty pieces
     */
    private List<Piece> initListPieces() {
        List<Piece> listPieces = new ArrayList<>();
        long offset = torrent.getPieceLength();
        for (int i = 0; i < nbPieces; i++) {
            long size = torrent.getPieceLength();
            listPieces.add(i, new Piece(i, size, Arrays.copyOfRange(torrent.getPiecesHash(), i * HASH_SIZE, (i + 1) * HASH_SIZE)));
            fileOffsets.add(i, (offset * i));
        }
        double size = torrent.getTotalLength() - (nbPieces - 1) * (double)torrent.getPieceLength();
        listPieces.set(nbPieces - 1, new Piece(nbPieces - 1, size,
                Arrays.copyOfRange(torrent.getPiecesHash(), (nbPieces - 1) * HASH_SIZE, nbPieces * HASH_SIZE)));
        return listPieces;
    }

    /**
     * Add a completed piece to piece manager list
     * @param p completed piece
     */
    public synchronized boolean addCompletedPiece(Piece p) throws NoSuchAlgorithmException, IOException {
        boolean completed = false;
        if (p.checkCompleted() && p.verifyIntegrity()) {
            completedPieces++;
            pieces.set(p.getIndex(), p);
            completed = true;
            // We add the piece to our bitfield
            updateBitfield();
            // We notify others peers we have a new piece
            this.notifyObserver(p);

        }
        return completed;
    }

    /**
     * check if all pieces are completed
     * @return true if all file pieces are completed
     */
    synchronized boolean allPiecesCompleted() {
        return completedPieces == nbPieces;
    }

    /**
     * Write piece in file
     * @param p piece element
     * @throws IOException exception
     */
    public synchronized void writePiece(Piece p) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(torrent.getOutputPath(), "rw");
        raf.seek(fileOffsets.get(p.getIndex()));
        raf.write(p.writePiece());
        raf.close();
    }

    /**
     * Get Left Pieces = pieces not downloaded
     *
     * @return Map of left pieces to download
     */
    public synchronized Map<Piece, List<Block>> getLeftPieces() {
        Map<Piece, List<Block>> leftPieces = new Hashtable<>();
        for (Piece p : pieces) {
            if (!p.checkCompleted()) {
                leftPieces.put(p, new ArrayList<>());
                for (Block b : p.getBlocks()) {
                    if (b.getBlockState() == BlockState.EMPTY || b.getBlockState() == BlockState.PENDING) {
                        leftPieces.get(p).add(b);
                    }
                }
            }
        }
        return leftPieces;
    }

    /**
     * GETTER for list of pieces
     * @return list of pieces
     */
    public synchronized List<Piece> getPieces() {
        return pieces;
    }

    /**
     * GETTER for number fo pieces
     * @return number of pieces
     */
    public synchronized int getNbPieces() {
        return nbPieces;
    }

    public synchronized Selection getSelectionPieces() {
        return selection;
    }

    /**
     * Update the blockBitField for the specified piece
     *
     * @param indexPiece index of the piece
     * @throws IOException exception
     */
    public void setBlockBitfieldForPiece(double indexPiece) throws IOException {
        this.blockBitfield[(int) indexPiece] = 1;
        this.updateBlockBitfieldInFile();
    }

    /**
     * Update the blockBitfield in the configuration file
     * @throws IOException exception
     */
    public void updateBlockBitfieldInFile() throws IOException {
        Path path = Paths.get(torrent.getFileName().concat("-config.txt"));
        if(!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.createFile(path);
        }
        Files.write(path, this.blockBitfield);
    }

    /**
     * Retrieve all pieces from final file to fill in PieceManager
     */
    public void retrieveAllPieces() throws IOException {
        Path path = Paths.get(torrent.getFileName().concat("-config.txt"));
        if(!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.createFile(path);
        }
        byte[] bFile = Files.readAllBytes(path);
        try {
            RandomAccessFile raf = new RandomAccessFile(torrent.getOutputPath(), "rw");
            int index = 0;
            for(byte b : bFile) {
                if(b == 1) {
                    this.blockBitfield[index] = 1;
                    Piece p = pieces.get(index);
                    long pos = fileOffsets.get(index);
                    raf.seek(pos);
                    for (Block block : p.getBlocks()) {
                        byte[] blockData = new byte[(int) block.getBlockSize()];
                        raf.read(blockData);
                        p.addBlockToPiece(block.getId() * 16384, blockData);
                    }
                    this.addCompletePiece(p);
                }
                index++;
            }
        } catch (Exception e) {
            globalLogger.severe(e.getMessage());
        }
    }

    /**
     * add a completed piece and update our bitfield
     *
     * @param p piece
     * @throws IOException              exception
     * @throws NoSuchAlgorithmException exception
     */
    public void addCompletePiece(Piece p) throws IOException, NoSuchAlgorithmException {
        if (p.checkCompleted() && p.verifyIntegrity()) {
            completedPieces++;
            pieces.set(p.getIndex(), p);
            // We add the piece to our bitfield
            updateBitfield();
        }
    }

    /**
     * get a PieceMessage and add this block to the piece manager
     * and if it's completing a piece then send it to the FileWriter
     *
     * @param pm PieceMessage
     * @throws IOException exception
     * @throws NoSuchAlgorithmException exception
     * @throws InterruptedException exception
     */
    public void addBlock(PieceMessage pm) throws IOException, NoSuchAlgorithmException, InterruptedException {
        int index = pm.getIndex();
        int begin = pm.getBegin();
        byte[] block = pm.getBlock();
        pieces.get(index).addBlockToPiece(begin, block);
        if (addCompletedPiece(pieces.get(pm.getIndex()))) {
            this.setBlockBitfieldForPiece(index);
            int downloaded = getPercentageDownloaded();
            globalLogger.info("Downloaded : " + downloaded + " %");
            if (downloaded == 100)
                conditionMet.put(true);
            synchronized (queue) {
                queue.add(pieces.get(pm.getIndex()));
                queue.notify();
            }
        }
    }

    /**
     * Method that construct the bitfield based on the list of completed pieces
     */
    public void updateBitfield(){
        int bitfieldSize = 1 + this.nbPieces / 8; //We calculate the needed size to write the bitfield
        byte[] bitfield = new byte[bitfieldSize];
        for (int i = 0; i < this.nbPieces; i++) {
            if (this.pieces.get(i).checkCompleted()) {
                int byteIndex = i / 8;
                int bitIndex = 7 - i % 8;
                bitfield[byteIndex] = (byte) (bitfield[byteIndex] | (1 << bitIndex));
            }
        }
        this.bitfield = bitfield;
    }

    public byte[] getBitfield() {
        updateBitfield();
        return bitfield;
    }

    /**
     * We check the number of pieces downloaded in order to update the download percentage
     *
     * @return percentage downloaded of file
     */
    public float updateDownloadPercentage() {
        int piecesDownloaded = 0;
        for (Piece p : pieces) {
            if (p.isCompleted()) {
                piecesDownloaded += 1;
            }
        }
        return (((float) piecesDownloaded / (float) pieces.size()) * 100);
    }

    public int getPercentageDownloaded() {
        percentageDownloaded.set((int) updateDownloadPercentage());
        return percentageDownloaded.get();
    }

    public int getMaxPiecesToAskEachTime() {
        return maxPiecesToAskEachTime;
    }

    public ConcurrentLinkedQueue getQueue() {
        return queue;
    }
}
