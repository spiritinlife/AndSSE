package gr.spiritinlife.andsse;


import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class AndSSE  {

    private static final int CONNECTION_TIMEOUT = 3000;
    private volatile  boolean isRunning = false;

    private IEventSource mEventSource;


    private HttpURLConnection con;

    private URL url;
    /**
     * @param url the URL of this connection.
     * @see java.net.URL
     * @see java.net.URLConnection
     */
    protected AndSSE(URL url, IEventSource eventSource) {

        this.url = url;
        mEventSource = eventSource;
    }


    private void setConnectionProperties() {
        // set false for caching
        con.setUseCaches(false);

        con.setConnectTimeout(CONNECTION_TIMEOUT);

        // read infinite
        //setReadTimeout(0);

        // we get responses
        con.setDoInput(true);

        // we do not send anything back into a body like post
        con.setDoOutput(false);

        // connection should follow redirects
        con.setInstanceFollowRedirects(true);

        // set  Last-Event-ID header if needed
        if ( !mEventSource.getLastEventId().isEmpty() && con != null) {
            Log.e("Last event id", mEventSource.getLastEventId());
            con.setRequestProperty("last-event-id", mEventSource.getLastEventId());
        }

    }


    private void start() throws Exception {
        con = (HttpURLConnection) url.openConnection();

        try {
            setConnectionProperties();


            // we announce the connection
            mEventSource.onOpen();

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
                        event.data = "";
                        event.name = AndEvent.ON_MESSAGE;
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
                                    mEventSource.onRestablish(reconnectionTime);
                                }
                            default:
                                continue;
                        }

                    }
                }


                in.close();

            }

        } catch (IOException e) {
            // this sets the eventsource to CLOSED
            mEventSource.onError();
        }
        finally {
            con.disconnect();
            // this sets the eventsource to CLOSED
            mEventSource.onError();
        }

    }


    private void dispatchEvent(AndEvent event) {
        if (event != null && !event.data.isEmpty()) {
            mEventSource.onMessage(event);
        }
    }


    public void disconnect() {
        isRunning = false;
    }


    public void connect() throws IOException {
        isRunning = true;
        try {
            start();
        } catch (Exception e) {
            mEventSource.onError();
            e.printStackTrace();
        }
    }



}