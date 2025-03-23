package com.example.opencv_app_python;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;

public class PieceListAdapter extends BaseAdapter {

    private Context context;
    private List<Piece> pieces;

    public PieceListAdapter(Context context, List<Piece> pieces) {
        this.context = context;
        this.pieces = pieces;
    }

    @Override
    public int getCount() {
        return pieces.size();
    }

    @Override
    public Object getItem(int position) {
        return pieces.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflar el layout si es necesario
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.piece_list_item, parent, false);
        }

        // Obtener vistas
        ImageView pieceImageView = convertView.findViewById(R.id.pieceImageView);
        TextView pieceNameTextView = convertView.findViewById(R.id.pieceNameTextView);
        TextView pieceTypeTextView = convertView.findViewById(R.id.pieceTypeTextView);
        ImageButton optionsButton = convertView.findViewById(R.id.optionsButton);

        // Obtener pieza actual
        Piece piece = pieces.get(position);

        // Establecer nombre y tipo
        pieceNameTextView.setText(piece.getName());
        pieceTypeTextView.setText("Tipo: " + piece.getType());

        // Mostrar imagen
        Bitmap bitmap = BitmapFactory.decodeFile(piece.getImageUrl());
        if (bitmap != null) {
            pieceImageView.setImageBitmap(bitmap);
        } else {
            pieceImageView.setImageResource(R.drawable.ic_launcher_background);
        }

        // Menú de opciones
        optionsButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, optionsButton);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.piece_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.option_view) {
                    ((HistoryActivity) context).viewPieceDetail(piece);
                    return true;

                } else if (itemId == R.id.option_rename) {
                    // Mostrar diálogo para renombrar
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Renombrar pieza");

                    final EditText input = new EditText(context);
                    input.setText(piece.getName());
                    builder.setView(input);

                    builder.setPositiveButton("Aceptar", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            ((HistoryActivity) context).renamePiece(piece, newName);
                            Toast.makeText(context, "Pieza renombrada a \"" + newName + "\"", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

                    builder.show();
                    return true;

                } else if (itemId == R.id.option_delete) {
                    // Mostrar diálogo de confirmación antes de eliminar
                    AlertDialog.Builder confirmDialog = new AlertDialog.Builder(context);
                    confirmDialog.setTitle("Eliminar pieza");
                    confirmDialog.setMessage("¿Estás seguro de que quieres eliminar esta pieza?");

                    confirmDialog.setPositiveButton("Eliminar", (dialog, which) -> {
                        ((HistoryActivity) context).deletePiece(piece);
                        Toast.makeText(context, "Pieza eliminada", Toast.LENGTH_SHORT).show();
                    });

                    confirmDialog.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

                    confirmDialog.show();
                    return true;

                }

                return false;
            });

            popup.show();
        });

        return convertView;
    }
}
