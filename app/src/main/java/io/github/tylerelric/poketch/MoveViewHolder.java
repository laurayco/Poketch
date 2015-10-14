package io.github.tylerelric.poketch;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.Map;

import io.github.tylerelric.poketch.data.QueryViewHolder;

public class MoveViewHolder
extends QueryViewHolder {

    TextView name, description, accuracy, power, pp;

    public MoveViewHolder(View itemView) {
        super(itemView);
        name = (TextView)itemView.findViewById(R.id.move_name);
        description = (TextView)itemView.findViewById(R.id.move_description);
        accuracy = (TextView)itemView.findViewById(R.id.move_accuracy);
        power = (TextView) itemView.findViewById(R.id.move_power);
        pp = (TextView) itemView.findViewById(R.id.move_pp);
    }

    @Override
    public void update_data(Map<String,Object> data) {
        super.update_data(data);
        name.setText(data.get("name").toString());
        description.setText(data.get("description").toString());
        accuracy.setText(data.get("accuracy").toString());
        power.setText(data.get("power").toString());
        pp.setText(data.get("pp").toString());
    }
}
