package fiu.com.skillcourt.ui.game.gameOver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.ui.game.GameActivty;
import fiu.com.skillcourt.ui.main.MainActivity;


/**
 * Created by Joosshhhh on 11/13/17.
 */

public class GameOverFragment extends Fragment {

    private TextView mHit, mMiss;
    private Button mHomeButton, mPlayAgainButton, mNewGameButton;
    private int testHit;
    private int testMiss;
    private ProgressBar mProgress;
    private String Home = "Home";
    private String mPlayAgain = "Play Again";
    private int hit;
    private int miss;

    private GameActivty gameActivty;

    public static GameOverFragment newInstance() {
        return new GameOverFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gameover, container, false);
        gameActivty = (GameActivty) getActivity();

        hit = (int) ((double) testHit / (testHit + testMiss) * 100);
        miss = (int) ((double) testMiss / (testHit + testMiss) * 100);
        Log.i("GameOverActivity", "Hit: " + hit + "%" + " Miss: " + miss + "%");

        mHit = view.findViewById(R.id.Hit);
        mMiss = view.findViewById(R.id.Miss);
        mHomeButton = view.findViewById(R.id.Home);
        mPlayAgainButton = view.findViewById(R.id.playAgain);
        // mProgress = findViewById(R.id.progressBarHitMiss);

        mHit.setText("Hit Rate: " + hit);
        mMiss.setText("Miss Rate: " + miss);
        mHomeButton.setText(Home);
        mPlayAgainButton.setText(mPlayAgain);
        //mProgress.setProgress(10);

        mHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), MainActivity.class);
                startActivity(intent);

            }
        });

        mPlayAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.gaming_frame_layout, StartGameFragment, "START_GAME");
                transaction.commit();
                */

            }
        });
        return view;
    }

}
