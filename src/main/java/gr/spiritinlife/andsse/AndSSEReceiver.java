package gr.spiritinlife.andsse;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public abstract class AndSSEReceiver extends BroadcastReceiver {

    public IntentFilter eventName;

    public AndSSEReceiver(String _eventName) {
        super();
        this.eventName = new IntentFilter(_eventName);
    }


}
