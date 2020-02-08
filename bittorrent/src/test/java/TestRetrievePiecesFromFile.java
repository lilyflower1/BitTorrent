import pieces.Piece;
import pieces.PieceManager;
import pieces.Torrent;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

public class TestRetrievePiecesFromFile {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Torrent torrentObject = new Torrent(new File("testFiles/original.torrent"), new File("testFiles/"), Level.INFO);
        PieceManager manager = new PieceManager(torrentObject, Level.INFO, null);
        manager.retrieveAllPieces();
        torrentObject.setOutputPath("testFiles/original2");
        for (Piece p : manager.getPieces()) {
            manager.writePiece(p);
        }
    }
}
