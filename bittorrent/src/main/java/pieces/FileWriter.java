package pieces;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileWriter class and thread
 */
public class FileWriter extends Thread {
    private final ConcurrentLinkedQueue writingQueue;
    private PieceManager manager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    protected Logger globalLogger = Logger.getLogger("global");

    public FileWriter(PieceManager pieceManager, Level level) {
        globalLogger.setLevel(level);
        manager = pieceManager;
        writingQueue = pieceManager.getQueue();
    }

    /**
     * Write piece byte array whenever FileWriter receive a Piece
     * with the help of a queue
     */
    @Override
    public void run() {
        running.set(true);
        while (running.get() && !manager.allPiecesCompleted()) {
            synchronized (writingQueue) {
                while (writingQueue.isEmpty()) {
                    try {
                        writingQueue.wait();
                    } catch (InterruptedException e) {
                        globalLogger.severe(e.getMessage());
                    }
                }
            }
            Piece p;
            try {
                p = (Piece) writingQueue.poll();
                manager.writePiece(p);
            } catch (IOException e) {
                globalLogger.severe(e.getMessage());
                running.set(false);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Interrupt thread
     */
    @Override
    public void interrupt() {
        running.set(false);
        Thread.currentThread().interrupt();
    }
}
