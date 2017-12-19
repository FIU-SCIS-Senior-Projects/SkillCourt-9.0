package fiu.com.skillcourt.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import fiu.com.skillcourt.R;
import fiu.com.skillcourt.connection.ConnectionService;

/**
 * Created by Joosshhhh on 11/7/17.
 */

public class PadViewAdapter extends BaseAdapter {

    private final Context mContext;
    private ConnectionService mConnectionService;
    private String mCurrentColor = "white";

    public PadViewAdapter(Context context, ConnectionService connectionService) {
        mContext = context;
        mConnectionService = connectionService;
    }


    @Override
    public int getCount() {
        return mConnectionService.getPadsConnected();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            view = layoutInflater.inflate(R.layout.pad, null);
        }
        return view;
    }

    public String getCurrentColor() {
        return mCurrentColor;
    }

    public void changeColor(View view, String color) {
        TextView led1 = view.findViewById(R.id.led_1);
        TextView led2 = view.findViewById(R.id.led_2);
        TextView led3 = view.findViewById(R.id.led_3);
        TextView led4 = view.findViewById(R.id.led_4);
        TextView led5 = view.findViewById(R.id.led_5);

        switch (color) {
            case "white":
                mCurrentColor = "white";
                led1.setBackgroundColor(Color.WHITE);
                led2.setBackgroundColor(Color.WHITE);
                led3.setBackgroundColor(Color.WHITE);
                led4.setBackgroundColor(Color.WHITE);
                led5.setBackgroundColor(Color.WHITE);
                break;
            case "blue":
                mCurrentColor = "blue";
                led1.setBackgroundColor(Color.parseColor("#42bcf4"));
                led2.setBackgroundColor(Color.parseColor("#42bcf4"));
                led3.setBackgroundColor(Color.parseColor("#42bcf4"));
                led4.setBackgroundColor(Color.parseColor("#42bcf4"));
                led5.setBackgroundColor(Color.parseColor("#42bcf4"));
                break;
            case "green":
                mCurrentColor = "green";
                led1.setBackgroundColor(Color.GREEN);
                led2.setBackgroundColor(Color.GREEN);
                led3.setBackgroundColor(Color.GREEN);
                led4.setBackgroundColor(Color.GREEN);
                led5.setBackgroundColor(Color.GREEN);
                break;
            case "red":
                mCurrentColor = "red";
                led1.setBackgroundColor(Color.RED);
                led2.setBackgroundColor(Color.RED);
                led3.setBackgroundColor(Color.RED);
                led4.setBackgroundColor(Color.RED);
                led5.setBackgroundColor(Color.RED);
                break;
        }
    }
}
