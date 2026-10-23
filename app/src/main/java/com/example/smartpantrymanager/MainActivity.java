package com.example.smartpantrymanager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "smart_pantry";
    private static final String INGREDIENTS_KEY = "ingredients";

    private LinearLayout pantryIngredientsContainer;
    private ScrollView pantryListScroll;
    private TextView emptyPantryMessage;
    private final ArrayList<String> ingredients = new ArrayList<>();

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

        pantryIngredientsContainer = findViewById(R.id.pantryIngredientsContainer);
        pantryListScroll = findViewById(R.id.pantryListScroll);
        emptyPantryMessage = findViewById(R.id.emptyPantryMessage);

        loadIngredients();

        Button addIngredientButton = findViewById(R.id.addIngredientButton);
        addIngredientButton.setOnClickListener(view -> showAddIngredientForm());
    }

    private void showAddIngredientForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Ingredient name");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        form.addView(nameInput);

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Quantity");
        quantityInput.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        form.addView(quantityInput);

        EditText unitInput = new EditText(this);
        unitInput.setHint("Unit, for example pieces or cups");
        unitInput.setInputType(InputType.TYPE_CLASS_TEXT);
        form.addView(unitInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add ingredient")
                .setView(form)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", (dialogInterface, which) ->
                        dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String name = nameInput.getText().toString().trim();
                    String quantity = quantityInput.getText().toString().trim();
                    String unit = unitInput.getText().toString().trim();

                    if (name.isEmpty() || quantity.isEmpty() || unit.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Please fill in all fields.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String ingredient = name + " — " + quantity + " " + unit;
                    ingredients.add(ingredient);
                    addIngredientToScreen(ingredient);
                    saveIngredients();
                    dialog.dismiss();
                })
        );

        dialog.show();
    }

    private void addIngredientToScreen(String ingredient) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        row.setPadding(padding, padding, padding, padding);

        TextView ingredientText = new TextView(this);
        ingredientText.setText(ingredient);
        ingredientText.setTextSize(16);
        ingredientText.setTextColor(0xFF29252F);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        row.addView(ingredientText, textParams);

        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        row.addView(removeButton);

        removeButton.setOnClickListener(view -> {
            ingredients.remove(ingredient);
            pantryIngredientsContainer.removeView(row);
            saveIngredients();

            if (ingredients.isEmpty()) {
                emptyPantryMessage.setVisibility(View.VISIBLE);
                pantryListScroll.setVisibility(View.GONE);
            }
        });

        pantryIngredientsContainer.addView(row);
        emptyPantryMessage.setVisibility(View.GONE);
        pantryListScroll.setVisibility(View.VISIBLE);
    }

    private void saveIngredients() {
        JSONArray jsonArray = new JSONArray();

        for (String ingredient : ingredients) {
            jsonArray.put(ingredient);
        }

        SharedPreferences preferences =
                getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        preferences.edit()
                .putString(INGREDIENTS_KEY, jsonArray.toString())
                .apply();
    }

    private void loadIngredients() {
        SharedPreferences preferences =
                getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String savedIngredients = preferences.getString(INGREDIENTS_KEY, "[]");

        try {
            JSONArray jsonArray = new JSONArray(savedIngredients);

            for (int i = 0; i < jsonArray.length(); i++) {
                String ingredient = jsonArray.getString(i);
                ingredients.add(ingredient);
                addIngredientToScreen(ingredient);
            }
        } catch (JSONException exception) {
            ingredients.clear();
            pantryIngredientsContainer.removeAllViews();
            emptyPantryMessage.setVisibility(View.VISIBLE);
            pantryListScroll.setVisibility(View.GONE);
        }
    }
}