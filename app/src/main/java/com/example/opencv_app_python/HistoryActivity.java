package com.example.opencv_app_python;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;
    private Spinner filterSpinner;
    private PieceListAdapter adapter;
    private List<Piece> pieceList;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.listViewPieces);
        filterSpinner = findViewById(R.id.spinnerFilter);
        databaseHelper = new DatabaseHelper(this);

        loadPieces("");

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Piece selectedPiece = pieceList.get(position);
            showPieceOptions(selectedPiece);
        });
    }

    private void loadPieces(String filter) {
        pieceList = databaseHelper.getAllPieces();
        adapter = new PieceListAdapter(this, pieceList);
        listView.setAdapter(adapter);
    }

    private void showPieceOptions(Piece piece) {
        Intent intent = new Intent(this, PieceDetailActivity.class);
        intent.putExtra("piece_id", piece.getId());
        startActivity(intent);
    }

    public void viewPieceDetail(Piece piece) {
        Intent intent = new Intent(this, PieceDetailActivity.class);
        intent.putExtra("piece_id", piece.getId());
        startActivity(intent);
    }

    public void renamePiece(Piece piece, String newName) {
        // Renombrar archivo físico
        File oldFile = new File(piece.getImageUrl());

        // Armar nuevo nombre de archivo (sin espacios ni caracteres raros)
        String safeName = newName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File newFile = new File(oldFile.getParent(), safeName + ".png");

        boolean renamed = oldFile.renameTo(newFile);
        if (renamed) {
            // Actualizar en la base de datos
            databaseHelper.updatePieceName(piece.getId(), newName);
            databaseHelper.updatePiecePath(piece.getId(), newFile.getAbsolutePath());

            // Actualizar el objeto Piece en memoria
            piece.setName(newName);
            piece.setImageUrl(newFile.getAbsolutePath());

            // Notificar al adaptador para refrescar la lista
            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Pieza renombrada correctamente", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error: No se pudo renombrar el archivo", Toast.LENGTH_LONG).show();
        }
    }



    public void deletePiece(Piece piece) {
        databaseHelper.deletePiece(piece.getId());
        loadPieces(""); // Recargar la lista después de eliminar
    }

}
