package com.example.milita;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CertificateManagementActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_certificate = 10;
    private RecyclerView certificateRecyclerView;
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList;
    private CheckBox[] checkBoxes;
    private Button btnDelete, btnAdd;
    private CheckBox chkCheckAll;
    private FirebaseFirestore db;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_certificate_management);

        db = FirebaseFirestore.getInstance();
        certificateRecyclerView = findViewById(R.id.recyclerView);
        certificateRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnDelete = findViewById(R.id.btnDelete);
        btnAdd = findViewById(R.id.btnAdd);
        chkCheckAll = findViewById(R.id.chkCheckAll);
        String role = getIntent().getStringExtra("role");
        studentId = getIntent().getStringExtra("studentId");
        certificateList = new ArrayList<>();
        certificateAdapter = new CertificateAdapter(this, certificateList, role, studentId);
        certificateRecyclerView.setAdapter(certificateAdapter);
        //check quyền ng dùng
        if ("Employee".equals(role)) {
            // Ẩn các chức năng mà nhân viên không thể làm
            btnAdd.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            chkCheckAll.setVisibility(View.GONE);
            Toast.makeText(this, "Employee role: View-only access", Toast.LENGTH_SHORT).show();
        }
        // Lấy dữ liệu từ Firestore
        loadCertificateDataFromFirestore(studentId);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CertificateManagementActivity.this, AddCertificateActivity.class);
                intent.putExtra("studentId", studentId);

                startActivityForResult(intent, 6);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        chkCheckAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            certificateAdapter.selectAllItems(isChecked);
        });

    }

    private void loadCertificateDataFromFirestore(String studentId) {
        // Tìm tài liệu sinh viên dựa trên studentId
        db.collection("students")
                .whereEqualTo("id", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Lấy documentId của sinh viên đầu tiên tìm thấy
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Lấy subcollection certificates của tài liệu sinh viên
                        db.collection("students")
                                .document(documentId)
                                .collection("certificates")
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        certificateList.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            String id = document.getId(); // Lấy ID từ Firestore
                                            String name = document.getString("cername");
                                            String session = document.getString("session");
                                            String date = document.getString("dateCreated");
                                            String profileImageBase64 = document.getString("profileImageBase64");

                                            // Chuyển Base64 thành Bitmap
                                            Bitmap profileImageBitmap = null;
                                            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                                profileImageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                            }

                                            // Tạo đối tượng certificate và truyền Bitmap vào nếu có
                                            Certificate certificate = new Certificate(name, session, date, profileImageBitmap);
                                            certificate.setId(id);
                                            certificateList.add(certificate);
                                        }

                                        // Kiểm tra nếu có dữ liệu trước khi cập nhật adapter
                                        if (!certificateList.isEmpty()) {
                                            certificateAdapter.notifyDataSetChanged();
                                        } else {
                                            Toast.makeText(this, "Không có dữ liệu certificate nào.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(this, "Lỗi khi lấy dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Không tìm thấy sinh viên với studentId này.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tìm sinh viên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // Hiển thị dialog xác nhận trước khi xóa
    private void showDeleteConfirmationDialog() {
        // Tạo một AlertDialog để xác nhận hành động xóa
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm delete")
                .setMessage("Are you sure to delete certificates?")
                .setCancelable(false)  // Không cho phép đóng dialog khi nhấn ngoài
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Nếu người dùng xác nhận xóa, gọi phương thức xóa các mục đã chọn và xóa trong Firestore
                        certificateAdapter.removeSelectedItems(db, studentId); // Truyền db vào
                        chkCheckAll.setChecked(false);
                    }
                })
                .setNegativeButton("Hủy", null)  // Nếu người dùng hủy, không làm gì
                .create();

        // Lấy ra các button của dialog
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                // Lấy các button từ dialog
                Button btnDelete = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                Button btnCancel = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);

                btnDelete.setTextColor(getResources().getColor(R.color.blue)); // Màu xanh (thêm màu xanh vào resource)
                btnCancel.setTextColor(getResources().getColor(R.color.red)); // Màu đỏ (thêm màu đỏ vào resource)
            }
        });

        // Hiển thị dialog
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 6 && resultCode == RESULT_OK) {
            loadCertificateDataFromFirestore(studentId);
        }
    }
}