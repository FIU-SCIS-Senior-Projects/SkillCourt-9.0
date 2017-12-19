package fiu.com.skillcourt.ui.game.startgame;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.adapters.PadViewAdapter;
import fiu.com.skillcourt.connection.ConnectionService;
import fiu.com.skillcourt.ui.base.BaseActivity;
import fiu.com.skillcourt.ui.game.gameOver.GameOverActivity;

public class StartGameActivity extends BaseActivity {
    private static final String TAG = StartGameActivity.class.getSimpleName();
    private static final String PAD_HIT = "fiu.com.skillcourt.intent.action.PAD_HIT";
    private TextView mCountDown;
    private TextView mTimer;
    private TextView mGreenHits;
    private TextView mRedHits;
    private RelativeLayout mTimerContainer;
    private LinearLayout mHitsContainer;
    private String mGameMode;
    private long mGameTime;
    private ConnectionService mConnectionService;
    private GridView mGridView;
    private PadViewAdapter mPadViewAdapter;
    private boolean mBounded = false;
    private Game mGame;
    IntentFilter padHitIntentFilter;
    PadHitReceiver padHitReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Intent it = getIntent();
        mGameMode = it.getStringExtra("GAME_MODE");
        if (mGameMode == null)
            mGameMode = "Random";
        mGameTime = it.getLongExtra("GAME_TIME", 30);
        mHitsContainer = findViewById(R.id.hits_container);
        mGreenHits = findViewById(R.id.tv_green_hits);
        mRedHits = findViewById(R.id.tv_red_hits);
        mCountDown = findViewById(R.id.countDown);
        mTimer = findViewById(R.id.tvTimer);
        mTimerContainer = findViewById(R.id.timer_container);
        mGridView = findViewById(R.id.padGameGridView);

        updateTimerText(mGameTime);
        mHitsContainer.setVisibility(View.INVISIBLE);
        mTimerContainer.setVisibility(View.INVISIBLE);
        mGridView.setVisibility(View.INVISIBLE);

        Intent intent = new Intent(this, ConnectionService.class);
        if (!mBounded) {
            bindService(intent, mConnection, BIND_AUTO_CREATE);
        }
        padHitIntentFilter = new IntentFilter();
        padHitIntentFilter.addAction(PAD_HIT);
        padHitReceiver = new PadHitReceiver();
        new CountDownTimer(6000, 1000) {

            public void onTick(long millisUntilFinished) {
                //here you can have your logic to set text to edittext
                mCountDown.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                mCountDown.setText("READY");
                mGame = new Game(mGameMode, mGameTime, 0, 0, "FD4C14");
                mCountDown.setVisibility(View.INVISIBLE);
                mHitsContainer.setVisibility(View.VISIBLE);
                mTimerContainer.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.VISIBLE);
                mConnectionService.nsdConnection.tearDown(); //stop nsd broadcast
                Log.i(TAG, "Game time in seconds: " + mGameTime);
                mGame.start();
            }

        }.start();


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(padHitReceiver, padHitIntentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(padHitReceiver);
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
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
            mPadViewAdapter = new PadViewAdapter(StartGameActivity.this, mConnectionService);
            mGridView.setAdapter(mPadViewAdapter);
        }
    };

    private void updateTimerText(long seconds) {
        if (seconds >= 60) {
            int minute = (int) seconds / 60;
            seconds = seconds % 60;
            if (seconds < 10) {
                mTimer.setText(minute + ":0" + seconds);
            } else {
                mTimer.setText(minute + ":" + seconds);
            }
        } else {
            if (seconds < 10) {
                mTimer.setText("00:0" + seconds);
            } else {
                mTimer.setText("00:" + seconds);
            }
        }
    }

    public class Game extends Thread {

        private String mGameMode, mHitColor, mCurrentActivePadUUID;
        private long mGameTime;
        private int mPadLightUpTime, mPadLightUpTimeDelay, mTotalDelay, mTotalCount, mTotalPoints;
        private int mCurrentPadLit, mLastPadLit;
        public int mHitCount, hits;
        public int mMissCount, miss;
        public boolean isHit;
        public ConcurrentHashMap<String, Integer> padHits = new ConcurrentHashMap<>();
        public ConcurrentHashMap<String, Integer> padMisses = new ConcurrentHashMap<>();


        public Game(String gameMode, long gameTime, int padLightUpTime, int padLightUpTimeDelay, String hitColor) {
            mGameMode = gameMode;
            mGameTime = gameTime * 1000; //transfer into seconds
            mPadLightUpTime = padLightUpTime * 1000;
            mPadLightUpTimeDelay = padLightUpTimeDelay * 1000;
            mTotalDelay = mPadLightUpTime + mPadLightUpTimeDelay;
            mHitColor = hitColor;
            int i = 0;
            for (ConnectionService.OutputThread outputThread : mConnectionService.getActiveOutputSocketThreads()) {
                outputThread.setMessage("2"); //send a game is starting so start sensor reading
                padHits.put("Pad " + i, 0);
                padMisses.put("Pad " + i, 0);
                i++;
                //outputThread.setMessage(padLightUpTime + "");
                /*
                try {
                    //outputThread.setMessage("5");
                   //outputThread.setMessage(mHitColor);
                } catch (Exception e) {
                    Log.e(TAG, "Could not send pad color change");
                    e.printStackTrace();
                }
                */
            }
        }


        public String getCurrentActivePadUUID() {
            return mCurrentActivePadUUID;
        }

        public void setCurrentActivePadUUID(String currentActivePadUUID) {
            mCurrentActivePadUUID = currentActivePadUUID;
        }

        public String getHitColor() {
            return mHitColor;
        }


        @Override
        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                //reinitialize just in case
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new CountDownTimer(mGameTime, 1000) {
                            public void onTick(long millisUntilFinished) {
                                //here you can have your logic to set text to edittext
                                long seconds = millisUntilFinished / 1000;
                                updateTimerText(seconds);
                            }

                            public void onFinish() {
                                mTimer.setText("00:00");
                                Intent intent = new Intent(StartGameActivity.this, GameOverActivity.class);
                                intent.putExtra("GAME_MODE", mGameMode);
                                intent.putExtra("GAME_TIME", mGameTime / 1000);
                                intent.putExtra("HIT_COUNT", mHitCount);
                                intent.putExtra("MISS_COUNT", mMissCount);
                                intent.putExtra("SCORE", mTotalPoints);
                                startActivity(intent);
                                finish();
                            }
                        }.start();

                        mGreenHits.setText("0");
                        mRedHits.setText("0");
                    }
                });

                mHitCount = 0;
                hits = 0;
                mMissCount = 0;
                miss = 0;
                mTotalCount = 0;
                mTotalPoints = 0;
                mLastPadLit = -1;
                ConnectionService.OutputThread outputThread;
                ConnectionService.InputThread inputThread;
                while (mGameTime > System.currentTimeMillis() - startTime) {
                    isHit = false;
                    switch (mGameMode) {
                        case "Random":
                            if (mConnectionService.getActiveOutputSocketThreads().size() > 2) {
                                //check to make sure the same pad doesn't light up twice in a row
                                while (true) {
                                    mCurrentPadLit = (int) (Math.random() * mConnectionService.getActiveOutputSocketThreads().size());
                                    if (mCurrentPadLit == mLastPadLit) continue;
                                    Log.i(TAG, "Last Light up #" + mLastPadLit);
                                    Log.i(TAG, "Light up #" + mCurrentPadLit);
                                    mLastPadLit = mCurrentPadLit;
                                    break;

                                }
                            } else {
                                mCurrentPadLit = (int) (Math.random() * mConnectionService.getActiveOutputSocketThreads().size());
                                Log.i(TAG, "Light up #" + mCurrentPadLit);
                            }
                            break;
                        case "Sequence":
                            break;
                    }

                    outputThread = mConnectionService.getActiveOutputSocketThreads().get(mCurrentPadLit);
                    inputThread = mConnectionService.getActiveInputSocketThreads().get(mCurrentPadLit);
                    outputThread.setMessage("3"); //send light up command
                    setCurrentActivePadUUID(inputThread.getUuid()); //get the currently lit pad's uuid
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPadViewAdapter.changeColor(mGridView.getChildAt(mCurrentPadLit), "blue");
                            mPadViewAdapter.notifyDataSetChanged();
                        }
                    });

                    //if the player specified a time that the pad stays lit
                    if (mPadLightUpTime != 0) {
                        try {
                            Log.i(TAG, "Sleeping...");
                            Thread.sleep(mTotalDelay);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        //loop until a hit is registered or the game is over
                        while (!isHit) {
                            if (!(mGameTime > System.currentTimeMillis() - startTime)) {
                                break;
                            }
                        }

                        if (mPadLightUpTimeDelay != 0) {
                            try {
                                Log.i(TAG, "Sleeping...");
                                Thread.sleep(mPadLightUpTimeDelay);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }

                Log.i(TAG, "Hit count: " + mHitCount);
                Log.i(TAG, "Miss count: " + mMissCount);

                for (Map.Entry<String, Integer> e : padHits.entrySet()) {
                    Log.i(TAG, e.getKey() + " hit count: " + e.getValue());
                }
                for (Map.Entry<String, Integer> e : padMisses.entrySet()) {
                    Log.i(TAG, e.getKey() + " miss count: " + e.getValue());
                }
                mTotalCount = mHitCount + mMissCount;
                if (mTotalCount != 0) {
                    hits = (int) (((double) mHitCount / mTotalCount) * 100);
                    miss = (int) (((double) mMissCount / mTotalCount) * 100);
                }
                for (ConnectionService.OutputThread ot : mConnectionService.getActiveOutputSocketThreads()) {
                    ot.setMessage("4"); //send game over command
                    Log.i(TAG, "Sending game over");
                }


                Log.i(TAG, "Game Over");
                Log.i(TAG, "Hits: " + hits + "%");
                Log.i(TAG, "Misses: " + miss + "%");
                Log.i(TAG, "Total: " + mTotalCount);
                Log.i(TAG, "Total Points: " + mTotalPoints);

                //restart nsd broadcast
                mConnectionService.nsdConnection.registerService(mConnectionService.getLocalPort());
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

    }

    private class PadHitReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if a pad was hit then see if it was the correct pad by checking the uuid that sent
            //the hit broadcast vs the uuid that was set after sending the light up command in game
            if (intent.getAction().equals(PAD_HIT)) {
                String key = "Pad " + mGame.mCurrentPadLit;
                if (mGame.getCurrentActivePadUUID().equals(intent.getStringExtra("PAD_HIT"))) {
                    mGame.mHitCount++;
                    if (mGame.padHits.containsKey(key)) {
                        mGame.padHits.put(key, mGame.padHits.get(key) + 1);
                    }
                    mGame.mTotalPoints = mGame.mTotalPoints + 2;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPadViewAdapter.changeColor(mGridView.getChildAt(mGame.mCurrentPadLit), "white");
                            mPadViewAdapter.notifyDataSetChanged();
                        }
                    });
                    mGame.isHit = true;
                    mGreenHits.setText(mGame.mHitCount + "");
                } else {

                    mGame.mMissCount++;
                    if (mGame.padMisses.containsKey(key)) {
                        mGame.padMisses.put(key, mGame.padMisses.get(key) + 1);
                    }
                    mGame.mTotalPoints--;
                    //mGame.isHit = true;
                    mRedHits.setText(mGame.mMissCount + "");
                }

            }
        }
    }
}
