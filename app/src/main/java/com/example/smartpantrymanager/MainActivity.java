package com.example.smartpantrymanager;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private PantryDatabaseHelper databaseHelper;
    private ListView pantryListView;
    private TextView emptyPantryMessage;

    private final ArrayList<PantryItemAdapter.PantryItem> pantryItems =
            new ArrayList<>();

    private PantryItemAdapter pantryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        pantryListView = findViewById(R.id.pantryListView);
        emptyPantryMessage = findViewById(R.id.emptyPantryMessage);

        databaseHelper = new PantryDatabaseHelper(this);

        pantryAdapter = new PantryItemAdapter(
                this,
                pantryItems,
                new PantryItemAdapter.ItemActions() {
                    @Override
                    public void onEdit(PantryItemAdapter.PantryItem item) {
                        openEditIngredientForm(item);
                    }

                    @Override
                    public void onRemove(PantryItemAdapter.PantryItem item) {
                        removeIngredient(item);
                    }
                }
        );

        pantryListView.setAdapter(pantryAdapter);

        Button addIngredientButton = findViewById(R.id.addIngredientButton);
        addIngredientButton.setOnClickListener(view -> openAddIngredientForm());

        Button findRecipesButton = findViewById(R.id.findRecipesButton);
        findRecipesButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, RecipeListActivity.class))
        );

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class))
        );
    }

    private void openAddIngredientForm() {
        Intent intent = new Intent(this, IngredientFormActivity.class);
        startActivity(intent);
    }

    private void openEditIngredientForm(PantryItemAdapter.PantryItem item) {
        Intent intent = new Intent(this, IngredientFormActivity.class);
        intent.putExtra(IngredientFormActivity.EXTRA_ID, item.getId());
        intent.putExtra(IngredientFormActivity.EXTRA_NAME, item.getName());
        intent.putExtra(IngredientFormActivity.EXTRA_QUANTITY, item.getQuantity());
        intent.putExtra(IngredientFormActivity.EXTRA_UNIT, item.getUnit());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && pantryAdapter != null) {
            loadIngredientsFromDatabase();
        }
    }

    private void loadIngredientsFromDatabase() {
        pantryItems.clear();

        Cursor cursor = databaseHelper.getPantryItems();

        try {
            int idColumn = cursor.getColumnIndexOrThrow("_id");
            int nameColumn = cursor.getColumnIndexOrThrow("name");
            int quantityColumn = cursor.getColumnIndexOrThrow("quantity");
            int unitColumn = cursor.getColumnIndexOrThrow("unit");

            while (cursor.moveToNext()) {
                pantryItems.add(new PantryItemAdapter.PantryItem(
                        cursor.getLong(idColumn),
                        cursor.getString(nameColumn),
                        cursor.getDouble(quantityColumn),
                        cursor.getString(unitColumn)
                ));
            }
        } finally {
            cursor.close();
        }

        pantryAdapter.notifyDataSetChanged();
        updatePantryVisibility();
    }

    private void removeIngredient(PantryItemAdapter.PantryItem item) {
        int deletedRows = databaseHelper.deletePantryItem(item.getId());

        if (deletedRows > 0) {
            for (int i = 0; i < pantryItems.size(); i++) {
                if (pantryItems.get(i).getId() == item.getId()) {
                    pantryItems.remove(i);
                    break;
                }
            }

            pantryAdapter.notifyDataSetChanged();
            updatePantryVisibility();
        }
    }

    private void updatePantryVisibility() {
        boolean pantryIsEmpty = pantryItems.isEmpty();

        emptyPantryMessage.setVisibility(
                pantryIsEmpty ? View.VISIBLE : View.GONE
        );
        pantryListView.setVisibility(
                pantryIsEmpty ? View.GONE : View.VISIBLE
        );
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}