package com.example.milita;

import android.graphics.Bitmap;

public class Student {
    private String id;
    private String name;
    private String studentClass;
    private Bitmap profileImage;

    // Constructor bao gồm ảnh đại diện
    public Student(String id, String name, String studentClass, Bitmap profileImage) {
        this.id = id;
        this.name = name;
        this.studentClass = studentClass;
        this.profileImage = profileImage;
    }

    // Các getter và setter
    public String getId() { return id; }
    public String getName() { return name; }
    public String getStudentClass() { return studentClass; }
    public Bitmap getProfileImage() { return profileImage; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }
    public void setProfileImage(Bitmap profileImage) { this.profileImage = profileImage; }
}
