import message.Message;

import java.nio.ByteBuffer;
import java.util.List;


public class TestParseMessagesAsByteArray {
    public static void main(String[] args) {
        //byte[] corresponding to a UNCHOKE MESSAGE
        byte[] m1 = {0,0,0,1,1};
        //byte[] corresponding to a INTERESTED MESSAGE
        byte[] m2 = {0,0,0,1,2};
        //byte[] corresponding to a BITFIELD MESSAGE
        byte[] m3 = {0,0,0,9,5,1,2,3,4,5,6,7,8};
        //byte[] corresponding to a REQUEST MESSAGE
        byte[] m4 = {0, 0, 0, 13, 6, 0,0,0,1,0,0,0,1,0,0,0,8};
        //byte[] corresponding to a PIECE MESSAGE
        byte[] m5 = {0, 0, 0, 13, 7, 0,0,0,1,0,0,0,1,0,0,0,8};
        //byte[] corresponding to a KEEP-ALIVE MESSAGE
        byte[] m6 = {0,0,0,0};
        // m = m1 + m2 + m3 + m4 + m5 + m6
        byte[] m = {0,0,0,1,1,0,0,0,1,2,0,0,0,9,5,1,2,3,4,5,6,7,8,0, 0, 0, 13, 6, 0,0,0,1,0,0,0,1,0,0,0,8,0, 0, 0, 13, 7, 0,0,0,1,0,0,0,1,0,0,0,8,0,0,0,0,    0, 0, 0, 13, 6, 0,0};
        ByteBuffer buf = ByteBuffer.wrap(m);
        List<Message> messages = Message.parserMessages(buf);
        for (Message message : messages){
            System.out.println(message);
        }
        buf.put(new byte[]{0, 1, 0, 0, 0, 1, 0, 0, 0, 8});
        List<Message> messages2 = Message.parserMessages(buf);
        for (Message message : messages2){
            System.out.println(message);
        }
    }
}
