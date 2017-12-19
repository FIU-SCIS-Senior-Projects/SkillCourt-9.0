package fiu.com.skillcourt.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.adapters.GamesPlayedRecyclerViewAdapter;
import fiu.com.skillcourt.entities.Game;
import fiu.com.skillcourt.ui.base.BaseFragment;
import fiu.com.skillcourt.ui.game.GameActivty;
import fiu.com.skillcourt.ui.game.creategame.CreateGameActivity;

public class MainDashboardFragment extends BaseFragment {

    private static final String TAG = MainDashboardFragment.class.getName();
    private MainActivity mainActivity;
    private FloatingActionButton mPlayButton;

    LineChart mChart;
    List<Entry> entryGamesAccuracy;
    LineDataSet gamesAccuracy;
    LineData gamesAccuracyLineData;

    //Firebase database reference
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mUserDatabaseReference;
    DatabaseReference mRoleRef;

    //Objects for graph
    Date oneWeekAgo;
    List<Game> gamesThisWeek;

    //Games for list of games played
    RecyclerView rvHistory;
    GamesPlayedRecyclerViewAdapter gamesPlayedRecyclerViewAdapter;
    List<Game> historyGames;

    public static MainDashboardFragment newInstance() {
        return new MainDashboardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_dashboard, container, false);
        mainActivity = (MainActivity) getActivity();
        mPlayButton = view.findViewById(R.id.playButton);

        // Play button will go to a StartGame Activity
        // where you configure your settings for the current game
        // like Game Mode, Time Limit, and Time Delay

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainActivity.mBounded) {
                    if (mainActivity.mConnectionService.getPadsConnected() > 0) {
                        //ask if these are the pads they want
                        //if yes then go to start game activity
                        //else just dismiss
                        Intent intent = new Intent(mainActivity, CreateGameActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(mainActivity, "You must be connected to at least 1 pad to play", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

        return view;
    }

    private void setupHistoryView() {
        historyGames = new ArrayList<>();
        gamesPlayedRecyclerViewAdapter = new GamesPlayedRecyclerViewAdapter(historyGames);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setSmoothScrollbarEnabled(true);
        rvHistory.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvHistory.getContext(),
                layoutManager.getOrientation());
        rvHistory.addItemDecoration(dividerItemDecoration);
        rvHistory.setAdapter(gamesPlayedRecyclerViewAdapter);
    }

    private void setupChart() {
        entryGamesAccuracy = new ArrayList<>();
        mChart.getDescription().setEnabled(false);
        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.getAxisLeft().setEnabled(true);
        mChart.getAxisLeft().setSpaceTop(40);
        mChart.getAxisLeft().setSpaceBottom(40);
        mChart.getAxisRight().setEnabled(false);
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setDrawAxisLine(true);
        yAxis.setGranularity(1f);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 7);
        oneWeekAgo = cal.getTime();

        gamesThisWeek = new ArrayList<>();

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserDatabaseReference = mFirebaseDatabase.getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        mRoleRef = mUserDatabaseReference.child("role");

    }

    private void getGamesLastWeek() {
        DatabaseReference mGameDatabaseReference = mUserDatabaseReference
                .child("games");
        Query query = mGameDatabaseReference
                .orderByChild("date")
                .startAt(oneWeekAgo.getTime())
                .endAt(new Date().getTime());

        ChildEventListener mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Game game = dataSnapshot.getValue(Game.class);
                gamesThisWeek.add(game);
                updateChart();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        query.addChildEventListener(mChildEventListener);
    }

    private void updateChart() {
        int index = entryGamesAccuracy.size();
        entryGamesAccuracy.add(new Entry(index, gamesThisWeek.get(index).getScore()));
        if (gamesAccuracyLineData == null) {
            gamesAccuracy = new LineDataSet(entryGamesAccuracy, "Accuracy");
            gamesAccuracy.disableDashedLine();
            gamesAccuracy.setCircleColor(getContext().getResources().getColor(R.color.colorAccent));
            gamesAccuracy.setColor(getContext().getResources().getColor(R.color.colorAccent));
            gamesAccuracyLineData = new LineData(gamesAccuracy);
            mChart.setData(gamesAccuracyLineData);
        }
        gamesAccuracyLineData.notifyDataChanged();
        mChart.invalidate();
        mChart.fitScreen();
    }

    public void getHistoryGames() {
        DatabaseReference mGameDatabaseReference = mUserDatabaseReference
                .child("games");
        Query query = mGameDatabaseReference
                .orderByChild("date");

        ChildEventListener mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Game game = dataSnapshot.getValue(Game.class);
                historyGames.add(game);
                updateHistoryGames();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        query.addChildEventListener(mChildEventListener);
    }

    private void updateHistoryGames() {
        gamesPlayedRecyclerViewAdapter.notifyDataSetChanged();
    }

    public void showCoachOptions(final Menu menu) {

        mRoleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userRole = dataSnapshot.getValue(String.class);
                /*
                if (userRole.equals("coach")) {
                    menu.findItem(R.id.addCoaching).setVisible(false);
                } else {
                    menu.findItem(R.id.addCoaching).setVisible(true);
                }
                */
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
