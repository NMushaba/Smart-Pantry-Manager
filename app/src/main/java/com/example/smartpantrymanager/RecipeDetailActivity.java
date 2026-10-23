package com.example.smartpantrymanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_NAME = "recipe_name";
    public static final String EXTRA_RECIPE_INSTRUCTIONS = "recipe_instructions";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        TextView titleView = findViewById(R.id.recipeDetailTitle);
        TextView instructionsView = findViewById(R.id.recipeInstructions);
        Button backButton = findViewById(R.id.backToRecipesButton);

        String recipeName = getIntent().getStringExtra(EXTRA_RECIPE_NAME);
        String instructions =
                getIntent().getStringExtra(EXTRA_RECIPE_INSTRUCTIONS);

        if (recipeName != null && !recipeName.trim().isEmpty()) {
            titleView.setText(recipeName);
        }

        if (instructions != null && !instructions.trim().isEmpty()) {
            instructionsView.setText(instructions);
        } else {
            instructionsView.setText("Instructions are unavailable.");
        }

        backButton.setOnClickListener(view -> finish());
    }
}