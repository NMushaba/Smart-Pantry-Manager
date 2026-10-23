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
        ArrayList<Recipe> matchingRecipes = new ArrayList<>();

        Cursor recipesCursor = databaseHelper.getRecipes();

        try {
            int idColumn = recipesCursor.getColumnIndexOrThrow("_id");
            int nameColumn = recipesCursor.getColumnIndexOrThrow("name");
            int instructionsColumn =
                    recipesCursor.getColumnIndexOrThrow("instructions");

            while (recipesCursor.moveToNext()) {
                long recipeId = recipesCursor.getLong(idColumn);
                String recipeName = recipesCursor.getString(nameColumn);
                String instructions = recipesCursor.getString(instructionsColumn);

                if (canMakeRecipe(recipeId)) {
                    matchingRecipes.add(
                            new Recipe(recipeId, recipeName, instructions)
                    );
                }
            }
        } finally {
            recipesCursor.close();
        }

        if (matchingRecipes.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No recipes available yet")
                    .setMessage("Add enough ingredients to your pantry and check again.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] recipeNames = new String[matchingRecipes.size()];

        for (int i = 0; i < matchingRecipes.size(); i++) {
            recipeNames[i] = matchingRecipes.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Recipes you can make")
                .setItems(recipeNames, (dialog, which) ->
                        showRecipeInstructions(matchingRecipes.get(which)))
                .setNegativeButton("Close", null)
                .show();
    }

    private boolean canMakeRecipe(long recipeId) {
        Cursor ingredientsCursor =
                databaseHelper.getRecipeIngredients(recipeId);

        try {
            int nameColumn =
                    ingredientsCursor.getColumnIndexOrThrow("ingredient_name");
            int quantityColumn =
                    ingredientsCursor.getColumnIndexOrThrow("required_quantity");
            int unitColumn =
                    ingredientsCursor.getColumnIndexOrThrow("unit");

            if (!ingredientsCursor.moveToFirst()) {
                return false;
            }

            do {
                String ingredientName = ingredientsCursor.getString(nameColumn);
                double requiredQuantity =
                        ingredientsCursor.getDouble(quantityColumn);
                String requiredUnit = ingredientsCursor.getString(unitColumn);

                if (!hasEnoughIngredient(
                        ingredientName,
                        requiredQuantity,
                        requiredUnit
                )) {
                    return false;
                }
            } while (ingredientsCursor.moveToNext());

            return true;
        } finally {
            ingredientsCursor.close();
        }
    }

    private boolean hasEnoughIngredient(
            String requiredName,
            double requiredQuantity,
            String requiredUnit
    ) {
        String wantedName = normalizeIngredientName(requiredName);
        String wantedUnit = normalizeUnit(requiredUnit);

        double requiredBaseQuantity =
                toBaseQuantity(requiredQuantity, requiredUnit);

        if (requiredBaseQuantity < 0) {
            return false;
        }

        double availableBaseQuantity = 0;

        for (PantryItemAdapter.PantryItem item : pantryItems) {
            String pantryName = normalizeIngredientName(item.getName());
            String pantryUnit = normalizeUnit(item.getUnit());

            if (pantryName.equals(wantedName)
                    && pantryUnit.equals(wantedUnit)) {
                double pantryBaseQuantity =
                        toBaseQuantity(item.getQuantity(), item.getUnit());

                if (pantryBaseQuantity >= 0) {
                    availableBaseQuantity += pantryBaseQuantity;
                }
            }
        }

        return availableBaseQuantity >= requiredBaseQuantity;
    }

    private String normalizeIngredientName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);

        if (normalized.equals("tomatoes")) {
            return "tomato";
        }
        if (normalized.equals("potatoes")) {
            return "potato";
        }
        if (normalized.equals("eggs")) {
            return "egg";
        }
        if (normalized.equals("peas")) {
            return "pea";
        }
        if (normalized.endsWith("ies") && normalized.length() > 3) {
            return normalized.substring(0, normalized.length() - 3) + "y";
        }
        if (normalized.endsWith("s") && normalized.length() > 1) {
            return normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String normalizeUnit(String unit) {
        String normalized = unit.trim().toLowerCase(Locale.ROOT);

        if (normalized.equals("piece") || normalized.equals("pieces")
                || normalized.equals("pc") || normalized.equals("pcs")) {
            return "piece";
        }
        if (normalized.equals("slice") || normalized.equals("slices")) {
            return "slice";
        }
        if (normalized.equals("g") || normalized.equals("gram")
                || normalized.equals("grams") || normalized.equals("kg")
                || normalized.equals("kilogram")
                || normalized.equals("kilograms")) {
            return "gram";
        }
        if (normalized.equals("ml") || normalized.equals("milliliter")
                || normalized.equals("milliliters")
                || normalized.equals("millilitre")
                || normalized.equals("millilitres")
                || normalized.equals("l") || normalized.equals("liter")
                || normalized.equals("liters") || normalized.equals("litre")
                || normalized.equals("litres")) {
            return "ml";
        }

        return normalized;
    }

    private double toBaseQuantity(double quantity, String unit) {
        String normalized = unit.trim().toLowerCase(Locale.ROOT);

        if (normalized.equals("kg")
                || normalized.equals("kilogram")
                || normalized.equals("kilograms")) {
            return quantity * 1000;
        }

        if (normalized.equals("l") || normalized.equals("liter")
                || normalized.equals("liters") || normalized.equals("litre")
                || normalized.equals("litres")) {
            return quantity * 1000;
        }

        if (normalizeUnit(unit).equals("piece")
                || normalizeUnit(unit).equals("slice")
                || normalizeUnit(unit).equals("gram")
                || normalizeUnit(unit).equals("ml")) {
            return quantity;
        }

        return -1;
    }

    private void showRecipeInstructions(Recipe recipe) {
        new AlertDialog.Builder(this)
                .setTitle(recipe.name)
                .setMessage(recipe.instructions)
                .setPositiveButton("Close", null)
                .show();
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

    private static class Recipe {
        final long id;
        final String name;
        final String instructions;

        Recipe(long id, String name, String instructions) {
            this.id = id;
            this.name = name;
            this.instructions = instructions;
        }
    }
}