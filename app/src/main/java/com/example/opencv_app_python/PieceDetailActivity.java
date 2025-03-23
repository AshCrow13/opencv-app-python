package com.example.opencv_app_python;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class PieceDetailActivity extends AppCompatActivity {

    private ImageView pieceImageView;
    private TextView pieceNameTextView;
    private TextView pieceTypeTextView;
    private TextView pieceDateTextView;
    private TextView pieceDimensionsTextView;
    private TextView pieceMaterialTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piece_detail);

        // Inicializar vistas
        pieceImageView = findViewById(R.id.pieceImageView);
        pieceNameTextView = findViewById(R.id.pieceNameTextView);
        pieceTypeTextView = findViewById(R.id.pieceTypeTextView);
        pieceDateTextView = findViewById(R.id.pieceDateTextView);
        pieceDimensionsTextView = findViewById(R.id.pieceDimensionsTextView);
        pieceMaterialTextView = findViewById(R.id.pieceMaterialTextView);

        // Obtener ID de la pieza desde el intent
        Intent intent = getIntent();
        if (intent != null) {
            String pieceId = intent.getStringExtra("piece_id");
            if (pieceId != null) {
                // Obtener la pieza desde la base de datos
                DatabaseHelper databaseHelper = new DatabaseHelper(this);
                Piece piece = databaseHelper.getPieceById(pieceId);

                if (piece != null) {
                    // Mostrar la imagen con Glide
                    Glide.with(this).load(piece.getImageUrl()).into(pieceImageView);

                    // Mostrar los detalles
                    pieceNameTextView.setText(piece.getName());
                    pieceTypeTextView.setText("Tipo: " + piece.getType());
                    pieceDateTextView.setText("Fecha: " + piece.getDate());
                    pieceDimensionsTextView.setText("Dimensiones: " + piece.getDimensions());
                    pieceMaterialTextView.setText("Material: " + piece.getMaterial());
                }
            }
        }
    }
}
