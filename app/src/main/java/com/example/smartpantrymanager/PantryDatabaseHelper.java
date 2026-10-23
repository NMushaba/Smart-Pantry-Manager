package com.example.smartpantrymanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PantryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_pantry.db";
    private static final int DATABASE_VERSION = 2;
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
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2 && tableExists(database, PANTRY_TABLE)) {
            database.execSQL(
                    "ALTER TABLE " + PANTRY_TABLE +
                            " RENAME TO " + LEGACY_PANTRY_TABLE
            );

            createTables(database);
            copyLegacyPantryItems(database);
            database.execSQL("DROP TABLE " + LEGACY_PANTRY_TABLE);
        } else {
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