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

        Button findRecipesButton = findViewById(R.id.findRecipesButton);
        findRecipesButton.setOnClickListener(view -> showRecipeSuggestions());
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

    private void showRecipeSuggestions() {
        ArrayList<String> matchingRecipes = new ArrayList<>();

        if (hasIngredients("tomato", "egg")) {
            matchingRecipes.add("Tomato and egg scramble");
        }

        if (hasIngredients("egg", "bread")) {
            matchingRecipes.add("Egg toast");
        }

        if (hasIngredients("tomato", "cucumber")) {
            matchingRecipes.add("Tomato and cucumber salad");
        }

        if (matchingRecipes.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No recipes available yet")
                    .setMessage("Add more ingredients to your pantry and check again.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] recipeNames = matchingRecipes.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Recipes you can make")
                .setItems(recipeNames, (dialog, which) ->
                        showRecipeInstructions(recipeNames[which]))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRecipeInstructions(String recipeName) {
        String instructions;

        switch (recipeName) {
            case "Tomato and egg scramble":
                instructions = "1. Wash and chop the tomato.\n"
                        + "2. Beat the eggs in a bowl.\n"
                        + "3. Cook the tomato in a pan for a few minutes.\n"
                        + "4. Add the eggs and stir until fully cooked.";
                break;

            case "Egg toast":
                instructions = "1. Toast the bread.\n"
                        + "2. Cook the egg in a pan until done.\n"
                        + "3. Place the cooked egg on the toast and serve.";
                break;

            case "Tomato and cucumber salad":
                instructions = "1. Wash the tomato and cucumber.\n"
                        + "2. Chop them into small pieces.\n"
                        + "3. Mix together in a bowl and serve.";
                break;

            default:
                instructions = "Instructions are not available for this recipe.";
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle(recipeName)
                .setMessage(instructions)
                .setPositiveButton("Close", null)
                .show();
    }

    private boolean hasIngredients(String... requiredIngredients) {
        for (String required : requiredIngredients) {
            if (!hasIngredient(required)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasIngredient(String requiredIngredient) {
        for (String savedIngredient : ingredients) {
            String[] parts = savedIngredient.split(" — ", 2);
            String name = parts[0].trim().toLowerCase();

            if (name.equals(requiredIngredient)
                    || name.equals(requiredIngredient + "s")
                    || (requiredIngredient.equals("tomato") && name.equals("tomatoes"))) {
                return true;
            }
        }
        return false;
    }
}