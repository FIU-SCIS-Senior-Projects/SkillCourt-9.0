package fiu.com.skillcourt.ui.game.creategame;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.shawnlin.numberpicker.NumberPicker;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.ui.base.BaseActivity;
import fiu.com.skillcourt.ui.game.startgame.StartGameActivity;
import fiu.com.skillcourt.ui.main.MainActivity;

public class CreateGameActivity extends BaseActivity {
    private static final String TAG = CreateGameActivity.class.getSimpleName();
    private Button mPlayGame, mCancel;
    private NumberPicker mMinutePicker;
    private NumberPicker mSecondPicker;
    private String mGameMode = "Random";

    private int mGameMinutes = 0;
    private int mGameSeconds = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);
        mMinutePicker = findViewById(R.id.minute_picker);
        mSecondPicker = findViewById(R.id.second_picker);
        mPlayGame = findViewById(R.id.playGameBtn);
        mCancel = findViewById(R.id.cancelBtn);

        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(59);
        mSecondPicker.setMinValue(0);
        mSecondPicker.setMaxValue(59);

        mMinutePicker.setWrapSelectorWheel(true);
        mSecondPicker.setWrapSelectorWheel(true);

        mMinutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                mGameMinutes = newVal;
            }
        });

        mSecondPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                mGameSeconds = newVal;
            }
        });

        mPlayGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Minutes: " + mGameMinutes);
                Log.i(TAG, "Seconds: " + mGameSeconds);

                if (mGameMinutes == 0 && mGameSeconds == 0) {
                    Toast.makeText(CreateGameActivity.this, "Must select an appropriate amount of time for playing", Toast.LENGTH_SHORT).show();
                } else {
                    long gameTimeFinal = (60 * mGameMinutes) + mGameSeconds;
                    Log.i(TAG, "Game time in seconds: " + gameTimeFinal);
                    Intent intent = new Intent(CreateGameActivity.this, StartGameActivity.class);
                    intent.putExtra("GAME_MODE", mGameMode);
                    intent.putExtra("GAME_TIME", gameTimeFinal);
                    startActivity(intent);
                }
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CreateGameActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });
    }



}
