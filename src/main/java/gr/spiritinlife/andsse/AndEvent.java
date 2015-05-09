package gr.spiritinlife.andsse;

public class AndEvent{
    public static final String ON_MESSAGE = "ssemessage";

    String name;
    String data;
    String id;

    public AndEvent() {
        this.name = ON_MESSAGE;
        this.data = "";
    }

    public void setEventData( String _data) {
        if ( _data.endsWith("\n") )
           _data =  _data.substring(0,_data.length()-1);
        this.data = _data + "\n";
    }

    public void setEventName( String eventName ) {
        this.name = eventName;
    }


    public void setId(String id) {
        this.id = id;
    }
}
