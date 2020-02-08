public class TestConstructBitfield {
    public static void main(String[] args) {
        byte[] bitfield = constructBitfield(2);
    }

    public static byte[] constructBitfield(int nbPieces) {
        int bitfieldSize = 1 + (int) nbPieces / 8; //We calculate the needed size to write the bitfield
        byte[] bitfield = new byte[bitfieldSize];
        for (int i = 0; i < nbPieces; i++) {
            //int byteIndex = (bitfieldSize - 1) - i / 8;
            int byteIndex = i/8;
            int bitIndex = 7 - i % 8;
            bitfield[byteIndex] = (byte) (bitfield[byteIndex] | (1 << bitIndex));
        }
        return bitfield;
    }
}
