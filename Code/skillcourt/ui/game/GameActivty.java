package fiu.com.skillcourt.ui.game;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.ui.base.BaseActivity;
import fiu.com.skillcourt.ui.game.gameOver.GameOverFragment;

/**
 * Created by Joosshhhh on 11/13/17.
 */

public class GameActivty extends BaseActivity {

    public String mGameMode = "Random";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaming);
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.gaming_frame_layout, GameOverFragment.newInstance(), "GAME_SETTINGS");
            transaction.commit();
        }

    }
}
