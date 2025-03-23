package com.example.opencv_app_python;

public class Piece {
    private String id;
    private String name;
    private String type;  // Macho o Hembra
    private String date;
    private String imageUrl;
    private String dimensions;
    private String material;

    // Constructor vacío necesario para Firebase o deserialización
    public Piece() {}

    // Constructor con parámetros
    public Piece(String id, String name, String type, String date, String imageUrl, String dimensions, String material) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.date = date;
        this.imageUrl = imageUrl;
        this.dimensions = dimensions;
        this.material = material;
    }

    // Métodos Getter y Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
}
