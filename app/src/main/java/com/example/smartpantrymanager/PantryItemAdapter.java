package com.example.smartpantrymanager;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class PantryItemAdapter extends BaseAdapter {

    public static class PantryItem {
        private final long id;
        private final String name;
        private final double quantity;
        private final String unit;

        public PantryItem(long id, String name, double quantity, String unit) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }
    }

    public interface ItemActions {
        void onEdit(PantryItem item);
        void onRemove(PantryItem item);
    }

    private final Context context;
    private final List<PantryItem> items;
    private final ItemActions actions;

    public PantryItemAdapter(
            Context context,
            List<PantryItem> items,
            ItemActions actions
    ) {
        this.context = context;
        this.items = items;
        this.actions = actions;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public PantryItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowHolder holder;

        if (convertView == null) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            int padding = toDp(6);
            row.setPadding(padding, padding, padding, padding);

            TextView itemText = new TextView(context);
            itemText.setTextSize(15);
            itemText.setTextColor(0xFF29252F);

            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            row.addView(itemText, textParams);

            Button editButton = new Button(context);
            editButton.setText("Edit");
            row.addView(editButton);

            Button removeButton = new Button(context);
            removeButton.setText("Remove");
            row.addView(removeButton);

            holder = new RowHolder(itemText, editButton, removeButton);
            row.setTag(holder);
            convertView = row;
        } else {
            holder = (RowHolder) convertView.getTag();
        }

        PantryItem item = getItem(position);
        holder.itemText.setText(
                item.getName() + " — " + formatQuantity(item.getQuantity())
                        + " " + item.getUnit()
        );

        holder.editButton.setOnClickListener(view -> actions.onEdit(item));
        holder.removeButton.setOnClickListener(view -> actions.onRemove(item));

        return convertView;
    }

    private int toDp(int value) {
        return (int) (value * context.getResources()
                .getDisplayMetrics().density);
    }

    private String formatQuantity(double quantity) {
        if (quantity == Math.floor(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }

    private static class RowHolder {
        final TextView itemText;
        final Button editButton;
        final Button removeButton;

        RowHolder(TextView itemText, Button editButton, Button removeButton) {
            this.itemText = itemText;
            this.editButton = editButton;
            this.removeButton = removeButton;
        }
    }
}