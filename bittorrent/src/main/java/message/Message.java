package message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Message abstract class
 */
public abstract class Message {
    private ByteBuffer buf;
    private TypeMessage typeMessage;

    public Message(int bufferLength) {
        buf = ByteBuffer.allocate(bufferLength);
    }

    public Message(){}

    /**
     * retrieve messages in byte buffer
     *
     * @param messages bytebuffer
     * @return list of messages
     */
    public static List<Message> parserMessages(ByteBuffer messages) {
        ArrayList<Message> receivedMessages = new ArrayList<>();
        //Parse
        messages.flip();        // ready for reading
        while (messages.position() + 4 < messages.limit()) {
            int messageLength = messages.getInt();   //Get the messageLength
            if (messageLength > 0) {
                ByteBuffer buf = ByteBuffer.allocate(messageLength + 4);
                ByteBuffer bb = ByteBuffer.allocate(4);
                bb.putInt(messageLength);
                buf.put(bb.array());
                // We check that the message isn't a KeepAlive message
                byte[] payload = new byte[messageLength];
                if ((messages.position() + payload.length) <= messages.limit()) {
                    messages.get(payload);
                    buf.put(payload);
                    receivedMessages.add(detectMessageType(buf)); //Add the created Message after parsing to the list of receivedMessages
                } else {
                    messages.position(messages.position() - 4);
                    break;
                }
            }
        }
        messages.compact();

        return receivedMessages;
    }

    /**
     * detect message type depending on its index in bytebuffer
     * @param m bytebuffer
     * @return message
     */
    private static Message detectMessageType(ByteBuffer m) {
        m.flip();
        m.getInt();
        byte[] id = new byte[1];
        m.get(id, 0, 1);
        m.position(0);
        switch (id[0]) {
            case 0:
                ChokeMessage chokeMessage = new ChokeMessage();
                return (chokeMessage.getMessageFromByteBuffer(m));
            case 1:
                UnchokeMessage unchokeMessage = new UnchokeMessage();
                return (unchokeMessage.getMessageFromByteBuffer(m));
            case 2:
                InterestedMessage interestedMessage = new InterestedMessage();
                return (interestedMessage.getMessageFromByteBuffer(m));
            case 3:
                NotInterestedMessage notInterestedMessage = new NotInterestedMessage();
                return(notInterestedMessage.getMessageFromByteBuffer(m));
            case 4:
                HaveMessage haveMessage = new HaveMessage();
                return(haveMessage.getMessageFromByteBuffer(m));
            case 5:
                BitfieldMessage bitfieldMessage = new BitfieldMessage();
                return(bitfieldMessage.getMessageFromByteBuffer(m));
            case 6:
                RequestMessage requestMessage = new RequestMessage();
                return(requestMessage.getMessageFromByteBuffer(m));
            case 7:
                PieceMessage pieceMessage = new PieceMessage();
                return (pieceMessage.getMessageFromByteBuffer(m));
            case 8:
                CancelMessage cancelMessage = new CancelMessage();
                return (cancelMessage.getMessageFromByteBuffer(m));
            default:
                return null;
        }
    }

    /**
     * create byte array method for a message
     *
     * @return bytebuffer
     */
    public abstract ByteBuffer createByteArray();

    /**
     * find payload in byte array
     *
     * @param m array of byte received
     * @return a message with the information contained in the array of bytes
     */
    public abstract Message getMessageFromByteArray(byte[] m);

    /**
     * find payload in byte buffer
     *
     * @param m byte buffer received
     * @return a message with the information contained in the array of bytes
     */
    public abstract Message getMessageFromByteBuffer(ByteBuffer m);

    public abstract int getSize();

    public ByteBuffer getBuf() {
        return buf;
    }

    public TypeMessage getTypeMessage() {
        return typeMessage;
    }

    public void setTypeMessage(TypeMessage m) {
        typeMessage = m;
    }
}
