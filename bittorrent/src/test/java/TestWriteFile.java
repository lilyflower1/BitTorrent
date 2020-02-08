import com.google.common.primitives.Bytes;
import pieces.Block;
import pieces.Piece;
import pieces.PieceManager;
import pieces.Torrent;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class TestWriteFile {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Torrent torrentObject = new Torrent(new File("testFiles/original.torrent"), new File("testFiles/"), Level.INFO);
        PieceManager manager = new PieceManager(torrentObject, Level.INFO, null);
        for (Piece p : manager.getPieces()) {
            int i = 0;
            for (Block b : p.getBlocks()) {
                InputStream is = new FileInputStream("src/test/files/" + "piece_" + (int) p.getIndex() + "_bloc_" + i + ".txt");
                System.out.println("src/test/files/" + "piece_" + (int) p.getIndex() + "_bloc_" + i + ".txt");
                BufferedReader buf = new BufferedReader(new InputStreamReader(is));
                String line = buf.readLine();
                StringBuilder sb = new StringBuilder();
                while (line != null) {
                    sb.append(line);
                    line = buf.readLine();
                }
                List<Byte> data = new ArrayList<>(hexStringToByteArray(sb.toString()));
                if (i == 0)
                    p.addBlockToPiece(0, Bytes.toArray(data));
                else
                    p.addBlockToPiece((int) (i * p.getBlocks().get(i - 1).getBlockSize()), Bytes.toArray(data));
                i++;
            }
            manager.addCompletePiece(p);
            manager.writePiece(p);
        }

    }
    public static Collection<? extends Byte> hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        final List<Byte> list = new ArrayList<>();
        for (byte o : data) {
            list.add(o);
        }
        return list;
    }
}
