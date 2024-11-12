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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
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
        mAuth = FirebaseAuth.getInstance();
        txtUserName = findViewById(R.id.txtUserName);
        txtPassword = findViewById(R.id.txtPassword);
        btnHidenPass = findViewById(R.id.btnHidenPass);

        btnLogin.setOnClickListener(v -> {
            String username = txtUserName.getText().toString().trim();
            String password = txtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your username and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(username, password);
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

//    private void loginUser(String email, String password) {
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//                        // Đăng nhập thành công, lấy thông tin người dùng
//                        FirebaseUser user = mAuth.getCurrentUser();
//                        Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
//
//                        // Điều hướng tới trang chính hoặc một màn hình khác
//                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
//                        finish();
//                    } else {
//                        // Thông báo lỗi nếu đăng nhập thất bại
//                        Toast.makeText(MainActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

//    login phan quyen
    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userId = user.getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // Lấy vai trò người dùng từ Firestore
                        db.collection("users").document(userId).get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        DocumentSnapshot document = task1.getResult();
                                        if (document.exists()) {
                                            String role = document.getString("role");

                                            // Lưu vai trò người dùng và chuyển hướng
                                            if ("Admin".equals(role)) {
                                                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                            } else if ("Manager".equals(role)) {
                                                Intent intent = new Intent(MainActivity.this, StudentManagementActivity.class);
                                                intent.putExtra("role", role);
                                                startActivity(intent);
                                            } else if ("Employee".equals(role)) {
                                                Intent intent = new Intent(MainActivity.this, StudentManagementActivity.class);
                                                intent.putExtra("role", role);
                                                startActivity(intent);
                                            }
                                            finish();
                                        } else {
                                            Toast.makeText(MainActivity.this, "ko login dc", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "Error getting user role", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public void onStart() {
        super.onStart();
        // Kiểm tra xem người dùng đã đăng nhập chưa, nếu có thì chuyển thẳng vào trang chính
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(MainActivity.this, StudentManagementActivity.class));
            finish();
        }
    }
}