package com.example.smartpantrymanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PantryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_pantry.db";
    private static final int DATABASE_VERSION = 3;
    private static final String LEGACY_PANTRY_TABLE = "pantry_items_legacy";

    public static final String PANTRY_TABLE = "pantry_items";
    public static final String RECIPES_TABLE = "recipes";
    public static final String RECIPE_INGREDIENTS_TABLE = "recipe_ingredients";

    public PantryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        createTables(database);
    }

    @Override
    public void onOpen(SQLiteDatabase database) {
        super.onOpen(database);
        seedRecipesIfEmpty(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2 && tableExists(database, PANTRY_TABLE)) {
            database.execSQL(
                    "ALTER TABLE " + PANTRY_TABLE
                            + " RENAME TO " + LEGACY_PANTRY_TABLE
            );

            createTables(database);
            copyLegacyPantryItems(database);
            database.execSQL("DROP TABLE " + LEGACY_PANTRY_TABLE);
        } else {
            createTables(database);
        }

        if (oldVersion < 3) {
            database.execSQL(
                    "DROP TABLE IF EXISTS " + RECIPE_INGREDIENTS_TABLE
            );
            database.execSQL(
                    "DROP TABLE IF EXISTS " + RECIPES_TABLE
            );

            createTables(database);
        }
    }

    private void createTables(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + PANTRY_TABLE + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "quantity REAL NOT NULL CHECK(quantity > 0), " +
                        "unit TEXT NOT NULL)"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + RECIPES_TABLE + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "instructions TEXT NOT NULL)"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + RECIPE_INGREDIENTS_TABLE + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "recipe_id INTEGER NOT NULL, " +
                        "ingredient_name TEXT NOT NULL, " +
                        "required_quantity REAL NOT NULL CHECK(required_quantity > 0), " +
                        "unit TEXT NOT NULL, " +
                        "FOREIGN KEY(recipe_id) REFERENCES " + RECIPES_TABLE +
                        "(_id) ON DELETE CASCADE)"
        );
    }

    private boolean tableExists(SQLiteDatabase database, String tableName) {
        Cursor cursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{tableName}
        );

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    private void copyLegacyPantryItems(SQLiteDatabase database) {
        Cursor cursor = database.rawQuery(
                "SELECT name, quantity, unit FROM " + LEGACY_PANTRY_TABLE,
                null
        );

        try {
            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put("name", cursor.getString(0));
                values.put("quantity", cursor.getDouble(1));
                values.put("unit", cursor.getString(2));

                database.insertOrThrow(PANTRY_TABLE, null, values);
            }
        } finally {
            cursor.close();
        }
    }

    private void seedRecipesIfEmpty(SQLiteDatabase database) {
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + RECIPES_TABLE,
                null
        );

        try {
            if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                return;
            }
        } finally {
            cursor.close();
        }

        database.beginTransaction();

        try {
            seedRecipe(database, "Tomato and egg scramble",
                    "1. Chop the tomato. 2. Beat the eggs. 3. Cook the tomato, add the eggs, and stir until fully cooked.",
                    new SeedIngredient("tomato", 1, "pieces"),
                    new SeedIngredient("egg", 2, "pieces"));

            seedRecipe(database, "Egg toast",
                    "1. Toast the bread. 2. Cook the egg until done. 3. Serve the egg on the toast.",
                    new SeedIngredient("egg", 1, "pieces"),
                    new SeedIngredient("bread", 2, "slices"));

            seedRecipe(database, "Tomato and cucumber salad",
                    "1. Wash and chop the tomato and cucumber. 2. Mix them together and serve.",
                    new SeedIngredient("tomato", 1, "pieces"),
                    new SeedIngredient("cucumber", 1, "pieces"));

            seedRecipe(database, "Vegetable omelette",
                    "1. Beat the eggs. 2. Chop the tomato and onion. 3. Cook the vegetables with the eggs until done.",
                    new SeedIngredient("egg", 2, "pieces"),
                    new SeedIngredient("tomato", 1, "pieces"),
                    new SeedIngredient("onion", 0.5, "pieces"));

            seedRecipe(database, "Banana pancakes",
                    "1. Mash the banana. 2. Mix with eggs and flour. 3. Cook small pancakes in a pan.",
                    new SeedIngredient("banana", 1, "pieces"),
                    new SeedIngredient("egg", 2, "pieces"),
                    new SeedIngredient("flour", 50, "grams"));

            seedRecipe(database, "Peanut butter toast",
                    "1. Toast the bread. 2. Spread peanut butter on each slice.",
                    new SeedIngredient("bread", 2, "slices"),
                    new SeedIngredient("peanut butter", 30, "grams"));

            seedRecipe(database, "Tomato pasta",
                    "1. Cook the pasta. 2. Chop and cook the tomatoes and onion in olive oil. 3. Mix with the pasta.",
                    new SeedIngredient("pasta", 100, "grams"),
                    new SeedIngredient("tomato", 2, "pieces"),
                    new SeedIngredient("onion", 1, "pieces"),
                    new SeedIngredient("olive oil", 10, "ml"));

            seedRecipe(database, "Fried rice",
                    "1. Cook the rice. 2. Cook the egg and carrot. 3. Add rice, peas, and soy sauce, then stir.",
                    new SeedIngredient("rice", 150, "grams"),
                    new SeedIngredient("egg", 1, "pieces"),
                    new SeedIngredient("carrot", 1, "pieces"),
                    new SeedIngredient("peas", 50, "grams"),
                    new SeedIngredient("soy sauce", 10, "ml"));

            seedRecipe(database, "Vegetable stir-fry",
                    "1. Chop and cook the carrot and broccoli. 2. Add oil and soy sauce. 3. Stir until tender.",
                    new SeedIngredient("carrot", 1, "pieces"),
                    new SeedIngredient("broccoli", 100, "grams"),
                    new SeedIngredient("soy sauce", 15, "ml"),
                    new SeedIngredient("olive oil", 10, "ml"));

            seedRecipe(database, "Potato omelette",
                    "1. Chop and cook the potato and onion. 2. Add beaten eggs. 3. Cook until the eggs are done.",
                    new SeedIngredient("potato", 1, "pieces"),
                    new SeedIngredient("egg", 2, "pieces"),
                    new SeedIngredient("onion", 0.5, "pieces"));

            seedRecipe(database, "Fruit salad",
                    "1. Wash and chop the apple, banana, and orange. 2. Mix and serve.",
                    new SeedIngredient("apple", 1, "pieces"),
                    new SeedIngredient("banana", 1, "pieces"),
                    new SeedIngredient("orange", 1, "pieces"));

            seedRecipe(database, "Chicken sandwich",
                    "1. Cook the chicken thoroughly. 2. Put chicken, lettuce, and mayonnaise between slices of bread.",
                    new SeedIngredient("bread", 2, "slices"),
                    new SeedIngredient("chicken", 100, "grams"),
                    new SeedIngredient("lettuce", 20, "grams"),
                    new SeedIngredient("mayonnaise", 15, "grams"));

            seedRecipe(database, "Tuna salad",
                    "1. Drain the tuna. 2. Chop the lettuce and cucumber. 3. Mix with mayonnaise.",
                    new SeedIngredient("tuna", 80, "grams"),
                    new SeedIngredient("lettuce", 30, "grams"),
                    new SeedIngredient("cucumber", 0.5, "pieces"),
                    new SeedIngredient("mayonnaise", 10, "grams"));

            seedRecipe(database, "Banana oatmeal",
                    "1. Cook the oats with milk. 2. Slice the banana and add it to the oatmeal.",
                    new SeedIngredient("oats", 50, "grams"),
                    new SeedIngredient("milk", 200, "ml"),
                    new SeedIngredient("banana", 1, "pieces"));

            seedRecipe(database, "Grilled cheese",
                    "1. Put cheese between slices of bread. 2. Butter the outside. 3. Cook both sides until golden.",
                    new SeedIngredient("bread", 2, "slices"),
                    new SeedIngredient("cheese", 40, "grams"),
                    new SeedIngredient("butter", 10, "grams"));

            seedRecipe(database, "Vegetable soup",
                    "1. Chop the potato, carrot, and onion. 2. Simmer them in broth until tender.",
                    new SeedIngredient("potato", 1, "pieces"),
                    new SeedIngredient("carrot", 1, "pieces"),
                    new SeedIngredient("onion", 0.5, "pieces"),
                    new SeedIngredient("broth", 250, "ml"));

            seedRecipe(database, "Banana smoothie",
                    "1. Peel the banana. 2. Blend it with milk and honey until smooth.",
                    new SeedIngredient("banana", 1, "pieces"),
                    new SeedIngredient("milk", 200, "ml"),
                    new SeedIngredient("honey", 10, "grams"));

            seedRecipe(database, "Cheese omelette",
                    "1. Beat the eggs. 2. Cook them in a pan with butter. 3. Add cheese and fold the omelette.",
                    new SeedIngredient("egg", 2, "pieces"),
                    new SeedIngredient("cheese", 30, "grams"),
                    new SeedIngredient("butter", 5, "grams"));

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void seedRecipe(
            SQLiteDatabase database,
            String name,
            String instructions,
            SeedIngredient... ingredients
    ) {
        ContentValues recipeValues = new ContentValues();
        recipeValues.put("name", name);
        recipeValues.put("instructions", instructions);

        long recipeId = database.insertOrThrow(
                RECIPES_TABLE,
                null,
                recipeValues
        );

        for (SeedIngredient ingredient : ingredients) {
            ContentValues ingredientValues = new ContentValues();
            ingredientValues.put("recipe_id", recipeId);
            ingredientValues.put("ingredient_name", ingredient.name);
            ingredientValues.put("required_quantity", ingredient.quantity);
            ingredientValues.put("unit", ingredient.unit);

            database.insertOrThrow(
                    RECIPE_INGREDIENTS_TABLE,
                    null,
                    ingredientValues
            );
        }
    }

    private static class SeedIngredient {
        final String name;
        final double quantity;
        final String unit;

        SeedIngredient(String name, double quantity, String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    public long addPantryItem(String name, double quantity, String unit) {
        validateItem(name, quantity, unit);

        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("quantity", quantity);
        values.put("unit", unit.trim());

        return getWritableDatabase().insert(PANTRY_TABLE, null, values);
    }

    public Cursor getPantryItems() {
        return getReadableDatabase().query(
                PANTRY_TABLE,
                new String[]{"_id", "name", "quantity", "unit"},
                null,
                null,
                null,
                null,
                "name COLLATE NOCASE ASC"
        );
    }

    public int updatePantryItem(long id, String name, double quantity, String unit) {
        validateItem(name, quantity, unit);

        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("quantity", quantity);
        values.put("unit", unit.trim());

        return getWritableDatabase().update(
                PANTRY_TABLE,
                values,
                "_id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int deletePantryItem(long id) {
        return getWritableDatabase().delete(
                PANTRY_TABLE,
                "_id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int clearPantry() {
        return getWritableDatabase().delete(PANTRY_TABLE, null, null);
    }

    public long addRecipe(String name, String instructions) {
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("instructions", instructions.trim());

        return getWritableDatabase().insert(RECIPES_TABLE, null, values);
    }

    public Cursor getRecipes() {
        return getReadableDatabase().query(
                RECIPES_TABLE,
                new String[]{"_id", "name", "instructions"},
                null,
                null,
                null,
                null,
                "name COLLATE NOCASE ASC"
        );
    }

    public long addRecipeIngredient(
            long recipeId,
            String ingredientName,
            double requiredQuantity,
            String unit
    ) {
        if (ingredientName == null || ingredientName.trim().isEmpty()
                || requiredQuantity <= 0
                || unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a valid recipe ingredient.");
        }

        ContentValues values = new ContentValues();
        values.put("recipe_id", recipeId);
        values.put("ingredient_name", ingredientName.trim());
        values.put("required_quantity", requiredQuantity);
        values.put("unit", unit.trim());

        return getWritableDatabase().insert(RECIPE_INGREDIENTS_TABLE, null, values);
    }

    public Cursor getRecipeIngredients(long recipeId) {
        return getReadableDatabase().query(
                RECIPE_INGREDIENTS_TABLE,
                new String[]{
                        "_id",
                        "recipe_id",
                        "ingredient_name",
                        "required_quantity",
                        "unit"
                },
                "recipe_id = ?",
                new String[]{String.valueOf(recipeId)},
                null,
                null,
                "ingredient_name COLLATE NOCASE ASC"
        );
    }

    private void validateItem(String name, double quantity, String unit) {
        if (name == null || name.trim().isEmpty()
                || quantity <= 0
                || unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Enter an ingredient name, a quantity greater than zero, and a unit."
            );
        }
    }
}