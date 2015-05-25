package gr.spiritinlife.andsse;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AndSSEEventSource implements   Handler.Callback{

    /**
     * Reference to the {@link AndSSE AndSSE} thread that handles
     * the http connection and parsing of the responses
     */
    private final AndSSE andSSE;

    /**
     * A HashMap that holds all eventListeners defined by the user.
     * The key of the hashmap is the event name and the value is the {@link IEventCallback IEventCallback} which defines the onEvent function
     */
    private HashMap<String,IEventCallback> eventListeners;

    /**
     * This is the handler used by the {@link AndSSE AndSSE} thread to inform the main thread about events
     */
    public static Handler uiHandler;

    /**
     * {@link URL URL} this holds the information about the url used for the server
     */
    private URL url;


    /**
     * This holds the state of the SSE
     * Starting from {@link EventSource#CLOSED closed}
     */
    short readyState = EventSource.CLOSED;

    /**
     * This holds the last event id
     * It will be used when client needs to get the messages from a point and after
     */
    String lastEventId = "";

    /**
     * reconnection time is how much time is used before reconnecting
     */
    int reconnectionTime;


    /**
     * This is used when a call to {@link #start() start()} has been made but the {@link AndSSE#mHandler handler's} looper is not yet ready
     * to receive messages. In that case we set that flag to true and wait for the {@link #onLooperPrepared() onLooperPrepared } to fire
     */
    private boolean doOpenWhenPossible = false;

    /**
     * This is used when a call to {@link #start() start()} and the {@link #onLooperPrepared() onLooperPrepared} has been called
     * so we can start the SSE loop right away
     */
    private boolean isLooperPrepared = false;



    /**
     *
     * @param _url String
     * @param endpoint String
     * @param port Integer
     * @throws MalformedURLException
     */
    public AndSSEEventSource(String _url,String endpoint,int port)
            throws MalformedURLException {

        // create the handler that is used to get messages from AndSSE thread
        uiHandler = new Handler(this);

        // Create the URL object
        url = new URL("http",_url,port,endpoint);

        Log.i("Connecting",this.url.toString());

        // create and start the thread
        andSSE = new AndSSE(url);
        andSSE.start();

        // create the HashMap
        eventListeners = new HashMap<>();

    }

    /**
     * Call this when you want to start the server sent events listening loop
     */
    public void start() { open(); }

    /**
     * Gets the last event's id
     * @return String
     */
    public String getLastEventId() {
        return lastEventId;
    }





    //---------------------------------------//---------------------------------------//


    //---------------------------------------//---------------------------------------//
    //       Methods that handle and route the events from the AndSSE thread         //


    /**
     * AndSSE will send messages when data comes in and wants to pass them in the ui
     * @param msg {@link Message Message}
     * @return boolean false if not handled , true if handled
     */
    @Override
    public boolean handleMessage(Message msg) {
        //here we are in the ui thread we can call onMessage from bundle of message
        switch ( msg.what ) {
            case EventSource.EVENT :
                onMessage((AndEvent) msg.peekData().getParcelable("event"));
                break;
            case EventSource.ERROR :
                onError();
                break;
            case EventSource.OPEN :
                onOpen();
                break;
            case EventSource.RESTABLISH :
                onRestablish(msg.arg1);
                break;
            case EventSource.LOOOPER_READY :
                onLooperPrepared();
                break;
            default:
                return false;
        }
        return true;
    }


    /**
     * Call this from your activity to add an event listener
     * @param eventName String
     * @param callback {@link IEventCallback IEventCallback interface that defines an event method}
     */
    public void addEventListener(String eventName,IEventCallback callback){
        eventListeners.put(eventName, callback);
    }


    /**
     *
     * @param event {@link AndEvent AndEvent}
     */
    private void onMessage(final AndEvent event) {
        lastEventId = event.id;
        final IEventCallback cb = eventListeners.get(event.getEventName());
        if ( cb != null ) {
            cb.onEvent(event);
        }
    }


    /**
     * This is called by the {@link #handleMessage(Message) handleMessage(Message) } when the
     * {@link EventSource#LOOOPER_READY LOOPER_READY } is called by the AndSSE thread.
     * This will indicate that the AndSSE's handler is ready. In case of start has been called before that
     * then the {@link #doOpenWhenPossible } should be set and we should start the SSE loop
     */
    public void onLooperPrepared() {
        isLooperPrepared = true;
        if (doOpenWhenPossible)
            andSSE.mHandler.sendEmptyMessage(AndSSE.START);
    }


    /**
     * This is called by the {@link #handleMessage(Message) handleMessage(Message) } when the
     * {@link EventSource#OPEN OPEN } is called by the AndSSE thread
     */
    private void onOpen() {
        Log.i("EventSource", "OPENED");
        readyState = EventSource.OPEN;
    }

    /**
     * This is called by the {@link #handleMessage(Message) handleMessage(Message) } when the
     * {@link EventSource#ERROR ERROR } is called by the AndSSE thread
     * Attention : IF readyState is {@link EventSource#RESTABLISH RESTABLISH } then we will try to reconnect after {@link #reconnectionTime reconnectionTime ms}
     *             else we will CLOSE for ever
     */
    private void onError() {
        if ( readyState == EventSource.RESTABLISH ) {
            Log.i("EventSource", "Reconnecting");
            Runnable taskOpen = new Runnable() {
                public void run() {
                    if (readyState == EventSource.RESTABLISH)
                        open();
                }
            };

            Executors.newSingleThreadScheduledExecutor().schedule(taskOpen, reconnectionTime,
                    TimeUnit.MILLISECONDS);
        }
        else {
            Log.i("EventSource", "Closed");
            readyState = EventSource.CLOSED;
        }

    }

    /**
     * This is called by the {@link #handleMessage(Message) handleMessage(Message) } when the
     * {@link EventSource#RESTABLISH RESTABLISH } is called by the AndSSE thread
     */
    public void onRestablish(int reconnectionMs) {
        readyState = EventSource.RESTABLISH;
        reconnectionTime = reconnectionMs;
        // close stream and everything
        close();
    }


    //---------------------------------------//---------------------------------------//



    //---------------------------------------//---------------------------------------//
    //              Helpers methods that user AndSee Handler to pass events           //


    /**
     * Send a kill event to the AndSee loop to terminate everything , including thread
     */
    private void terminate() {
        // if all well this will call on error and close the asynctask
        Log.i("EventSource", "Disconnect");
        andSSE.mHandler.sendEmptyMessage(AndSSE.KILL);
    }

    /**
     * Send a Stop event to the AndSee loop
     */
    private void close() {
        // if all well this will call on error and close the asynctask
        Log.i("EventSource", "Disconnect");
        andSSE.mHandler.sendEmptyMessage(AndSSE.STOP);
    }

    /**
     * Send an open event to  AndSee to start the loop
     */
    private void open() {
        if (readyState == EventSource.CLOSED) {
            readyState = EventSource.CONNECTING;
            Log.i("EventSource", "Connecting");
            if (isLooperPrepared)
                andSSE.mHandler.sendEmptyMessage(AndSSE.START);
            else
                doOpenWhenPossible = true;
        }
    }
    //---------------------------------------//---------------------------------------//


    //---------------------------------------//---------------------------------------//
    //                                 AndSSE Lifecycle                               //

    /**
     * This should be called from the activities onResume
     */
    public void onResume() {
        if (isClosed())
            open();
    }

    /**
     * This should be called from the activities onPause
     */
    public void onPause() {
        if (isOpen())
            close();
    }

    /**
     * This should be called from the activities onDestroy
     */
    public void onDestroy() {
        close();
        eventListeners.clear();
    }

    //---------------------------------------//---------------------------------------//


    //---------------------------------------//---------------------------------------//
    //               Helper methods that give back the sate of the SSE                //


    /**
     *
     * @return boolean returns true if sse's state is closed
     */
    public boolean isClosed() {
        return readyState == EventSource.CLOSED;
    }

    /**
     *
     * @return boolean returns true if sse's state is open
     */
    public boolean isOpen() {
        return readyState == EventSource.OPEN;
    }

    /**
     *
     * @return boolean returns true is sse's state is connecting
     */
    public boolean isConnecting() {
        return readyState == EventSource.CONNECTING;
    }

    //---------------------------------------//---------------------------------------//

}
