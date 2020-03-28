package com.example.takepicture;

public class mDatabase {

    private int id;
    private String image, img_name;

    public mDatabase(int id, String image, String img_name) {
        this.id = id;
        this.image = image;
        this.img_name = img_name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImg_name() {
        return img_name;
    }

    public void setImg_name(String img_name) {
        this.img_name = img_name;
    }
}
