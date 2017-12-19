package fiu.com.skillcourt.ui.pads;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.adapters.PadViewAdapter;
import fiu.com.skillcourt.connection.ConnectionService;
import fiu.com.skillcourt.ui.base.BaseActivity;

/**
 * Created by Joosshhhh on 11/7/17.
 */

public class PadConfigActivity extends BaseActivity {
    private static final String TAG = PadConfigActivity.class.getSimpleName();
    private boolean mBounded = false;
    private ConnectionService mConnectionService;
    private GridView mGridView;
    private PadViewAdapter mPadViewAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_pads);
        Intent intent = new Intent(this, ConnectionService.class);
        if (!mBounded) {
            bindService(intent, mConnection, BIND_AUTO_CREATE);
        }
        mGridView = findViewById(R.id.padGridView);


        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {

                // This tells the GridView to redraw itself
                // in turn calling your BooksAdapter's getView method again for each cell
                if(mPadViewAdapter.getCurrentColor().equalsIgnoreCase("white")) {
                    mPadViewAdapter.changeColor(view, "blue");
                    mPadViewAdapter.notifyDataSetChanged();
                }else{
                    mPadViewAdapter.changeColor(view, "white");
                    mPadViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mConnectionService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "binding service");
            mBounded = true;
            ConnectionService.LocalBinder mLocalBinder = (ConnectionService.LocalBinder) service;
            mConnectionService = mLocalBinder.getService();
            mPadViewAdapter = new PadViewAdapter(PadConfigActivity.this, mConnectionService);
            mGridView.setAdapter(mPadViewAdapter);
        }
    };
}
