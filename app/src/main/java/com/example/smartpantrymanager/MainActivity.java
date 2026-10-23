package com.example.smartpantrymanager;

import android.database.Cursor;
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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private PantryDatabaseHelper databaseHelper;
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

        databaseHelper = new PantryDatabaseHelper(this);
        loadIngredientsFromDatabase();

        Button addIngredientButton = findViewById(R.id.addIngredientButton);
        addIngredientButton.setOnClickListener(view -> showAddIngredientForm());

        Button findRecipesButton = findViewById(R.id.findRecipesButton);
        findRecipesButton.setOnClickListener(view -> showRecipeSuggestions());

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> showSettings());
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
                    String quantityText = quantityInput.getText().toString().trim();
                    String unit = unitInput.getText().toString().trim();

                    if (name.isEmpty() || quantityText.isEmpty() || unit.isEmpty()) {
                        Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double quantity;
                    try {
                        quantity = Double.parseDouble(quantityText);
                    } catch (NumberFormatException exception) {
                        Toast.makeText(this, "Enter a valid quantity.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (quantity <= 0) {
                        Toast.makeText(
                                this,
                                "Quantity must be greater than zero.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    long itemId = databaseHelper.addPantryItem(name, quantity, unit);

                    if (itemId == -1) {
                        Toast.makeText(
                                this,
                                "The ingredient could not be saved.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    ingredients.add(name);
                    addIngredientToScreen(itemId, name, quantity, unit);
                    dialog.dismiss();
                })
        );

        dialog.show();
    }

    private void showEditIngredientForm(
            long itemId,
            String oldName,
            double oldQuantity,
            String oldUnit,
            TextView ingredientText
    ) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Ingredient name");
        nameInput.setText(oldName);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        form.addView(nameInput);

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Quantity");
        quantityInput.setText(String.valueOf(oldQuantity));
        quantityInput.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        form.addView(quantityInput);

        EditText unitInput = new EditText(this);
        unitInput.setHint("Unit, for example pieces or cups");
        unitInput.setText(oldUnit);
        unitInput.setInputType(InputType.TYPE_CLASS_TEXT);
        form.addView(unitInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit ingredient")
                .setView(form)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialogInterface, which) ->
                        dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String newName = nameInput.getText().toString().trim();
                    String quantityText = quantityInput.getText().toString().trim();
                    String newUnit = unitInput.getText().toString().trim();

                    if (newName.isEmpty() || quantityText.isEmpty() || newUnit.isEmpty()) {
                        Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newQuantity;
                    try {
                        newQuantity = Double.parseDouble(quantityText);
                    } catch (NumberFormatException exception) {
                        Toast.makeText(this, "Enter a valid quantity.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newQuantity <= 0) {
                        Toast.makeText(
                                this,
                                "Quantity must be greater than zero.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    int updatedRows = databaseHelper.updatePantryItem(
                            itemId,
                            newName,
                            newQuantity,
                            newUnit
                    );

                    if (updatedRows > 0) {
                        ingredients.remove(oldName);
                        ingredients.add(newName);
                        ingredientText.setText(
                                newName + " — " + newQuantity + " " + newUnit
                        );
                        Toast.makeText(this, "Ingredient updated.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(
                                this,
                                "The ingredient could not be updated.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
        );

        dialog.show();
    }

    private void loadIngredientsFromDatabase() {
        ingredients.clear();
        pantryIngredientsContainer.removeAllViews();

        Cursor cursor = databaseHelper.getPantryItems();

        try {
            int idColumn = cursor.getColumnIndexOrThrow("_id");
            int nameColumn = cursor.getColumnIndexOrThrow("name");
            int quantityColumn = cursor.getColumnIndexOrThrow("quantity");
            int unitColumn = cursor.getColumnIndexOrThrow("unit");

            while (cursor.moveToNext()) {
                long itemId = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                double quantity = cursor.getDouble(quantityColumn);
                String unit = cursor.getString(unitColumn);

                ingredients.add(name);
                addIngredientToScreen(itemId, name, quantity, unit);
            }
        } finally {
            cursor.close();
        }

        updatePantryVisibility();
    }

    private void addIngredientToScreen(
            long itemId,
            String name,
            double quantity,
            String unit
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        row.setPadding(padding, padding, padding, padding);

        TextView ingredientText = new TextView(this);
        ingredientText.setText(name + " — " + quantity + " " + unit);
        ingredientText.setTextSize(16);
        ingredientText.setTextColor(0xFF29252F);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        row.addView(ingredientText, textParams);

        Button editButton = new Button(this);
        editButton.setText("Edit");
        row.addView(editButton);

        editButton.setOnClickListener(view ->
                showEditIngredientForm(itemId, name, quantity, unit, ingredientText));

        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        row.addView(removeButton);

        removeButton.setOnClickListener(view -> {
            int deletedRows = databaseHelper.deletePantryItem(itemId);

            if (deletedRows > 0) {
                ingredients.remove(name);
                pantryIngredientsContainer.removeView(row);
                updatePantryVisibility();
            }
        });

        pantryIngredientsContainer.addView(row);
        updatePantryVisibility();
    }

    private void updatePantryVisibility() {
        boolean pantryIsEmpty = ingredients.isEmpty();

        emptyPantryMessage.setVisibility(pantryIsEmpty ? View.VISIBLE : View.GONE);
        pantryListScroll.setVisibility(pantryIsEmpty ? View.GONE : View.VISIBLE);
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
            String name = savedIngredient.trim().toLowerCase();

            if (name.equals(requiredIngredient)
                    || name.equals(requiredIngredient + "s")
                    || (requiredIngredient.equals("tomato") && name.equals("tomatoes"))) {
                return true;
            }
        }
        return false;
    }

    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setMessage("Ingredients saved: " + ingredients.size())
                .setPositiveButton("Clear pantry", (dialog, which) ->
                        confirmClearPantry())
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmClearPantry() {
        new AlertDialog.Builder(this)
                .setTitle("Clear pantry?")
                .setMessage("This will remove all saved ingredients.")
                .setPositiveButton("Clear", (dialog, which) -> clearPantry())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearPantry() {
        databaseHelper.clearPantry();
        ingredients.clear();
        pantryIngredientsContainer.removeAllViews();
        updatePantryVisibility();

        Toast.makeText(this, "Pantry cleared.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }
}