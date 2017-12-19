package fiu.com.skillcourt.ui.game.gameOver;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import at.grabner.circleprogress.CircleProgressView;
import fiu.com.skillcourt.R;
import fiu.com.skillcourt.ui.base.BaseActivity;
import fiu.com.skillcourt.ui.game.creategame.CreateGameActivity;
import fiu.com.skillcourt.ui.game.startgame.StartGameActivity;
import fiu.com.skillcourt.ui.main.MainActivity;

/**
 * Created by LVaron on 10/13/17.
 */

public class GameOverActivity extends BaseActivity {

    private TextView mScore, mHits, mMode, mTime;
    private Button mPlayAgainButton, mNewGameButton, mHomeButton;
    private int testHit;
    private int testMiss;
    private String mGameMode;
    private long mGameTime;
    private int hit, miss, totalPoints;
    private CircleProgressView mCircleView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_over);

        Intent it = getIntent();
        mGameMode = it.getStringExtra("GAME_MODE");
        mGameTime = it.getLongExtra("GAME_TIME", 30);
        testHit = it.getIntExtra("HIT_COUNT", 0);
        testMiss = it.getIntExtra("MISS_COUNT", 0);
        totalPoints = it.getIntExtra("SCORE", 0);

        hit = (int) ((double) testHit / (testHit + testMiss) * 100);
        miss = (int) ((double) testMiss / (testHit + testMiss) * 100);

        Log.i("GameOverActivity", "Hit: " + hit + "%" + " Miss: " + miss + "%");

        mCircleView = findViewById(R.id.game_over_progress);
        mScore = findViewById(R.id.game_over_score);
        mHits = findViewById(R.id.game_over_hits);
        mMode = findViewById(R.id.game_over_mode);
        mTime = findViewById(R.id.game_over_time);
        mPlayAgainButton = findViewById(R.id.game_over_play_again_btn);
        mNewGameButton = findViewById(R.id.game_over_new_game_btn);
        mHomeButton = findViewById(R.id.game_over_home_btn);

        if(mGameMode == null){
            mGameMode = "Random";
        }
        mMode.setText(mGameMode);
        mMode.setAllCaps(true);
        mHits.setText("Hits: " + testHit + " / " + (testHit + testMiss));
        if(totalPoints < 0){
            totalPoints = 0;
        }
        mScore.setText("Score: " + totalPoints);
        setGameTimeText(mGameTime);
        if(hit == 0){
            setProgressBarColor(Color.parseColor("#B3B3B3"));
        }
        else if (hit > 0 && hit <= 20) {
           setProgressBarColor(Color.RED);
        }else if(hit > 20 && hit <= 40){
            setProgressBarColor(Color.parseColor("#f48342"));
        }else if(hit > 40 && hit <= 60){
            setProgressBarColor(Color.parseColor("#f4ce42"));
        }else if(hit > 60 && hit < 80){
            setProgressBarColor(getResources().getColor(R.color.colorPrimary));
        }
        mCircleView.setValueAnimated(hit);

        mPlayAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GameOverActivity.this, StartGameActivity.class);
                intent.putExtra("GAME_MODE", mGameMode);
                intent.putExtra("GAME_TIME", mGameTime);
                startActivity(intent);
                finish();
            }
        });

        mNewGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GameOverActivity.this, CreateGameActivity.class);
                startActivity(intent);
                finish();
            }
        });

        mHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GameOverActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void setProgressBarColor(int color){
        mCircleView.setTextColor(color);
        mCircleView.setBarColor(color);
        mCircleView.setUnitColor(color);
    }

    private void setGameTimeText(long seconds) {
        if (seconds >= 60) {
            int minute = (int) seconds / 60;
            seconds = seconds % 60;
            if (seconds < 10) {
                mTime.setText(minute + ":0" + seconds);
            } else {
                mTime.setText(minute + ":" + seconds);
            }
        } else {
            if (seconds < 10) {
                mTime.setText("00:0" + seconds);
            } else {
                mTime.setText("00:" + seconds);
            }
        }
    }

}
