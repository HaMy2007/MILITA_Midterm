package com.example.milita;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddUserActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private Uri selectedImageUri;
    private EditText txtUserName, txtBirthday, txtEmail, txtPhone;
    private FirebaseFirestore db;
    private Button btnSave, btnCapture, btnBack;
    private CircleImageView profileImageView;
    private RadioGroup radioGroup;
    private RadioButton rBtnNormal, rBtnLocked;
    private String selectedOption = "", selectedRole = "";
    private Spinner spinnerRole;
    private String selectedDate = "";
    private int selectedYear, selectedMonth, selectedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_user);

        db = FirebaseFirestore.getInstance();
        txtUserName = findViewById(R.id.txtUserName);
        txtBirthday = findViewById(R.id.txtBirthday);
        txtEmail = findViewById(R.id.txtEmail);
        txtPhone = findViewById(R.id.txtPhone);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        profileImageView = findViewById(R.id.profile_image);
        btnCapture = findViewById(R.id.btnCapture);
        radioGroup = findViewById(R.id.radioGroup);
        rBtnNormal = findViewById(R.id.rBtnNormal);
        rBtnLocked = findViewById(R.id.rBtnLocked);

        spinnerRole = findViewById(R.id.spinnerRole);

        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);

        txtBirthday.setOnClickListener(v -> {
            showDateDialog();
        });

        btnSave.setOnClickListener(v -> {
            selectedRole = spinnerRole.getSelectedItem().toString();

            profileImageView.setDrawingCacheEnabled(true); // Bật cache
            profileImageView.buildDrawingCache();
            Bitmap profileBitmap = profileImageView.getDrawingCache();
            saveUserData(profileBitmap);
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnCapture.setOnClickListener(v -> showImagePickerDialog());

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rBtnNormal) {
                selectedOption = "Normal";
            } else if (checkedId == R.id.rBtnLocked) {
                selectedOption = "Locked";
            } else {
                selectedOption = "";
            }
        });
    }

    private void showDateDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = dayOfMonth;
            selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            txtBirthday.setText(selectedDate);
        }, selectedYear, selectedMonth, selectedDay);
        datePickerDialog.show();
    }

    private void saveUserData(Bitmap profileImageBitmap) {
        String username = txtUserName.getText().toString();
        String birthday = txtBirthday.getText().toString();
        String email = txtEmail.getText().toString();
        String phone = txtPhone.getText().toString();
        String status = selectedOption;

        // Chuyển đổi ảnh đại diện thành chuỗi Base64
        String profileImageBase64 = encodeImageToBase64(profileImageBitmap);

        // Tạo đối tượng user
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("birthday", birthday);
        user.put("email", email);
        user.put("phone", phone);
        user.put("status", status);
        user.put("role", selectedRole);
        user.put("profileImageBase64", profileImageBase64); // Lưu chuỗi Base64 của ảnh đại diện

        // Thêm dữ liệu vào Firestore
        db.collection("users").add(user)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "User saved successfully!", Toast.LENGTH_SHORT).show();

                    // Sau khi lưu thành công, quay lại trang Home
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent); // Trả kết quả về Home activity
                    finish(); // Quay lại màn hình Home
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    public String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream); // Bạn có thể giảm chất lượng nếu muốn
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
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
                profileImageView.setImageURI(selectedImageUri);
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    profileImageView.setImageURI(selectedImageUri);
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
}