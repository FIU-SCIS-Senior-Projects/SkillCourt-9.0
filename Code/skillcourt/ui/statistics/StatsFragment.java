package fiu.com.skillcourt.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.ui.base.BaseFragment;

/**
 * Created by Joosshhhh on 11/10/17.
 */

public class StatsFragment extends BaseFragment {

    public static StatsFragment newInstance(){
        return new StatsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        return view;
    }
}
