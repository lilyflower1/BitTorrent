package pieces;

import peers.PeerClient;

import java.util.*;

/**
 * SelectionRarestFirst class
 */
public class SelectionRarestFirst extends SelectionWeight {
    Map<Integer, Integer> rarestPieces; // < index Piece, nb of peers having piece>

    SelectionRarestFirst(int nbPieces) {
        super();
        rarestPieces = initRarestPiecesDict(nbPieces);
    }

    /**
     * init Map for rarest pieces
     *
     * @param nbPieces pieces number
     * @return Map rarestPieces
     */
    private Map<Integer, Integer> initRarestPiecesDict(int nbPieces) {
        Map<Integer, Integer> dict = new HashMap<>();
        for (int i = 0; i < nbPieces; i++) {
            dict.put(i, 0);
        }
        return dict;
    }

    /**
     * setBitfield for a peer
     *
     * @param p    Piece
     * @param peer PeerClient
     */
    @Override
    public synchronized void setBitfield(Piece p, PeerClient peer) {
        super.setBitfield(p, peer);
        rarestPieces.put(p.getIndex(), rarestPieces.get(p.getIndex()) + 1);
    }

    /**
     * select pieces to ask for every peer
     *
     * @param piecesToDownload left pieces to download
     */
    @Override
    public synchronized void selectPiecesToAsk(Map<Piece, List<Block>> piecesToDownload) {
        super.selectPiecesToAsk(piecesToDownload);
        // Reverse rarestPieces map in descending order
        LinkedHashMap<Integer, Integer> reverseSortedMap = new LinkedHashMap<>();
        rarestPieces.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
        rarestPieces.clear();
        rarestPieces.putAll(reverseSortedMap);

        // il faut trier le tableau piecesToAsk en fonction de rarestPieces
        for (Map.Entry<PeerClient, Map<Piece, List<Block>>> peer : piecesToAsk.entrySet()) {
            int cursor = 0;
            ArrayList<Piece> keySetList = new ArrayList<>(piecesToAsk.get(peer.getKey()).keySet());
            for (Integer rarestPiece : rarestPieces.keySet()) {
                Iterator iterator = piecesToAsk.get(peer.getKey()).keySet().iterator();
                while (iterator.hasNext()) {
                    Piece key = (Piece) iterator.next();
                    if (key.getIndex() == rarestPiece) {
                        Set<Piece> keys = piecesToAsk.get(peer.getKey()).keySet();
                        List<Piece> listKeys = new ArrayList<>(keys);
                        Collections.swap(keySetList, cursor, listKeys.indexOf(key));
                        LinkedHashMap<Piece, List<Block>> swappedMap = new LinkedHashMap<>();

                        for (Piece oldSwappedKey : keySetList) {
                            swappedMap.put(oldSwappedKey, piecesToAsk.get(peer.getKey()).get(oldSwappedKey));
                        }
                        piecesToAsk.get(peer.getKey()).clear();
                        piecesToAsk.get(peer.getKey()).putAll(swappedMap);
                        cursor++;
                    }
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
