package gr.spiritinlife.andsse;

public interface EventSource {

    public static final short CONNECTING = 0x0;
    public static final short OPEN = 0x1;
    public static final short CLOSED = 0x2;
    public static final short EVENT = 0x3;
    public static final short ERROR = 0x4;
    public static final short RESTABLISH = 0x5;
    public static final short LOOOPER_READY = 0x6;


//    public void onOpen();
//    public void onMessage(AndEvent event);
//    public void onError();
//
//    // those are not part of the rfc interface
//    public void onRestablish(int reconnectionMs);
//    String getLastEventId();

}
