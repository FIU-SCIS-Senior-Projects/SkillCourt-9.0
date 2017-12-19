package fiu.com.skillcourt.ui.main;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.connection.ConnectionService;
import fiu.com.skillcourt.ui.base.BaseActivity;
import fiu.com.skillcourt.ui.game.creategame.CreateGameActivity;
import fiu.com.skillcourt.ui.statistics.StatsFragment;

public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getName();
    private final static String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private BottomNavigationView mBottomNav;
    private TextView mPadConnected;
    public ConnectionService mConnectionService;
    public boolean mBounded = false;

    FirebaseDatabase mFirebaseDatabase;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    DatabaseReference mUserDatabaseReference;
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mUsersRef = mRootRef.child("users");
    DatabaseReference mUserIDref = mUsersRef.child(user.getUid());
    DatabaseReference mRoleRef = mUserIDref.child("role");

    IntentFilter connectionIntentFilter;
    IntentFilter padConnectionIntentFilter;
    ConnectionReceiver connectionReceiver;
    PadConnectionReceiver padConnectionReceiver;

    Fragment selectedFragment = null;
    String tag = "";
    String text = "0";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserDatabaseReference = mFirebaseDatabase.getReference().child("users").child(user.getUid());
        mRoleRef = mUserDatabaseReference.child("role");
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        TextView title = myToolbar.findViewById(R.id.toolbar_title);
        title.setText(myToolbar.getTitle());
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setNavigationIcon(R.drawable.ic_account_circle_black_24px);
        mBottomNav = findViewById(R.id.bottom_nav);
        if (!mRoleRef.equals("coach")) {
            mBottomNav.getMenu().removeItem(R.id.coaching);
        } else {
            mBottomNav.getMenu().removeItem(R.id.stats);
        }
        BottomNavigationViewHelper.disableShiftMode(mBottomNav);

        connectionIntentFilter = new IntentFilter();
        connectionIntentFilter.addAction(CONNECTIVITY_ACTION);
        connectionReceiver = new ConnectionReceiver();

        padConnectionIntentFilter = new IntentFilter();
        padConnectionIntentFilter.addAction(ConnectionService.PadConnectionReceiver.PAD_CONNECTION_CHANGE);
        padConnectionReceiver = new PadConnectionReceiver();

        /*
         This is for the initial start up of the activity
         If the device is connected to a network and the service has not
         been bounded or started (which means executed by the broadcast connectionReceiver below)
         then start it and bound the service else alert user
        */
        if (isConnected() && !mBounded) {

            Log.i(TAG, "Device is connected to a network");

            //start server socket and broadcasting nsd service
            Intent intent = new Intent(this, ConnectionService.class);
            startService(intent);
            /*
             Reason behind binding the service also is because
             this activity needs to access methods of the service
             when the activity is alive
            */
            bindService(intent, mConnection, BIND_AUTO_CREATE);
        } else {
            Log.e(TAG, "Device is not connected to a network");
            Toast.makeText(this, "You must connect to a network to start playing.", Toast.LENGTH_LONG).show();

        }

        mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                for (int i = 0; i < mBottomNav.getMenu().size(); i++) {
                    MenuItem menuItem = mBottomNav.getMenu().getItem(i);
                    boolean isChecked = menuItem.getItemId() == item.getItemId();
                    menuItem.setChecked(isChecked);
                }
                switch (item.getItemId()) {
                    case R.id.home:
                        if (checkCurrentFragment("HOME_FRAG")) {
                            selectedFragment = MainDashboardFragment.newInstance();
                        }
                        break;
                    case R.id.stats:
                        if (checkCurrentFragment("STATS_FRAG")) {
                            selectedFragment = StatsFragment.newInstance();
                        }
                        break;
                    case R.id.sequences:
                        if (checkCurrentFragment("SEQS_FRAG")) {
                            selectedFragment = MainDashboardFragment.newInstance();
                        }
                        break;
                    case R.id.coaching:
                        if (checkCurrentFragment("COACH_FRAG")) {
                            selectedFragment = MainDashboardFragment.newInstance();
                        }
                        break;
                    case R.id.more:
                        if (checkCurrentFragment("MORE_FRAG")) {
                            selectedFragment = MainDashboardFragment.newInstance();
                        }
                        break;
                }
                if (selectedFragment != null) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_frame_layout, selectedFragment, tag);
                    transaction.commit();
                    return true;
                }
                return false;


            }
        });

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_frame_layout, MainDashboardFragment.newInstance(), "HOME_FRAG");
            transaction.commit();
            mBottomNav.getMenu().getItem(0).setChecked(true);
        }

    }

    private boolean checkCurrentFragment(String tag) {
        Fragment currentFrag = getSupportFragmentManager().findFragmentByTag(tag);
        if (currentFrag == null || !currentFrag.isVisible()) {
            this.tag = tag;
            return true;
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mPadConnected = (TextView) menu.findItem(R.id.pads_connected).getActionView();
        mPadConnected.setText(getUpdatedPadConnectedText());
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(connectionReceiver, connectionIntentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(padConnectionReceiver, padConnectionIntentFilter);
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectionReceiver);
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Binding service disconnected");
            mBounded = false;
            mConnectionService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Binding service connected");
            mBounded = true;
            ConnectionService.LocalBinder mLocalBinder = (ConnectionService.LocalBinder) service;
            mConnectionService = mLocalBinder.getService();
            // Pads connected will be updated via broadcast connectionReceiver when a pad connects
            // This is just for initial launch of this activity
            if (mPadConnected != null)
                mPadConnected.setText(getUpdatedPadConnectedText());
        }
    };


    //Used to check a network change, like connecting to wifi or being disconnected
    private class ConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //If the action is a network change state
            if (intent.getAction().equals(CONNECTIVITY_ACTION)) {
                /*
                 Check if the device is connected to a network this handles the case where the user
                 just pulls down the notification screen and hits the wifi button, the reason why
                 this is here is because in this case the activity doesn't get destroyed just
                 paused so therefore the same code in the onCreate method doesn't get executed so
                 we need to execute it here to start the nsd and server service
                */
                if (isConnected()) {
                    Log.i(TAG, "Device is connected to a network");

                    //start server socket and broadcasting nsd service
                    Intent i = new Intent(context, ConnectionService.class);
                    context.startService(i);
                    bindService(i, mConnection, BIND_AUTO_CREATE);
                } else {
                    Log.e(TAG, "Device disconnected from a network");
                    Intent i = new Intent(context, ConnectionService.class);
                    if (mBounded) {
                        unbindService(mConnection);
                        mBounded = false;
                    }
                    stopService(i);
                }
            }
        }
    }

    private class PadConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectionService.PadConnectionReceiver.PAD_CONNECTION_CHANGE)) {
                Log.i(TAG, "PAD CONNECTION CHANGE");
                mPadConnected.setText(getUpdatedPadConnectedText());
            }
        }
    }


    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //If there is a network available and the device is connected or connecting then were good
        return (connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isAvailable()
                && connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting());

    }

    private String getUpdatedPadConnectedText() {
        if (mBounded) {
            Log.i(TAG, "Get updated pad(s) connected: " + mConnectionService.getPadsConnected());
            text = mConnectionService.getPadsConnected() + "";
        }
        return text;
    }
}
