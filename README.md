# AndSSE
Server Sent Events for Android !

### Use
This library is on maven .
In order to use it you need to add it as a dependency in your project.<br>
#### compile 'gr.spiritinlife:andsse:0.0.1'

### HOW-TO
The preferred way to use the library at this moment is to create a singleton adapter that creates the AndSSEEventSource which is the front end
of the tcp connection for our library.
eg.
<pre><code>
public class AndSSEAdapter {

    private static final String SERVER_ENDPOINT = "192.168.2.20";
    /**
    *   This is the endpoint that the sends the events
    */
    private static final String SSE_ENDPOINT = "/events/";

    private static AndSSEEventSource mAndSSEEventSource;

    public static AndSSEEventSource getSSEAdapter(Context _context) throws MalformedURLException {
        if (mAndSSEEventSource == null) {
            mAndSSEEventSource = new AndSSEEventSource(SERVER_ENDPOINT,SSE_ENDPOINT,8000);
        }

        return mAndSSEEventSource;
    }
}
</code></pre>

After that we can addEventListeners to any activity/fragment we want as follows

<pre><code>

try {
    mAndSSEEventSource = AndSSEAdapter.getSSEAdapter(this);

    mAndSSEEventSource.addEventListener("mouse",new IEventCallback() {
        @Override
        public void onEvent(AndEvent event) {

            String data = event.getData();
            ...

        }
    });

    mAndSSEEventSource.start();

   } catch (MalformedURLException e) {
     e.printStackTrace();
   }


</code></pre>

<b> Important </b> you need to handle the lifecycle of the AndSSEEventSource thread.
In order to achieve this you need to add the following
<pre><code>
@Override
protected void onResume() {
    super.onResume();
    if (mAndSSEEventSource != null)
        mAndSSEEventSource.onResume();
}


@Override
protected void onPause() {
    super.onPause();
    if (mAndSSEEventSource != null)
        mAndSSEEventSource.onPause();
}



@Override
protected void onDestroy() {
    super.onDestroy();
    if (mAndSSEEventSource != null)
        mAndSSEEventSource.onDestroy();
}
</code></pre>

### Philosophy
+   Keep as close as possible to the javascript experience
+   Keep as close as possible to the the RFC
+   Have no dependency

### TO-DO
There are a lot of things that need to be done in order for this library to be safe to use .
The most important are :
+   Add example project
+   Add example node server for testing
+   Docs
+   Library assumes that server is "speaking" SSE which is not what RFC describes .
    We should add ways to check if communication with server can be handled
+   Test for cases that the SSE parser is not functioning correctly
+   Maybe there is better/safer way to handle tcp connections and communication between threads



