package database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import com.example.dormchef.R;
import com.example.dormchef.models.Recipe;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "dormchef.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_NAME = "name";
    public static final String COL_ID = "id";
    public static final String COL_TIME = "time";
    public static final String COL_BUDGET = "budget";
    public static final String COL_EQUIPMENT = "equipment";
    public static final String COL_IMAGE = "image";
    public static final String COL_IS_FAVOURITE = "isFavourite";

    public DatabaseHelper(@Nullable Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        String createTable = "CREATE TABLE " + TABLE_RECIPES + " ( " +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_TIME + " TEXT, " +
                COL_IMAGE + " INTEGER, " +
                COL_BUDGET + " TEXT, " +
                COL_EQUIPMENT + " TEXT, " +
                COL_IS_FAVOURITE + " INTEGER DEFAULT 0)";

        db.execSQL(createTable);
    }

    public long insertRecipe(Recipe recipe){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_NAME, recipe.getName());
        values.put(COL_TIME, recipe.getTime());
        values.put(COL_BUDGET, recipe.getBudget());
        values.put(COL_EQUIPMENT, recipe.getEquipment());
        values.put(COL_IMAGE, recipe.getImageResId());
        values.put(COL_IS_FAVOURITE, recipe.isFavourite() ? 1 : 0);

        long result = db.insert(TABLE_RECIPES, null, values);
        db.close();
        return result;
    }

    public List<Recipe> getAllRecipes(){
        List<Recipe> recipeList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES, null);

        if(cursor.moveToFirst()){
            do{
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                int imageResId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IMAGE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME));
                String budget = cursor.getString(cursor.getColumnIndexOrThrow(COL_BUDGET));
                String equipment = cursor.getString(cursor.getColumnIndexOrThrow(COL_EQUIPMENT));
                boolean isFavourite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_FAVOURITE)) == 1;

                Recipe recipe = new Recipe(id, imageResId, name, time, budget, equipment, isFavourite);
                recipeList.add(recipe);
            }while(cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return recipeList;
    }

    public void insertSampleRecipes(){
        insertRecipe(new Recipe(R.drawable.salad, "Greek Salad", "15 min", "Low", "Bowl, Knife", false));
        insertRecipe(new Recipe(R.drawable.pasta, "Easy Pasta", "20 min", "Low", "Pot, Stove", false));
        insertRecipe(new Recipe(R.drawable.sandwich, "Dorm Sandwich", "10 min", "Low", "Knife", false));
        insertRecipe(new Recipe(R.drawable.cake, "Mug Cake", "8 min", "Low", "Microwave, Mug", false));
        insertRecipe(new Recipe(R.drawable.omelette, "Omelette", "12 min", "Low", "Pan, Stove", false));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " +TABLE_RECIPES);
        onCreate(db);
    }
}
