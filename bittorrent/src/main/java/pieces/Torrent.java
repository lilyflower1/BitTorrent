package pieces;

import bencode.BDecoder;
import bencode.BEncodedValue;
import bencode.BEncoder;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Torrent Class
 */
public class Torrent {
    private String outputPath;
    private String announce;
    private byte[] infoHash;
    private String fileName;
    private Long totalLength;
    private Long pieceLength;
    private double nbPieces;
    private byte[] piecesHash;
    protected Logger globalLogger = Logger.getLogger("global");

    public Torrent(File torrent, File directory, Level level) throws IOException, NoSuchAlgorithmException {
        globalLogger.setLevel(level);
        String directoryName = directory.getName();
        parse(torrent);
        outputPath = directoryName + "/" + fileName;
    }

    /**
     * method checking if the file is already in its directory
     *
     * @return true if the file exists and is a file and false if not
     */
    public boolean checkAlreadyDownloaded() {
        File file = new File(outputPath);
        return file.exists() && file.isFile();
    }

    /**
     * method parsing torrent file
     *
     * @param torrent File
     * @throws IOException              exception
     * @throws NoSuchAlgorithmException exception
     */
    private void parse(File torrent) throws IOException, NoSuchAlgorithmException {
        InputStream flux = new FileInputStream(torrent);

        BDecoder reader = new BDecoder(flux);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        Map<String, BEncodedValue> info = document.get("info").getMap();
        announce = document.get("announce").getString();
        totalLength = info.get("length").getLong();
        fileName = new String(info.get("name").getBytes());
        pieceLength = info.get("piece length").getLong();
        nbPieces = Math.ceil((float)totalLength / pieceLength);
        piecesHash = info.get("pieces").getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BEncoder.encode(info, baos);
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        this.infoHash = mDigest.digest(baos.toByteArray());
        globalLogger.info("file to download : " + fileName);
        globalLogger.info("pieces number to download : " + nbPieces);
    }

    /*************************************************************************/
    /**                         GETTER AND SETTER                           **/
    /*************************************************************************/
    public String getAnnounce() {
        return announce;
    }

    Long getPieceLength() {
        return pieceLength;
    }

    byte[] getPiecesHash() {
        return piecesHash;
    }

    public Long getTotalLength() {
        return totalLength; }

    public double getNbPieces() { return nbPieces; }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String path){ outputPath = path;}

    public byte[] getInfoHash() {
        return this.infoHash;
    }

    public String getFileName() {
        return fileName;
    }
}
