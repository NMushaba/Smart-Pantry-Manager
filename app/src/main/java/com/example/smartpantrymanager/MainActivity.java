package com.example.smartpantrymanager;

import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

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
                        showEditIngredientForm(item);
                    }

                    @Override
                    public void onRemove(PantryItemAdapter.PantryItem item) {
                        removeIngredient(item);
                    }
                }
        );

        pantryListView.setAdapter(pantryAdapter);
        loadIngredientsFromDatabase();

        Button addIngredientButton = findViewById(R.id.addIngredientButton);
        addIngredientButton.setOnClickListener(view -> showAddIngredientForm());

        Button findRecipesButton = findViewById(R.id.findRecipesButton);
        findRecipesButton.setOnClickListener(view -> showRecipeSuggestions());

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(view -> showSettings());
    }

    private void showAddIngredientForm() {
        LinearLayout form = createIngredientForm();

        EditText nameInput = (EditText) form.getChildAt(0);
        EditText quantityInput = (EditText) form.getChildAt(1);
        EditText unitInput = (EditText) form.getChildAt(2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add ingredient")
                .setView(form)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String name = nameInput.getText().toString().trim();
                    String quantityText = quantityInput.getText().toString().trim();
                    String unit = unitInput.getText().toString().trim();

                    if (name.isEmpty() || quantityText.isEmpty() || unit.isEmpty()) {
                        showMessage("Please fill in all fields.");
                        return;
                    }

                    Double quantity = parsePositiveQuantity(quantityText);
                    if (quantity == null) {
                        showMessage("Enter a quantity greater than zero.");
                        return;
                    }

                    long itemId = databaseHelper.addPantryItem(name, quantity, unit);

                    if (itemId == -1) {
                        showMessage("The ingredient could not be saved.");
                        return;
                    }

                    pantryItems.add(new PantryItemAdapter.PantryItem(
                            itemId, name, quantity, unit
                    ));
                    pantryAdapter.notifyDataSetChanged();
                    updatePantryVisibility();
                    dialog.dismiss();
                })
        );

        dialog.show();
    }

    private void showEditIngredientForm(PantryItemAdapter.PantryItem item) {
        LinearLayout form = createIngredientForm();

        EditText nameInput = (EditText) form.getChildAt(0);
        EditText quantityInput = (EditText) form.getChildAt(1);
        EditText unitInput = (EditText) form.getChildAt(2);

        nameInput.setText(item.getName());
        quantityInput.setText(String.valueOf(item.getQuantity()));
        unitInput.setText(item.getUnit());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit ingredient")
                .setView(form)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String newName = nameInput.getText().toString().trim();
                    String quantityText = quantityInput.getText().toString().trim();
                    String newUnit = unitInput.getText().toString().trim();

                    if (newName.isEmpty() || quantityText.isEmpty() || newUnit.isEmpty()) {
                        showMessage("Please fill in all fields.");
                        return;
                    }

                    Double newQuantity = parsePositiveQuantity(quantityText);
                    if (newQuantity == null) {
                        showMessage("Enter a quantity greater than zero.");
                        return;
                    }

                    int updatedRows = databaseHelper.updatePantryItem(
                            item.getId(),
                            newName,
                            newQuantity,
                            newUnit
                    );

                    if (updatedRows > 0) {
                        for (int i = 0; i < pantryItems.size(); i++) {
                            if (pantryItems.get(i).getId() == item.getId()) {
                                pantryItems.set(
                                        i,
                                        new PantryItemAdapter.PantryItem(
                                                item.getId(),
                                                newName,
                                                newQuantity,
                                                newUnit
                                        )
                                );
                                break;
                            }
                        }

                        pantryAdapter.notifyDataSetChanged();
                        showMessage("Ingredient updated.");
                        dialog.dismiss();
                    } else {
                        showMessage("The ingredient could not be updated.");
                    }
                })
        );

        dialog.show();
    }

    private LinearLayout createIngredientForm() {
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

        return form;
    }

    private Double parsePositiveQuantity(String quantityText) {
        try {
            double quantity = Double.parseDouble(quantityText);
            if (quantity > 0) {
                return quantity;
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        return null;
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
        for (PantryItemAdapter.PantryItem item : pantryItems) {
            String name = item.getName().trim().toLowerCase(Locale.ROOT);

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
                .setMessage("Ingredients saved: " + pantryItems.size())
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
        pantryItems.clear();
        pantryAdapter.notifyDataSetChanged();
        updatePantryVisibility();
        showMessage("Pantry cleared.");
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}