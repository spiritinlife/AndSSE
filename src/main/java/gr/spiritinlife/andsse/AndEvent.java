package gr.spiritinlife.andsse;

import android.os.Parcel;
import android.os.Parcelable;

public class AndEvent implements Parcelable{


    public static final String ON_MESSAGE = "ssemessage";

    String name;
    String data;
    String id;


    public AndEvent(Parcel in) {
        name = in.readString();
        data = in.readString();
        id = in.readString();
    }
    public AndEvent() {
        this.name = ON_MESSAGE;
        this.data = "";
    }

    public void setEventData( String _data) {

        if ( _data.endsWith("\n") )
           _data =  _data.substring(0,_data.length()-1);
        this.data = _data.trim();
    }

    public void setEventName( String eventName ) {
        this.name = eventName;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getData() {
        return data;
    }

    public String getEventName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(data);
        dest.writeString(id);
    }


    public static final Creator<AndEvent > CREATOR = new Creator<AndEvent >() {
        public AndEvent createFromParcel(Parcel in) {
            return new AndEvent (in);
        }
        public AndEvent [] newArray(int size) {
            return new AndEvent [size];
        }
    };
}
