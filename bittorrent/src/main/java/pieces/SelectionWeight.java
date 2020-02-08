package pieces;

import peers.PeerClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SelectionWeight class : pieces selected to balance charge on peers
 */
public class SelectionWeight extends Selection {
    SelectionWeight() {
        super();
    }

    /**
     * select pieces to ask for every peer
     *
     * @param piecesToDownload left pieces to download
     */
    @Override
    public synchronized void selectPiecesToAsk(Map<Piece, List<Block>> piecesToDownload) {
        // init pieces to ask Map
        for (Map.Entry<PeerClient, Map<Piece, List<Block>>> entry : piecesToAsk.entrySet())
            entry.setValue(new ConcurrentHashMap<>());

        // init peersCharge map
        Map<PeerClient, Integer> peersCharge = new HashMap<>();
        for (Map.Entry<PeerClient, List<Piece>> entry : pieces.entrySet())
            peersCharge.put(entry.getKey(), 0);

        // fill in map with pieces
        Iterator<Piece> it = piecesToDownload.keySet().iterator();
        while (it.hasNext()) {
            Piece p = it.next();
            for (Map.Entry<PeerClient, List<Piece>> entry : pieces.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    if (entry.getValue().contains(p))
                        peersCharge.put(entry.getKey(), peersCharge.get(entry.getKey()) + 1);
                } else
                    entry.getKey().setCompleted(true);
            }
            Map.Entry<PeerClient, Integer> min = null;
            for (Map.Entry<PeerClient, Integer> entry : peersCharge.entrySet()) {
                if (min == null || min.getValue() > entry.getValue()) {
                    min = entry;
                }
            }
            if (min != null) {
                piecesToAsk.get(min.getKey()).put(p, piecesToDownload.get(p));
                it.remove();
                for (Map.Entry<PeerClient, Integer> entry : peersCharge.entrySet()) {
                    if (entry.getKey() != min.getKey())
                        entry.setValue(entry.getValue() - 1);
                }
            }
        }
    }

    /**
     * pieces to ask fro a special peer
     *
     * @param p PeerClient
     */
    @Override
    public synchronized Map<Piece, List<Block>> selectPiecesToAskFromPeer(PeerClient p) {
        return this.piecesToAsk.get(p);
    }
}
