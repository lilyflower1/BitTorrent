import peers.PeerClient;
import peers.PeerManager;
import peers.Server;
import pieces.*;
import tracker.HttpRequestType;
import tracker.Tracker;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BitTorrentClient class
 * Application Class
 */
public class BitTorrentClient {
    private static File torrent = null;
    private static File directory = null;
    private static Logger globalLogger = Logger.getLogger("global");
    private static boolean displayMode = false;
    private static BlockingQueue<Boolean> isFileDownloaded = new SynchronousQueue<>();
    private static String interfaceIP = "lo0";

    public static void main(String[] args) throws Exception {
        globalLogger.setLevel(Level.SEVERE);
        // get and process arguments from command line
        Torrent torrentObject = parseArguments(args);
        PieceManager pieceManager = new PieceManager(torrentObject, globalLogger.getLevel(), isFileDownloaded);
        PeerManager peerManagerLeecher = new PeerManager(globalLogger.getLevel());
        Server myself = new Server(getMyAddress(interfaceIP), 6883, new PeerManager(globalLogger.getLevel()), pieceManager, torrentObject, globalLogger.getLevel());
        Tracker httpClient = new Tracker(torrentObject.getAnnounce(), globalLogger.getLevel());
        FileWriter fw = new FileWriter(pieceManager, globalLogger.getLevel());
        if (displayMode) {
            displayInfosThread(pieceManager, peerManagerLeecher, myself);
        }
        if (torrentObject.checkAlreadyDownloaded()) {
            pieceManager.retrieveAllPieces();
            pieceManager.getPercentageDownloaded();
        }
        globalLogger.info("Downloaded : " + pieceManager.getPercentageDownloaded() + " %");
        launch(torrentObject, pieceManager, peerManagerLeecher, myself, httpClient, fw);
    }

    /**
     * general launch method
     *
     * @param torrentObject Torrent
     * @param pieceManager  PieceManager
     * @param peerManager   PeerManager
     * @param myself        Server
     * @param httpClient    Tracker
     * @param fw            FileWriter
     * @throws UnsupportedEncodingException exception
     * @throws MalformedURLException        exception
     * @throws InterruptedException         exception
     */
    private static void launch(Torrent torrentObject, PieceManager pieceManager, PeerManager peerManager, Server myself, Tracker httpClient, FileWriter fw) throws UnsupportedEncodingException, MalformedURLException, InterruptedException {
        if (pieceManager.getPercentageDownloaded() != 100) { // leecher and seeder
            launchLeecherAndSeeder(torrentObject, pieceManager, peerManager, myself, httpClient, fw);
        } else if (pieceManager.getPercentageDownloaded() == 100) // seeder mode
        {
            launchSeeder(torrentObject, pieceManager, peerManager, myself, httpClient);
        }
    }

    /**
     * launch Seeder only method when the file is already downloaded
     *
     * @param torrentObject Torrent
     * @param pieceManager  PieceManager
     * @param peerManager   PeerManager
     * @param myself        Server
     * @param httpClient    Tracker
     * @throws UnsupportedEncodingException exception
     * @throws MalformedURLException        exception
     */
    private static void launchSeeder(Torrent torrentObject, PieceManager pieceManager, PeerManager peerManager, Server myself, Tracker httpClient) throws UnsupportedEncodingException, MalformedURLException {
        httpClient.sendGet(HttpRequestType.COMPLETED, myself, torrentObject, peerManager, pieceManager);
        myself.start();
    }

    /**
     * launch Leecher and Seeder method when the file is not already completely downloaded
     *
     * @param torrentObject Torrent
     * @param pieceManager  PieceManager
     * @param peerManager   PeerManager
     * @param myself        Server
     * @param httpClient    Tracker
     * @param fw            FileWriter
     * @throws UnsupportedEncodingException exception
     * @throws MalformedURLException exception
     * @throws InterruptedException exception
     */
    private static void launchLeecherAndSeeder(Torrent torrentObject, PieceManager pieceManager, PeerManager peerManager, Server myself, Tracker httpClient, FileWriter fw) throws UnsupportedEncodingException, MalformedURLException, InterruptedException {
        // init
        httpClient.sendGet(HttpRequestType.STARTED, myself, torrentObject, peerManager, pieceManager);
        fw.start();
        myself.start();
        launchSelection(torrentObject, pieceManager, peerManager, myself, httpClient);

        while (true) {
            Object output = isFileDownloaded.poll(10, TimeUnit.SECONDS);
            if (output != null) {
                break;
            }
            launchSelection(torrentObject, pieceManager, peerManager, myself, httpClient);
        }
        fw.interrupt();
        pieceManager.getSelectionPieces().setCompleted();
        httpClient.sendGet(HttpRequestType.COMPLETED, myself, torrentObject, peerManager, pieceManager);
    }

    /**
     * launch pieces selection and contact tracker
     *
     * @param torrentObject Torrent
     * @param pieceManager  PieceManager
     * @param peerManager   PeerManager
     * @param myself        Server
     * @param httpClient    Tracker
     * @throws UnsupportedEncodingException exception
     * @throws MalformedURLException        exception
     */
    private static void launchSelection(Torrent torrentObject, PieceManager pieceManager, PeerManager peerManager, Server myself, Tracker httpClient) throws UnsupportedEncodingException, MalformedURLException {
        pieceManager.getSelectionPieces().setUnDone();
        httpClient.sendGet(HttpRequestType.OTHER, myself, torrentObject, peerManager, pieceManager);
        while (!pieceManager.getSelectionPieces().allPeersAskedForTheirBitfield()) {
        }
        pieceManager.getSelectionPieces().selectPiecesToAsk(pieceManager.getLeftPieces());
        pieceManager.getSelectionPieces().setDone();
        pieceManager.getSelectionPieces().setMaj(true);
    }

    /**
     * thread where global information are displayed
     *
     * @param pieceManager       PieceManager
     * @param peerManagerLeecher PeerManager
     * @param myself             Server
     */
    private static void displayInfosThread(PieceManager pieceManager, PeerManager peerManagerLeecher, Server myself) {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    displayInfos(pieceManager, peerManagerLeecher, myself);
                    Thread.sleep(1000L);
                }
            } catch (InterruptedException e) {
                globalLogger.severe(e.getMessage());
            }
        });
        thread.start();
    }

    /**
     * method that displays information about current peers connected, percentage downloaded
     *
     * @param pieceManager       PieceManager
     * @param peerManagerLeecher PeerManager
     * @param myself             Server
     */
    private static void displayInfos(PieceManager pieceManager, PeerManager peerManagerLeecher, Server myself) {
        System.out.println("Liste des pairs connectés :");
        for (PeerClient p : peerManagerLeecher.getPeers()) {
            System.out.println(p.getIp() + " : " + p.getPort());
        }
        for (PeerClient p : myself.getPeerManager().getPeers()) {
            System.out.println(p.getIp() + " : " + p.getPort());
        }
        System.out.println("Pieces téléchargées à " + pieceManager.getPercentageDownloaded() + "%");
        for (Piece piece : pieceManager.getPieces()) {
            for (Block b : piece.getBlocks()) {
                System.out.println("Piece " + piece.getIndex() + " Bloc " + b.getId() + " : " + b.getBlockState().toString());
            }
        }
    }

    /**
     * get IP address of current application
     *
     * @param interfaceIp String interface of tracker
     * @return InetAdress IP
     * @throws SocketException exception
     */
    private static String getMyAddress(String interfaceIp) throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress myAddress = null;
        while (interfaces.hasMoreElements()) {
            NetworkInterface currentInterface = interfaces.nextElement();
            if (currentInterface.getName().equals(interfaceIp)) {
                //chaque carte réseau peut disposer de plusieurs adresses IP
                Enumeration<InetAddress> addresses = currentInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    myAddress = addresses.nextElement();
                }
            }
        }
        if (myAddress == null) {
            help();
            System.exit(1);
        }
        return myAddress.getHostAddress();
    }

    /**
     * method that parses arguments on command line
     *
     * @param args string arguments
     * @return Torrent
     * @throws IOException exception
     * @throws NoSuchAlgorithmException exception
     */
    private static Torrent parseArguments(String[] args) throws IOException, NoSuchAlgorithmException {
        Torrent t = null;
        for (String argument : args) {
            if (argument.equals("--debug")) {
                globalLogger.setLevel(Level.INFO);
            } else if (argument.equals("--info")) {
                displayMode = true;
            } else if (argument.contains("-ip=")) {
                interfaceIP = argument.split("-ip=")[1];
            } else {
                File torrentFile = new File(argument);
                if (argument.contains(".torrent")) {
                    if (torrentFile.isFile()) {
                        // torrent file
                        torrent = torrentFile;
                    } else {
                        globalLogger.severe("Not a valid torrent file");
                        help();
                        System.exit(1);
                    }
                } else {
                    if (torrentFile.isDirectory()) {
                        // Download Folder
                        directory = torrentFile;
                    }
                }
            }
        }
        if (directory != null) {
            t = new Torrent(torrent, directory, globalLogger.getLevel());
        } else {
            globalLogger.severe("Not a valid download directory");
            help();
            System.exit(1);
        }
        return t;
    }

    /**
     * help method displaying help for user
     */
    public static void help() {
        String help = "java -jar mybittorrent.jar <file.torrent> <download_folder> [--debug] [--info] <-ip=IP_interface>\n";
        help += "\n";
        help += "<file.torrent>         torrent file you would like to download\n";
        help += "<download_folder>      download folder\n";
        help += "[--debug]              mode debug to see trace\n";
        help += "[--info]               mode info to see every second which peers are connected\n";
        help += "<-ip=IP_interface>     select your ip interface where your server will be available (ex : -ip=em1 or -ip=lo0, etc...)\n";
        help += "                       by default it will be lo0\n";
        System.out.println(help);
    }
}
