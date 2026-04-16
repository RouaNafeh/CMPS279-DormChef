package database;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.dormchef.R;
import com.example.dormchef.models.Recipe;
import com.example.dormchef.models.RecipeContent;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "dormchef.db";
    private static final int DB_VERSION = 4;
    public static final String TABLE_RECIPES    = "recipes";
    public static final String COL_ID           = "id";
    public static final String COL_NAME         = "name";
    public static final String COL_TIME         = "time";
    public static final String COL_BUDGET       = "budget";
    public static final String COL_EQUIPMENT    = "equipment";
    public static final String COL_INGREDIENTS  = "ingredients";
    public static final String COL_STEPS        = "steps";
    public static final String COL_IMAGE_URI    = "imageUri";
    public static final String COL_IMAGE        = "image";
    public static final String COL_IS_FAVOURITE = "isFavourite";
    public static final String COL_IS_USER_CREATED = "isUserCreated";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_RECIPES + " (" +
                COL_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME         + " TEXT, " +
                COL_TIME         + " TEXT, " +
                COL_IMAGE        + " INTEGER, " +
                COL_BUDGET       + " TEXT, " +
                COL_EQUIPMENT    + " TEXT, " +
                COL_INGREDIENTS  + " TEXT DEFAULT '', " +
                COL_STEPS        + " TEXT DEFAULT '', " +
                COL_IMAGE_URI    + " TEXT DEFAULT '', " +
                COL_IS_FAVOURITE + " INTEGER DEFAULT 0, " +
                COL_IS_USER_CREATED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    private Recipe cursorToRecipe(Cursor cursor) {
        return new Recipe(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_IMAGE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_BUDGET)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EQUIPMENT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_INGREDIENTS)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_URI)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_FAVOURITE)) == 1
        );
    }

    public long insertRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME,         recipe.getName());
        values.put(COL_TIME,         recipe.getTime());
        values.put(COL_BUDGET,       recipe.getBudget());
        values.put(COL_EQUIPMENT,    recipe.getEquipment());
        values.put(COL_INGREDIENTS,  recipe.getIngredients());
        values.put(COL_STEPS,        recipe.getSteps());
        values.put(COL_IMAGE_URI,    recipe.getImageUri());
        values.put(COL_IMAGE,        recipe.getImageResId());
        values.put(COL_IS_FAVOURITE, recipe.isFavourite() ? 1 : 0);
        values.put(COL_IS_USER_CREATED, 0);
        long result = db.insert(TABLE_RECIPES, null, values);
        db.close();
        return result;
    }

    public List<Recipe> getAllRecipes() {
        return getRecipesWhere(COL_IS_USER_CREATED + "=0", null, null);
    }
    public List<Recipe> getFilteredRecipes(List<String> selectedIngredients, List<String> selectedEquipment,
                                           int maxTimeMinutes,
                                           String budget) {
        List<Recipe> all      = getAllRecipes();
        List<Recipe> filtered = new ArrayList<>();

        for (Recipe recipe : all) {
            if (!selectedIngredients.isEmpty()) {
                RecipeContent.Details details =
                        RecipeContent.getDetails(recipe.getName());
                List<String> recipeIngredients = details.getIngredients();

                StringBuilder ingredientBlob = new StringBuilder();
                for (String ing : recipeIngredients) {
                    ingredientBlob.append(ing.toLowerCase()).append(" ");
                }
                String blob = ingredientBlob.toString();

                boolean anyMatch = false;
                for (String selected : selectedIngredients) {
                    if (blob.contains(selected.toLowerCase())) {
                        anyMatch = true;
                        break;
                    }
                }
                if (!anyMatch) continue;
            }

            if (!selectedEquipment.isEmpty()) {
                String equipLower = recipe.getEquipment().toLowerCase();
                boolean hasAll = true;
                for (String item : selectedEquipment) {
                    if (!equipLower.contains(item.toLowerCase())) {
                        hasAll = false;
                        break;
                    }
                }
                if (!hasAll) continue;
            }

            if (maxTimeMinutes != -1) {
                if (parseMinutes(recipe.getTime()) > maxTimeMinutes) continue;
            }

            if (budget != null && !budget.isEmpty()) {
                if (!recipe.getBudget().equalsIgnoreCase(budget)) continue;
            }

            filtered.add(recipe);
        }

        return filtered;
    }

    private int parseMinutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return Integer.MAX_VALUE;
        try {
            int number = Integer.parseInt(timeStr.trim().split("\\s+")[0]);
            return timeStr.toLowerCase().contains("hr") ? number * 60 : number;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public void insertSampleRecipes() {
        insertRecipe(new Recipe(
                R.drawable.salad,
                "Greek Salad",
                "15 min",
                "Low",
                "Bowl, Knife",
                "Tomatoes,Cucumber,Feta,Olives,Olive oil,Oregano",
                "Wash and chop the tomatoes and cucumber into bite-sized pieces.|Add the vegetables to a bowl with olives and crumbled feta.|Drizzle with olive oil and season with oregano, salt, and pepper.|Toss lightly and serve fresh.",
                "",
                false
        ));

        insertRecipe(new Recipe(
                R.drawable.pasta,
                "Easy Pasta",
                "20 min",
                "Low",
                "Pot, Strainer",
                "Pasta,Garlic,Olive oil,Parmesan,Salt,Pepper",
                "Boil the pasta in salted water until tender, then drain.|Warm olive oil in a pot and cook the garlic for 30 seconds.|Return the pasta to the pot and toss with the garlic oil.|Season with salt and pepper, then finish with parmesan.",
                "",
                false
        ));

        insertRecipe(new Recipe(
                R.drawable.sandwich,
                "Dorm Sandwich",
                "5 min",
                "Low",
                "Knife",
                "Bread,Turkey,Cheese,Lettuce,Tomato,Mayo",
                "Lay out the bread slices and spread mayo on one side.|Layer turkey, cheese, lettuce, and tomato on the bread.|Close the sandwich and press lightly.|Slice in half and serve.",
                "",
                false
        ));

        insertRecipe(new Recipe(
                R.drawable.cake,
                "Mug Cake",
                "5 min",
                "Low",
                "Mug, Microwave",
                "Flour,Sugar,Cocoa powder,Milk,Oil,Baking powder",
                "Mix all ingredients in a microwave-safe mug until smooth.|Microwave for 60 to 90 seconds until the cake rises.|Let it rest for a minute before eating.|Top with chocolate chips or powdered sugar if available.",
                "",
                false
        ));

        insertRecipe(new Recipe(
                R.drawable.omelette,
                "Omelette",
                "10 min",
                "Low",
                "Pan, Spatula",
                "Eggs,Milk,Butter,Cheese,Salt,Pepper",
                "Whisk the eggs with a splash of milk, salt, and pepper.|Heat butter in a pan over medium heat.|Pour in the eggs and gently pull the edges inward as they set.|Add cheese, fold the omelette, and cook for one more minute.",
                "",
                false
        ));
    }

    public boolean isRecipesTableEmpty() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECIPES, null);
        boolean isEmpty = true;
        if (cursor.moveToFirst()) isEmpty = cursor.getInt(0) == 0;
        cursor.close();
        db.close();
        return isEmpty;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COL_INGREDIENTS + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COL_STEPS + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COL_IS_USER_CREATED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COL_IMAGE_URI + " TEXT DEFAULT ''");
        }
    }

    public void updateFavourite(int recipeId, boolean isFavourite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_FAVOURITE, isFavourite ? 1 : 0);
        db.update(TABLE_RECIPES, values, COL_ID + "=?",
                new String[]{String.valueOf(recipeId)});
        db.close();
    }

    public List<Recipe> getFavouriteRecipes() {
        return getRecipesWhere(
                COL_IS_FAVOURITE + "=1 AND " + COL_IS_USER_CREATED + "=0",
                null,
                null
        );
    }

    public long insertUserRecipe(String name, String time, String budget, String equipment,
                                 String ingredients, String steps, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_TIME, time);
        values.put(COL_BUDGET, budget);
        values.put(COL_EQUIPMENT, equipment);
        values.put(COL_INGREDIENTS, ingredients);
        values.put(COL_STEPS, steps);
        values.put(COL_IMAGE_URI, imageUri);
        values.put(COL_IMAGE, R.drawable.logo_cropped);
        values.put(COL_IS_FAVOURITE, 0);
        values.put(COL_IS_USER_CREATED, 1);
        long result = db.insert(TABLE_RECIPES, null, values);
        db.close();
        return result;
    }

    public List<Recipe> getUserRecipes() {
        return getRecipesWhere(COL_IS_USER_CREATED + "=1", null, COL_ID + " DESC");
    }

    private List<Recipe> getRecipesWhere(String selection, String[] selectionArgs, String orderBy) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, selection, selectionArgs, null, null, orderBy);
        try {
            if (cursor.moveToFirst()) {
                do {
                    recipes.add(cursorToRecipe(cursor));
                } while (cursor.moveToNext());
            }
            return recipes;
        } finally {
            cursor.close();
            db.close();
        }
    }
}
