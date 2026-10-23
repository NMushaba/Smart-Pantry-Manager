# Smart Pantry Manager

Smart Pantry Manager is a simple Android app that helps users keep track of food ingredients at home and find recipes they can make.

## Features

- Add, edit, and remove pantry ingredients.
- Save ingredient names, quantities, and units in a local SQLite database.
- Keep pantry data after closing and reopening the app.
- Browse recipes that match the ingredients and quantities in the pantry.
- View cooking instructions for a selected recipe.
- Clear the pantry from the Settings screen.
- Use a collection of 18 recipes saved in the database.

## Recipe matching

The app checks that the pantry contains every required ingredient in the amount needed for a recipe. For example, Tomato and egg scramble requires one tomato and two eggs. The app also recognises common singular and plural ingredient names and equivalent units such as grams and kilograms.

## Built with

- Java
- Android Studio
- SQLite

## How to run

1. Download or clone this repository.
2. Open the project in Android Studio.
3. Wait for Gradle sync to finish.
4. Select an Android emulator or connect an Android device.
5. Click **Run** to build and launch the app.

## Database

The app uses SQLite to store pantry ingredients and recipe information locally on the device. SQLite is suitable for this project because it stores structured data directly on the device and works without an internet connection.