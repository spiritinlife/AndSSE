package gr.spiritinlife.andsse;


import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class AndSSE extends HandlerThread{

    private static final String TAG = "AndSSE";
    private static final int CONNECTION_TIMEOUT = 3000;


    /**
     * A value indicating whether the SSE loop is running or not
     */
    private volatile boolean isRunning = false;

    /**
     * The last event's id
     */
    private String lastEventId = "";

    /**
     *  This is the real connection of the SSE which brings data to us!
     */
    private HttpURLConnection con;

    /**
     * A reference to the url object
     */
    private URL url;

    /**
     * The handler used by the {@link AndSSEEventSource AndSSEEventSource } to pass to this thread events
     */
    public Handler mHandler;


    /**
     * Those are the two events that we understand.
     * They are used by the {@link AndSSEEventSource AndSSEEventSource } to handle the execution of the SSE loop
     */
    public static final int START = 1;
    public static final int STOP = 2;
    public static final int KILL = 3;


    /**
     * @param url the URL of this connection.
     * @see java.net.URL
     * @see java.net.URLConnection
     */
    protected AndSSE(URL url) {
        super("AndSSE_" + url.toString());
        this.url = url;
    }

    /**
     * This is where we create the handler.
     * When the handler is created we inform the  {@link AndSSEEventSource AndSSEEventSource } that we are ready to start two way communication
     * between this thread and the main thread
     */
    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // process incoming messages here
                switch (msg.what) {
                    case START :
                        Log.i(TAG,"Start");
                        startNetworking();
                        break;
                    case STOP :
                        Log.i(TAG,"Stop");
                        disconnect();
                        break;
                    case KILL :
                        Log.i(TAG,"KILL");
                        terminate();
                        break;
                }
            }
        };
        AndSSEEventSource.uiHandler.obtainMessage(EventSource.LOOOPER_READY).sendToTarget();
        super.onLooperPrepared();
    }


    /**
     * This is called when a {@link #START} message is given and we start the SSE loop
     */
    private void startNetworking() {
        try {
            con = (HttpURLConnection) url.openConnection();
            isRunning = true;
        } catch (Exception e) {
            onError();
            e.printStackTrace();
        }
        try {

            setConnectionProperties();


            // we announce the connection
            onOpen();

            // create a default event
            AndEvent event = new AndEvent();
            String inputLine;

            while(isRunning) {
                // SSE requires UTF-8
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                while ((inputLine = in.readLine()) != null && isRunning) {

                    // if line is empty dispatch event
                    if ( inputLine.isEmpty() ) {
                        dispatchEvent(event);

                        // reset event
                        event = new AndEvent();
                    }
                    // if line starts with colon ( : ) then ignore the line
                    else if ( inputLine.startsWith(":") ) {
                    }
                    // if line contains a colon ( : ) then
                    // Collect the characters on the line before the first U+003A COLON character (:), and let field be that string.
                    // Collect the characters on the line after the first U+003A COLON character (:), and let value be that string.
                    // If value starts with a U+0020 SPACE character, remove it from value
                    else if ( inputLine.contains(":") ) {

                        String fieldAndValue[] = inputLine.split(":");

                        if ( fieldAndValue.length != 2 )
                            throw new Exception("Could not process a field at input line :  " + inputLine);

                        switch (fieldAndValue[0]) {
                            case "event":
                                event.setEventName(fieldAndValue[1]);
                                break;
                            case "id":
                                event.setId(fieldAndValue[1]);
                            case "data":
                                event.setEventData(fieldAndValue[1]);
                                break;
                            case "retry":
                                int reconnectionTime = Integer.valueOf(fieldAndValue[1]);
                                if ( reconnectionTime >= 0 && reconnectionTime <= 9 ) {
                                    onRestablish(reconnectionTime);
                                }
                            default:
                                continue;
                        }

                    }
                }


                in.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            con.disconnect();
            // this sets the eventsource to CLOSED
            onError();
        }
    }


    /**
     * Used to announce the connection to the {@link AndSSEEventSource AndSSEEventSource}
     */
    private void onOpen() {
        AndSSEEventSource.uiHandler.sendEmptyMessage(EventSource.OPEN);
    }

    /**
     * Used to inform {@link AndSSEEventSource AndSSEEventSource} that an ERROR occured.
     * Error in SSE protocol does not always something bad. It essentially means that the connection is CLOSED
     */
    private void onError() {
        AndSSEEventSource.uiHandler.sendEmptyMessage(EventSource.ERROR);
    }

    /**
     * Used to inform {@link AndSSEEventSource AndSSEEventSource} that we need to reconnect.
     */
    private void onRestablish(int reconnectionTime) {
        Message.obtain(AndSSEEventSource.uiHandler,EventSource.ERROR,reconnectionTime,0).sendToTarget();
    }


    /**
     * Sets the properties of the connection
     * It also handles last event id if one exists
     */
    private void setConnectionProperties() {
        // set false for caching
        con.setUseCaches(false);

        con.setConnectTimeout(CONNECTION_TIMEOUT);

        // read infinite
        con.setReadTimeout(0);

        // we get responses
        con.setDoInput(true);

        // we do not send anything back into a body like post
        con.setDoOutput(false);

        // connection should follow redirects
        con.setInstanceFollowRedirects(true);

        // set  Last-Event-ID header if needed
        if ( !lastEventId.isEmpty() && con != null) {
            Log.e("Last event id", lastEventId);
            con.setRequestProperty("last-event-id", lastEventId);
        }

    }


    /**
     * Called fromt the SSE loop when a new event is retrieved
     * It essentially sends a message to the main thread handler of {@link AndSSEEventSource AndSSEEventSource}
     * @param _event {@link AndEvent AndEvent}
     */
    private void dispatchEvent(AndEvent _event) {
        if (_event != null && !_event.data.isEmpty()) {
            lastEventId = _event.id;
            Message message = Message.obtain();
            message.what = EventSource.EVENT;
            Bundle msgBundle = new Bundle();
            msgBundle.putParcelable("event", _event);
            message.setData(msgBundle);
            AndSSEEventSource.uiHandler.sendMessage(message);
        }
    }


    private void disconnect() {
        isRunning = false;
    }

    private void terminate() {
        isRunning = false;
        quit();
    }






}