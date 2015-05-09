package gr.spiritinlife.andsse;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AndSSEEventSource implements IEventSource {

    // ready states
    public static final short CONNECTING = 0;
    public static final short OPEN = 1;
    public static final short CLOSED = 2;

    // not in rfc standard
    public static final short RECONECTING = 3;


    private final AndSSE andSSE;

    //absolute url
    String url;

    short readyState;

    String lastEventId = "";

    //in ms
    int reconnectionTime;

    private final LocalBroadcastManager lbm;

    private SSENetwork mNetwork;

    public AndSSEEventSource(URL _url, Context _context) {
        this.url = _url.toString();
        lbm = LocalBroadcastManager.getInstance(_context);
        andSSE = new AndSSE(_url, this);
    }


    private class SSENetwork  extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                andSSE.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }



    public void close() {
        // if all well this will call on error and close the asynctask
        Log.i("EventSource", "Disconnect");
        andSSE.disconnect();
    }

    public void open() {
        Log.i("EventSource", "Connecting");
        readyState = CONNECTING;
        mNetwork = new SSENetwork();
        mNetwork.execute();
    }





    public boolean isClosed() {
        return readyState == CLOSED;
    }

    public boolean isOpen() {
        return readyState == OPEN;
    }

    public boolean isConnecting() {
        return readyState == CONNECTING;
    }


    @Override
    public void onOpen() {
        Log.i("EventSource", "OPENED");
        readyState = OPEN;
    }

    @Override
    public void onMessage(AndEvent event) {
        Log.e("DWDWDW",""+event.id);
        lastEventId = event.id;
        lbm.sendBroadcast(new Intent(event.name).putExtra("data", event.data));
    }

    @Override
    public void onError() {

        if (!mNetwork.isCancelled()) {
            mNetwork.cancel(true);
        }

        if ( readyState == RECONECTING ) {
            Log.i("EventSource", "Reconnecting");
            Runnable taskOpen = new Runnable() {
                public void run() {
                    if (readyState == RECONECTING)
                        open();
                }

            };

            Executors.newSingleThreadScheduledExecutor().schedule(taskOpen, reconnectionTime,
                    TimeUnit.MILLISECONDS);
        }
        else {
            Log.i("EventSource", "Closed");
            readyState = CLOSED;
        }
    }

    @Override
    public void onRestablish(int reconnectionMs) {
        // close stream and everything
        close();
        readyState = RECONECTING;
        reconnectionTime = reconnectionMs;
    }

    @Override
    public String getLastEventId() {
        return lastEventId;
    }

}
