package pieces;

import peers.PeerClient;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Selection class
 */
public abstract class Selection {
    protected ConcurrentHashMap<PeerClient, List<Piece>> pieces;
    public Hashtable<PeerClient, Map<Piece, List<Block>>> piecesToAsk;
    public Hashtable<PeerClient, AtomicBoolean> maj;
    protected final AtomicBoolean done = new AtomicBoolean(false);

    Selection() {
        pieces = new ConcurrentHashMap<>();
        piecesToAsk = new Hashtable<>();
        maj = new Hashtable<>();
    }

    /**
     * init array lists for a peer
     *
     * @param p PeerClient
     */
    public synchronized void initBitfieldForPeer(PeerClient p) {
        pieces.put(p, new ArrayList<>());
        piecesToAsk.put(p, new ConcurrentHashMap<>());
        maj.put(p, new AtomicBoolean(false));
    }

    /**
     * set bitfield for peer
     *
     * @param p    Piece
     * @param peer PeerClient
     */
    public synchronized void setBitfield(Piece p, PeerClient peer) {
        pieces.get(peer).add(p);
    }

    /**
     * abstract method to select pieces to ask for every peer
     *
     * @param piecesToDownload left pieces to download
     */
    public abstract void selectPiecesToAsk(Map<Piece, List<Block>> piecesToDownload);

    /**
     * abstract method giving pieces to ask from a peer
     *
     * @param p PeerClient
     * @return Pieces to ask
     */
    public abstract Map<Piece, List<Block>> selectPiecesToAskFromPeer(PeerClient p);

    /**
     * method which verify if every peer has set up its bitfield
     *
     * @return true if all peers have set up their bitfield and false if not
     */
    public synchronized boolean allPeersAskedForTheirBitfield() {
        for (Map.Entry<PeerClient, List<Piece>> entry : pieces.entrySet()) {
            if (!entry.getKey().getBitfieldSet().get()) {
                return false;
            }
        }
        return true;
    }

    /**
     * method which set a peer to completed when there is nothing left to ask from it
     */
    public synchronized void setCompleted() {
        for (Map.Entry<PeerClient, Map<Piece, List<Block>>> entry : piecesToAsk.entrySet()) {
            entry.getKey().setCompleted(true);
        }
    }

    /**
     * method which remove peer from the Selection
     *
     * @param peerClient PeerClient
     */
    public void removePeer(PeerClient peerClient) {
        pieces.remove(peerClient);
        piecesToAsk.remove(peerClient);
    }
    /*****************************************************************************/
    /**                             GETTER AND SETTER                           **/
    /*****************************************************************************/
    public void setDone() {
        done.getAndSet(true);
    }

    public void setUnDone() {
        done.getAndSet(false);
    }

    public AtomicBoolean getDone() {
        return done;
    }

    public boolean getMaj(PeerClient p) {
        return maj.get(p).get();
    }

    public void setMaj(PeerClient p, boolean b) {
        maj.get(p).set(b);
    }

    public void setMaj(boolean b) {
        for (Map.Entry<PeerClient, AtomicBoolean> p : maj.entrySet()) {
            p.getValue().set(b);
        }
    }


}
