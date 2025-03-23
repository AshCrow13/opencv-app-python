package com.example.opencv_app_python;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pieces.db";
    private static final int DATABASE_VERSION = 1;

    // Nombre de la tabla y columnas
    private static final String TABLE_PIECES = "pieces";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TYPE = "type"; // Macho / Hembra
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_DIMENSIONS = "dimensions";
    private static final String COLUMN_MATERIAL = "material";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_PIECES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_TYPE + " TEXT, "
                + COLUMN_DATE + " TEXT, "
                + COLUMN_IMAGE_PATH + " TEXT, "
                + COLUMN_DIMENSIONS + " TEXT, "
                + COLUMN_MATERIAL + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PIECES);
        onCreate(db);
    }

    // Método para insertar una nueva pieza
    public boolean insertPiece(String name, String type, String date, String imagePath, String dimensions, String material) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_DIMENSIONS, dimensions);
        values.put(COLUMN_MATERIAL, material);

        long result = db.insert(TABLE_PIECES, null, values);
        db.close();
        return result != -1; // Retorna true si la inserción fue exitosa
    }

    // Método para obtener todas las piezas
    public List<Piece> getAllPieces() {
        List<Piece> pieceList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PIECES, null);

        if (cursor.moveToFirst()) {
            do {
                Piece piece = new Piece(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIMENSIONS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATERIAL))
                );
                pieceList.add(piece);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return pieceList;
    }

    // Método para actualizar una pieza
    public void updatePieceName(String id, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        db.update(TABLE_PIECES, values, COLUMN_ID + " = ?", new String[]{id});
        db.close();
    }


    // Método para eliminar una pieza
    public void deletePiece(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PIECES, COLUMN_ID + " = ?", new String[]{id});
        db.close();
    }

    public Piece getPieceById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("pieces", null, "id = ?", new String[]{id}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Piece piece = new Piece(
                    cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    cursor.getString(cursor.getColumnIndexOrThrow("image_path")),
                    cursor.getString(cursor.getColumnIndexOrThrow("dimensions")),
                    cursor.getString(cursor.getColumnIndexOrThrow("material"))
            );
            cursor.close();
            db.close();
            return piece;
        }

        return null;
    }

    public List<String> getAllImagePaths() {
        List<String> paths = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT image_path FROM pieces", null);

        if (cursor.moveToFirst()) {
            do {
                paths.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return paths;
    }

    public String getClassificationByPath(String path) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT type FROM pieces WHERE image_path = ?", new String[]{path}); // <-- corregido

        if (cursor.moveToFirst()) {
            String tipo = cursor.getString(0);
            cursor.close();
            return tipo;
        }

        cursor.close();
        return "Desconocido";
    }

    public void updatePiecePath(String id, String newPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("image_path", newPath);
        db.update("pieces", values, "id = ?", new String[]{id});
        db.close();
    }

}
