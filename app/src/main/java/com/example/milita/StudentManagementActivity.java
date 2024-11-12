package com.example.milita;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentManagementActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_STUDENT = 10;
    private RecyclerView studentRecyclerView;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private CheckBox[] checkBoxes;
    private Button btnDelete, btnAdd, btnFilter, btnInputList;
    private CheckBox chkCheckAll;
    private FirebaseFirestore db;
    private EditText search_input;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_management);

        db = FirebaseFirestore.getInstance();
        studentRecyclerView = findViewById(R.id.recyclerView);
        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnDelete = findViewById(R.id.btnDelete);
        btnAdd = findViewById(R.id.btnAdd);
        btnInputList = findViewById(R.id.btnInputList);
        chkCheckAll = findViewById(R.id.chkCheckAll);
        btnFilter = findViewById(R.id.btnFilter);
        search_input = findViewById(R.id.search_input);
        String role = getIntent().getStringExtra("role");
        studentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(this, studentList, role);
        studentRecyclerView.setAdapter(studentAdapter);
        //check quyền ng dùng
        if ("Employee".equals(role)) {
            // Ẩn các chức năng mà nhân viên không thể làm
            btnAdd.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            chkCheckAll.setVisibility(View.GONE);
            Toast.makeText(this, "Employee role: View-only access", Toast.LENGTH_SHORT).show();
        }
        // Lấy dữ liệu từ Firestore
        loadStudentDataFromFirestore();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentManagementActivity.this, AddStudentActivity.class);
                startActivityForResult(intent, 3);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        chkCheckAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            studentAdapter.selectAllItems(isChecked);
        });

        btnFilter.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(this, btnFilter);
            popupMenu.getMenuInflater().inflate(R.menu.filter_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.filter_name_asc) {
                    sortStudentListByName(true);
                } else if (id == R.id.filter_name_desc) {
                    sortStudentListByName(false);
                } else if (id == R.id.filter_id_asc) {
                    sortStudentListById(true);
                } else if (id == R.id.filter_id_desc) {
                    sortStudentListById(false);
                }
                return true;
            });

            popupMenu.show();
        });

        search_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase();
                filterStudentList(query);
            }
        });

        btnInputList.setOnClickListener(view -> {
            // Mở trình chọn tệp để người dùng chọn tệp danh sách học viên
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/*"); // Chọn loại tệp văn bản
            startActivityForResult(intent, 1);
        });

    }

    private void loadStudentDataFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String studentId = document.getString("id"); // Lấy ID từ Firestore
                            String name = document.getString("name");
                            String studentClass = document.getString("class");
                            String profileImageBase64 = document.getString("profileImageBase64");

                            // Chuyển Base64 thành Bitmap
                            Bitmap profileImageBitmap = null;
                            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                profileImageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            }

                            // Tạo đối tượng student và truyền Bitmap vào nếu có
                            Student student = new Student(studentId, name, studentClass,  profileImageBitmap);
                            studentList.add(student);
                        }

                        // Kiểm tra nếu có dữ liệu trước khi cập nhật adapter
                        if (!studentList.isEmpty()) {
                            studentAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "No student data found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    // Hiển thị dialog xác nhận trước khi xóa
    private void showDeleteConfirmationDialog() {
        // Tạo một AlertDialog để xác nhận hành động xóa
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm delete")
                .setMessage("Are you sure to delete students?")
                .setCancelable(false)  // Không cho phép đóng dialog khi nhấn ngoài
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Nếu người dùng xác nhận xóa, gọi phương thức xóa các mục đã chọn và xóa trong Firestore
                        studentAdapter.removeSelectedItems(db); // Truyền db vào
                        Toast.makeText(StudentManagementActivity.this, "Các mục đã được xóa.", Toast.LENGTH_SHORT).show();
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

    private void sortStudentListByName(boolean ascending) {
        if (ascending) {
            Collections.sort(studentList, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));
        } else {
            Collections.sort(studentList, (s1, s2) -> s2.getName().compareToIgnoreCase(s1.getName()));
        }
        studentAdapter.notifyDataSetChanged();
    }

    private void sortStudentListById(boolean ascending) {
        if (ascending) {
            Collections.sort(studentList, Comparator.comparingInt(s -> Integer.parseInt(s.getId())));
        } else {
            Collections.sort(studentList, (s1, s2) -> Integer.parseInt(s2.getId()) - Integer.parseInt(s1.getId()));
        }
        studentAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == RESULT_OK) {
            loadStudentDataFromFirestore();
        }
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    importStudentListFromFile(fileUri);
                }
            }
        }
    }

    private void filterStudentList(String query) {
        List<Student> filteredList = new ArrayList<>();

        for (Student student : studentList) {
            if (student.getId().toLowerCase().contains(query) ||
                    student.getName().toLowerCase().contains(query) ||
                    student.getStudentClass().toLowerCase().contains(query)) {
                filteredList.add(student);
            }
        }

        studentAdapter.updateList(filteredList);
    }

    private void importStudentListFromFile(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            List<Student> studentsToAdd = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(","); // Tách các trường dữ liệu dựa trên dấu phẩy
                if (data.length >= 3) {
                    String studentId = data[0].trim();
                    String name = data[1].trim();
                    String studentClass = data[2].trim();
                    String profileImageBase64 = data.length > 3 ? data[3].trim() : "";
                    Bitmap profileImageBitmap = null;
                    if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                        byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                        profileImageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    }
                    // Tạo đối tượng Student
                    Student student = new Student(studentId, name, studentClass, profileImageBitmap);
                    studentsToAdd.add(student);
                }
            }

            reader.close();

            // Sau khi đọc xong tất cả các học viên, gọi phương thức để lưu vào Firestore
            addStudentsToFirestore(studentsToAdd);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi đọc tệp", Toast.LENGTH_SHORT).show();
        }
    }

    private void addStudentsToFirestore(List<Student> students) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Student student : students) {
            // Tạo dữ liệu học viên dưới dạng Map để lưu vào Firestore
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("id", student.getId());
            studentData.put("name", student.getName());
            studentData.put("class", student.getStudentClass());
            studentData.put("profileImageBase64", student.getProfileImage());

            // Thêm học viên vào bộ sưu tập "students"
            db.collection("students")
                    .add(studentData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Học viên đã được thêm vào Firestore", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi thêm học viên vào Firestore", Toast.LENGTH_SHORT).show();
                    });
        }
    }




}