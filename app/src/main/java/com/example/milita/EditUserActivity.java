package com.example.milita;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.milita.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditUserActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private FirebaseStorage storage;
    private Uri selectedImageUri;
    private FirebaseFirestore db;
    Button btnCapture, btnSave;
    TextView username;
    CircleImageView profile_image;
    EditText et_username, et_birthday, et_email, et_phone, et_status;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnCapture = findViewById(R.id.btnCapture);
        btnSave = findViewById(R.id.btnSave);
        username = findViewById(R.id.username);
        profile_image = findViewById(R.id.profile_image);
        et_username = findViewById(R.id.et_username);
        et_birthday = findViewById(R.id.et_birthday);
        et_email = findViewById(R.id.et_email);
        et_phone = findViewById(R.id.et_phone);
        et_status = findViewById(R.id.et_status);
        db = FirebaseFirestore.getInstance();

        String userId = getIntent().getStringExtra("userId");
        loadUserProfile(userId);

        btnSave.setOnClickListener(v -> {
            profile_image.setDrawingCacheEnabled(true); // Bật cache
            profile_image.buildDrawingCache();
            Bitmap profileBitmap = profile_image.getDrawingCache();
            UpdateUserData(userId, profileBitmap);
        });

        btnCapture.setOnClickListener(v -> showImagePickerDialog());
    }
    private void showImagePickerDialog() {
        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ảnh")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    } else if (which == 1) {
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null && data.getData() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                selectedImageUri = getImageUri(imageBitmap);
                profile_image.setImageURI(selectedImageUri);
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    profile_image.setImageURI(selectedImageUri);
                } else {
                    Toast.makeText(this, "Lỗi khi chọn ảnh. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                }            }
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String birthday = documentSnapshot.getString("birthday");
                        String email = documentSnapshot.getString("email");
                        String name = documentSnapshot.getString("username");
                        String phone = documentSnapshot.getString("phone");
                        String status = documentSnapshot.getString("status");
                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");

                        username.setText(name);
                        et_username.setText(name);
                        et_birthday.setText(birthday);
                        et_email.setText(email);
                        et_phone.setText(phone);
                        et_status.setText(status);

                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                            Bitmap profileImageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            profile_image.setImageBitmap(profileImageBitmap);
                        }
                    } else {
                        Log.d("FirestoreData", "Document không tồn tại.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreData", "Lỗi khi lấy dữ liệu", e);
                });
    }

    private void UpdateUserData(String userId, Bitmap profileBitmap) {
        String username = et_username.getText().toString();
        String birthday = et_birthday.getText().toString();
        String email = et_email.getText().toString();
        String phone = et_phone.getText().toString();
        String status = et_status.getText().toString();

        // Chuyển đổi ảnh đại diện thành chuỗi Base64
        String profileImageBase64 = encodeImageToBase64(profileBitmap);

        // Lấy tham chiếu tới document người dùng cần cập nhật
        DocumentReference userRef = db.collection("users").document(userId);

        // Tạo map để lưu trữ các dữ liệu cần cập nhật
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("birthday", birthday);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("status", status);
        if (profileImageBase64 != null) {
            userData.put("profileImageBase64", profileImageBase64);
        }

        // Cập nhật dữ liệu vào Firestore
        userRef.update(userData)
                .addOnSuccessListener(aVoid -> {
                    // Thông báo cập nhật thành công
                    Toast.makeText(this, "User data updated successfully!", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("userId", userId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Thông báo lỗi khi cập nhật
                    Toast.makeText(this, "Error updating user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream); // Bạn có thể giảm chất lượng nếu muốn
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}