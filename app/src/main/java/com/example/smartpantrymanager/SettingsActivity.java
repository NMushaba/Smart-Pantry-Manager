package com.example.smartpantrymanager;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private PantryDatabaseHelper databaseHelper;
    private TextView ingredientCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        databaseHelper = new PantryDatabaseHelper(this);
        ingredientCountText = findViewById(R.id.settingsIngredientCount);

        updateIngredientCount();

        Button clearPantryButton = findViewById(R.id.clearPantryButton);
        clearPantryButton.setOnClickListener(view -> confirmClearPantry());

        Button backButton = findViewById(R.id.backToPantryFromSettingsButton);
        backButton.setOnClickListener(view -> finish());
    }

    private void updateIngredientCount() {
        int count = 0;
        Cursor cursor = databaseHelper.getPantryItems();

        try {
            count = cursor.getCount();
        } finally {
            cursor.close();
        }

        ingredientCountText.setText(
                getString(R.string.settings_ingredient_count, count)
        );
    }

    private void confirmClearPantry() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_pantry_confirmation_title)
                .setMessage(R.string.clear_pantry_confirmation_message)
                .setPositiveButton(R.string.clear_pantry, (dialog, which) -> {
                    databaseHelper.clearPantry();
                    updateIngredientCount();
                    Toast.makeText(
                            this,
                            "Pantry cleared.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}