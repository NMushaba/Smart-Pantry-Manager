package com.example.smartpantrymanager;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class RecipeListActivity extends AppCompatActivity {

    private PantryDatabaseHelper databaseHelper;
    private ListView recipeListView;
    private TextView emptyRecipesMessage;

    private final ArrayList<Recipe> matchingRecipes = new ArrayList<>();
    private final ArrayList<String> recipeNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);

        recipeListView = findViewById(R.id.recipeListView);
        emptyRecipesMessage = findViewById(R.id.emptyRecipesMessage);

        databaseHelper = new PantryDatabaseHelper(this);

        Button backButton = findViewById(R.id.backToPantryButton);
        backButton.setOnClickListener(view -> finish());

        loadMatchingRecipes();
    }

    private void loadMatchingRecipes() {
        matchingRecipes.clear();
        recipeNames.clear();

        Cursor recipesCursor = databaseHelper.getRecipes();

        try {
            int idColumn = recipesCursor.getColumnIndexOrThrow("_id");
            int nameColumn = recipesCursor.getColumnIndexOrThrow("name");
            int instructionsColumn =
                    recipesCursor.getColumnIndexOrThrow("instructions");

            while (recipesCursor.moveToNext()) {
                long recipeId = recipesCursor.getLong(idColumn);
                String name = recipesCursor.getString(nameColumn);
                String instructions = recipesCursor.getString(instructionsColumn);

                if (canMakeRecipe(recipeId)) {
                    matchingRecipes.add(
                            new Recipe(recipeId, name, instructions)
                    );
                    recipeNames.add(name);
                }
            }
        } finally {
            recipesCursor.close();
        }

        if (matchingRecipes.isEmpty()) {
            recipeListView.setVisibility(ListView.GONE);
            emptyRecipesMessage.setVisibility(TextView.VISIBLE);
            return;
        }

        recipeListView.setVisibility(ListView.VISIBLE);
        emptyRecipesMessage.setVisibility(TextView.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                recipeNames
        );

        recipeListView.setAdapter(adapter);

        recipeListView.setOnItemClickListener((parent, view, position, id) -> {
            Recipe recipe = matchingRecipes.get(position);

            Intent intent = new Intent(
                    RecipeListActivity.this,
                    RecipeDetailActivity.class
            );
            intent.putExtra(
                    RecipeDetailActivity.EXTRA_RECIPE_NAME,
                    recipe.name
            );
            intent.putExtra(
                    RecipeDetailActivity.EXTRA_RECIPE_INSTRUCTIONS,
                    recipe.instructions
            );

            startActivity(intent);
        });
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
        Cursor pantryCursor = databaseHelper.getPantryItems();

        try {
            int nameColumn = pantryCursor.getColumnIndexOrThrow("name");
            int quantityColumn =
                    pantryCursor.getColumnIndexOrThrow("quantity");
            int unitColumn = pantryCursor.getColumnIndexOrThrow("unit");

            while (pantryCursor.moveToNext()) {
                String pantryName = pantryCursor.getString(nameColumn);
                double pantryQuantity =
                        pantryCursor.getDouble(quantityColumn);
                String pantryUnit = pantryCursor.getString(unitColumn);

                if (normalizeIngredientName(pantryName).equals(wantedName)
                        && normalizeUnit(pantryUnit).equals(wantedUnit)) {
                    double baseQuantity =
                            toBaseQuantity(pantryQuantity, pantryUnit);

                    if (baseQuantity >= 0) {
                        availableBaseQuantity += baseQuantity;
                    }
                }
            }
        } finally {
            pantryCursor.close();
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

        String canonicalUnit = normalizeUnit(unit);

        if (canonicalUnit.equals("piece")
                || canonicalUnit.equals("slice")
                || canonicalUnit.equals("gram")
                || canonicalUnit.equals("ml")) {
            return quantity;
        }

        return -1;
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