# DormChef

DormChef is an Android app for students who want quick, simple meals with limited time and equipment. The app lets users browse recipes, save favorites, add their own recipes, and follow a cooking timer step by step.

## What It Does

- Browse built-in recipes
- Search recipes by name
- View recipe details, ingredients, and steps
- Save favorite recipes
- Add custom recipes in My Recipes
- Use Cooking Mode with a timer and step navigation
- Filter recipes by ingredients, time, budget, and equipment

## Stack

- Java
- Android Studio
- SQLite
- ViewBinding
- RecyclerView
- Material Components

## Run The App

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Run the app on an emulator or Android device.

The launcher screen starts from `LandingActivity`.

## Project Layout

- `app/src/main/java/com/example/dormchef/activities/`: app screens
- `app/src/main/java/com/example/dormchef/adapters/`: RecyclerView adapters
- `app/src/main/java/com/example/dormchef/models/`: recipe models and content helpers
- `app/src/main/java/database/`: SQLite database helper
- `app/src/main/res/layout/`: XML layouts

## Current Status

- Home, Favorites, My Recipes, Recipe Detail, Add Recipe, and Timer flows are implemented.
- The project currently builds successfully with Gradle.
- User-created recipes are stored locally in SQLite.

## Known Limitations

- Filtering logic is still being refined.
- Automated test coverage is still minimal.
- The repo still contains both Groovy and Kotlin Gradle files.
