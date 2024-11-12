package com.example.milita;

import android.graphics.Bitmap;

public class Certificate {
    private String id;
    private String name;
    private String session;
    private String dateCreated;
    private Bitmap profileImage;

    // Constructor bao gồm ảnh đại diện
    public Certificate (String name, String session, String dateCreated, Bitmap profileImage) {
        this.name = name;
        this.session = session;
        this.dateCreated = dateCreated;
        this.profileImage = profileImage;
    }

    // Các getter và setter
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSession() { return session; }
    public String getDateCreated() { return dateCreated; }
    public Bitmap getProfileImage() { return profileImage; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSession(String session) { this.session = session; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }
    public void setProfileImage(Bitmap profileImage) { this.profileImage = profileImage; }
}
