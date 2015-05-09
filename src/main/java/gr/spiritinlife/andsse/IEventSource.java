package gr.spiritinlife.andsse;

public interface IEventSource {

    public void onOpen();
    public void onMessage(AndEvent event);
    public void onError();

    // those are not part of the rfc interface
    public void onRestablish(int reconnectionMs);
    String getLastEventId();
}
