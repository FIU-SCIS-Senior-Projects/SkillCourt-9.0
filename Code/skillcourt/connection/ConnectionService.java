package fiu.com.skillcourt.connection;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by Josh on 9/24/2017.
 */

public class ConnectionService extends Service {

    private static final String TAG = ConnectionService.class.getName();
    private static final String PAD_CONNECTION = "fiu.com.skillcourt.intent.action.PAD_CONNECTION";
    private static final String PAD_HIT = "fiu.com.skillcourt.intent.action.PAD_HIT";

    private ServerThread mServerThread;
    private ServerSocket mServerSocket;
    private int mLocalPort;
    private PadConnectionReceiver mPadConnectionReceiver = new PadConnectionReceiver();
    private int mPadsConnected = 0;

    public NsdConnection nsdConnection;

    public List<InputThread> getActiveInputSocketThreads() {
        return activeInputSocketThreads;
    }

    public List<OutputThread> getActiveOutputSocketThreads() {
        return activeOutputSocketThreads;

    }

    private List<InputThread> activeInputSocketThreads = new CopyOnWriteArrayList<>();
    private List<OutputThread> activeOutputSocketThreads = new CopyOnWriteArrayList<>();

    private IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //So activities can access the methods inside this service
    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    @Override
    public void onCreate() {

        //Create and start the thread that'll handle creating a server socket and registering nsd
        mServerThread = new ServerThread();
        mServerThread.setName("ServerThread");
        mServerThread.start();

        IntentFilter padConnectionIntentFilter = new IntentFilter();
        padConnectionIntentFilter.addAction(PAD_CONNECTION);
        registerReceiver(mPadConnectionReceiver, padConnectionIntentFilter);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        unregisterReceiver(mPadConnectionReceiver);
        mServerThread.interrupt();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //broadcast a total disconnect
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public enum COMMANDS {

    }

    public int getPadsConnected() {
        return mPadsConnected;
    }

    public void setPadsConnected(int padsConnected) {
        mPadsConnected = padsConnected;
    }

    public int getLocalPort() {
        return mLocalPort;
    }

    public void setLocalPort(int localPort) {
        mLocalPort = localPort;
    }



    private class ServerThread extends Thread {


        @Override
        public void interrupt() {
            super.interrupt();
            for (OutputThread thread : activeOutputSocketThreads) {
                thread.interrupt();
            }
            activeOutputSocketThreads.clear();
            for (InputThread thread : activeInputSocketThreads) {
                thread.interrupt();
            }
            activeInputSocketThreads.clear();
            nsdConnection.tearDown();
            Log.i(TAG, "Closed nsd broadcast");
            try {
                mServerSocket.close();
                Log.i(TAG, "Closed server socket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            try {
                // Since discovery will happen via Nsd, we don't need to care which port is
                // used. Just grab an available one and advertise it via Nsd.
                ServerSocket serverSocket = new ServerSocket(0);
                mServerSocket = serverSocket;
                nsdConnection = new NsdConnection(getApplicationContext());
                setLocalPort(serverSocket.getLocalPort());
                nsdConnection.registerService(mLocalPort);

                //Continuously look for clients to communicate to
                while (!Thread.currentThread().isInterrupted()) {
                    Log.i(TAG, "ServerSocket Created, awaiting connection");
                    Socket socket = serverSocket.accept();
                    Log.i(TAG, "Connection accepted " + socket.getInetAddress());
                    // Branch into separate IO threads (one for Input & one for Output)
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
                    InputThread inputThread = new InputThread(socket, input);
                    OutputThread outputThread = new OutputThread(socket, output);
                    //ConnectionCheckThread connectionCheckThread = new ConnectionCheckThread(socket);
                    inputThread.setName("InputThread");
                    inputThread.start();
                    outputThread.setName("OutputThread");
                    outputThread.start();
                    //connectionCheckThread.setName("ConnectionCheckThread");
                    //connectionCheckThread.start();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
                interrupt();
            } catch (Exception e) {
                e.printStackTrace();
                interrupt();
            }
        }


    }


    public class ConnectionCheckThread extends Thread {
        private Socket mSocket;
        private Iterator<OutputThread> itOutput = activeOutputSocketThreads.iterator();
        private Iterator<InputThread> itInput = activeInputSocketThreads.iterator();

        ConnectionCheckThread(Socket socket) {
            mSocket = socket;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(5 * 1000);
                Log.i(TAG, "Starting connection check thread");
                while (!Thread.currentThread().isInterrupted()) {
                    for (OutputThread thread : activeOutputSocketThreads) {
                        if (mSocket.isConnected()) {
                            thread.setMessage("-1");
                            Log.i(TAG, "Sleeping connection check thread");
                            Thread.sleep(10 * 1000);
                        } else {
                            while (itOutput.hasNext()) {
                                OutputThread ot = itOutput.next();
                                if (ot.getSocket() == mSocket) {
                                    itOutput.remove();
                                    ot.interrupt();
                                }
                            }

                            while (itInput.hasNext()) {
                                InputThread it = itInput.next();
                                if (it.getSocket() == mSocket) {
                                    itInput.remove();
                                    it.interrupt();
                                }
                            }

                            Log.i(TAG, "Disconnecting");
                            Thread.currentThread().interrupt();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error in check connection thread " + e);
                Thread.currentThread().interrupt();
            }
        }
    }


    public class InputThread extends Thread {
        private Socket mSocket;
        private BufferedReader mInput;
        private String uuid;

        InputThread(Socket socket, BufferedReader input) {
            mSocket = socket;
            mInput = input;
        }

        @Override
        public void interrupt() {
            super.interrupt();
            try {
                if (mSocket.isConnected()) {
                    mInput.close();
                    Log.i(TAG, "Closed input socket stream");
                    mSocket.close();
                    Log.i(TAG, "Closed input socket");
                }
                mPadsConnected--;
                Intent intent = new Intent(PAD_CONNECTION);
                sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public Socket getSocket() {
            return mSocket;
        }

        public String getUuid() {
            return uuid;
        }


        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (mSocket.isClosed()) {
                        interrupt();
                        Log.i(TAG, "Interrupting input socket thread");
                    }
                    String data = mInput.readLine();
                    if (data.length() > 1) {
                        uuid = data;
                    }
                    switch (data) {
                        case "0":
                            interrupt();
                            break;
                        case "1":
                            mPadsConnected++;
                            Intent connectIntent = new Intent(PAD_CONNECTION);
                            sendBroadcast(connectIntent);
                            Log.i(TAG, "Pad connected " + mPadsConnected);
                            //send broadcast that a pad is connected
                            activeInputSocketThreads.add(this);
                            break;
                        case "3":
                            Intent hitIntent = new Intent(PAD_HIT);
                            hitIntent.putExtra("PAD_HIT", uuid);
                            sendBroadcast(hitIntent);
                            break;
                    }

                    Log.i(TAG + " Input Thread", "Received: " + data);

                }

            } catch (Exception e) {
                e.printStackTrace();
                // Disconnection happened with this socket so send pad connection broadcast
                // Check to make sure it is fully closed before sending
                interrupt();
            }
        }
    }

    public class OutputThread extends Thread {
        private Socket mSocket;
        private OutputStreamWriter mOutput;
        private String mMessage;

        OutputThread(Socket socket, OutputStreamWriter output) {
            mSocket = socket;
            mOutput = output;
        }

        public void setMessage(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }

        public Socket getSocket() {
            return mSocket;
        }


        @Override
        public void interrupt() {
            super.interrupt();
            try {
                if (mSocket.isConnected()) {
                    mOutput.close();
                    Log.i(TAG, "Closed output socket stream");
                    mSocket.close();
                    Log.i(TAG, "Closed output socket");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                setMessage("1"); //for the initial communication, to verify we can send data
                while (!Thread.currentThread().isInterrupted()) {
                    if (!mMessage.isEmpty()) {
                        Log.i(TAG, "Sending message " + mMessage);
                        mOutput.write(mMessage, 0, mMessage.length());
                        mOutput.flush();
                        if (mMessage.equalsIgnoreCase("1")) {
                            activeOutputSocketThreads.add(this);
                        }
                        setMessage("");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                interrupt();
            }
        }
    }

    public class PadConnectionReceiver extends BroadcastReceiver {
        public static final String PAD_CONNECTION_CHANGE = "PAD_CONNECTION_CHANGE";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PAD_CONNECTION)) {
                Intent padConnectionIntent = new Intent(PAD_CONNECTION_CHANGE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(padConnectionIntent);
            }
        }
    }

}
