package com.example.milita;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private Button btnLogin;
    private EditText txtUserName, txtPassword;
    private boolean isPasswordVisible = false;
    private ImageButton btnHidenPass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        db = FirebaseFirestore.getInstance();
        txtUserName = findViewById(R.id.txtUserName);
        txtPassword = findViewById(R.id.txtPassword);
        btnHidenPass = findViewById(R.id.btnHidenPass);

        btnLogin.setOnClickListener(v -> {
            String username = txtUserName.getText().toString().trim();
            String password = txtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your username and password", Toast.LENGTH_SHORT).show();
            } else {
                checkStatusAccount(username, isBlocked -> {
                    if (isBlocked) {
                        Toast.makeText(MainActivity.this, "Your account is blocked, please contact to admin open it!", Toast.LENGTH_SHORT).show();
                    } else {
                        loginUser(username, password); // Gọi phương thức đăng nhập khi tài khoản không bị khóa
                    }
                });
            }
        });


        btnHidenPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Nếu mật khẩu đang hiển thị, ẩn mật khẩu bằng PasswordTransformationMethod
                    txtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    btnHidenPass.setBackgroundResource(R.drawable.hiden_pass); // Đổi hình ảnh về mắt đóng
                    isPasswordVisible = false;
                } else {
                    // Nếu mật khẩu đang ẩn, hiển thị mật khẩu
                    txtPassword.setTransformationMethod(null); // Hiển thị mật khẩu
                    btnHidenPass.setBackgroundResource(R.drawable.show_pass); // Đổi hình ảnh về mắt mở
                    isPasswordVisible = true;
                }

                // Đảm bảo con trỏ ở cuối cùng của mật khẩu sau khi thay đổi transformationMethod
                txtPassword.setSelection(txtPassword.getText().length());
            }
        });
    }

    private void checkStatusAccount(String userEmail, final OnStatusCheckedListener listener) {
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isBlocked = false;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String status = documentSnapshot.getString("status");
                        if ("Locked".equals(status)) {
                            isBlocked = true;
                        }
                    }
                    listener.onStatusChecked(isBlocked); // Trả kết quả cho callback
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error check status", Toast.LENGTH_SHORT).show();
                    listener.onStatusChecked(false); // Xử lý lỗi, mặc định cho phép đăng nhập
                });
    }

    // Interface callback
    public interface OnStatusCheckedListener {
        void onStatusChecked(boolean isBlocked);
    }



    private void loginUser(String email, String password) {
        // Tìm tài khoản trong bảng accounts bằng email (email là thuộc tính, không phải documentId)
        db.collection("accounts")
                .whereEqualTo("email", email)  // Truy vấn tài liệu có thuộc tính "email" trùng khớp
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            // Lấy tài liệu đầu tiên (nếu có nhiều tài liệu, bạn cần xử lý trường hợp này)
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            String storedPassword = document.getString("password");
                            String role = document.getString("role");

                            if (storedPassword != null && storedPassword.equals(password)) {  // Kiểm tra mật khẩu
                                // Đăng nhập thành công
                                Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                                // Điều hướng dựa trên vai trò
                                if ("Admin".equals(role)) {
                                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                } else if ("Manager".equals(role) || "Employee".equals(role)) {
                                    Intent intent = new Intent(MainActivity.this, StudentManagementActivity.class);
                                    intent.putExtra("role", role);
                                    intent.putExtra("currentUserEmail", email);
                                    startActivity(intent);
                                }
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Account not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Error accessing account data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}