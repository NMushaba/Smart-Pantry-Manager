package com.example.smartpantrymanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class IngredientFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "ingredient_id";
    public static final String EXTRA_NAME = "ingredient_name";
    public static final String EXTRA_QUANTITY = "ingredient_quantity";
    public static final String EXTRA_UNIT = "ingredient_unit";

    private PantryDatabaseHelper databaseHelper;
    private EditText nameInput;
    private EditText quantityInput;
    private EditText unitInput;

    private long ingredientId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_form);

        databaseHelper = new PantryDatabaseHelper(this);

        TextView titleView = findViewById(R.id.ingredientFormTitle);
        nameInput = findViewById(R.id.ingredientNameInput);
        quantityInput = findViewById(R.id.ingredientQuantityInput);
        unitInput = findViewById(R.id.ingredientUnitInput);

        ingredientId = getIntent().getLongExtra(EXTRA_ID, -1);

        if (ingredientId >= 0) {
            titleView.setText("Edit ingredient");
            nameInput.setText(getIntent().getStringExtra(EXTRA_NAME));
            quantityInput.setText(
                    String.valueOf(getIntent().getDoubleExtra(EXTRA_QUANTITY, 0))
            );
            unitInput.setText(getIntent().getStringExtra(EXTRA_UNIT));
        } else {
            titleView.setText("Add ingredient");
        }

        Button saveButton = findViewById(R.id.saveIngredientButton);
        saveButton.setOnClickListener(view -> saveIngredient());

        Button cancelButton = findViewById(R.id.cancelIngredientButton);
        cancelButton.setOnClickListener(view -> finish());
    }

    private void saveIngredient() {
        String name = nameInput.getText().toString().trim();
        String quantityText = quantityInput.getText().toString().trim();
        String unit = unitInput.getText().toString().trim();

        if (name.isEmpty() || quantityText.isEmpty() || unit.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please fill in all fields.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        double quantity;

        try {
            quantity = Double.parseDouble(quantityText);
        } catch (NumberFormatException exception) {
            Toast.makeText(
                    this,
                    "Enter a valid quantity.",
                    Toast.LENGTH_SHORT
            ).show();
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

        if (ingredientId >= 0) {
            int updatedRows = databaseHelper.updatePantryItem(
                    ingredientId,
                    name,
                    quantity,
                    unit
            );

            if (updatedRows > 0) {
                Toast.makeText(this, "Ingredient updated.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(
                        this,
                        "The ingredient could not be updated.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            long newId = databaseHelper.addPantryItem(name, quantity, unit);

            if (newId >= 0) {
                Toast.makeText(this, "Ingredient added.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(
                        this,
                        "The ingredient could not be saved.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}