package vn.edu.vhu.ltdd.a7permission;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // TODO: thay 2201234567 bằng MSSV của bạn
    private static final String TAG = "A7_231A010060";

    private static final String CHANNEL_ID = "a7_channel";
    private static final int NOTI_ID = 1001;

    private ImageView imgAnh;
    private TextView tvTrangThai;

    // ---------------------------------------------------------------
    // 1) Bộ xin quyền CAMERA.
    //    registerForActivityResult phải được gọi TRƯỚC khi Activity ở trạng thái
    //    STARTED, nên ta khai báo ở cấp field (chạy trong constructor).
    // ---------------------------------------------------------------
    private final ActivityResultLauncher<String> xinQuyenCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                Log.d(TAG, "Kết quả xin quyền CAMERA: " + duocCap);
                if (duocCap) {
                    moCamera();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Trạng thái 2: từ chối nhưng còn hỏi lại được
                    hienGiaiThich();
                } else {
                    // Trạng thái 3: đã chọn "Không cho phép" nhiều lần → hệ thống không
                    // hiện hộp thoại nữa, chỉ còn cách vào Cài đặt
                    hienMoCaiDat();
                }
            });

    // 2) Bộ xin quyền POST_NOTIFICATIONS (chỉ cần từ Android 13 / API 33)
    private final ActivityResultLauncher<String> xinQuyenThongBao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    guiThongBao();
                } else {
                    Toast.makeText(this, R.string.noti_denied, Toast.LENGTH_SHORT).show();
                }
            });

    // 3) Bộ mở Camera lấy ảnh xem trước (bitmap thu nhỏ, không cần lưu file)
    private final ActivityResultLauncher<Void> chupAnh = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            (Bitmap bitmap) -> {
                if (bitmap != null) {
                    imgAnh.setImageBitmap(bitmap);
                    tvTrangThai.setText(getString(R.string.status_photo,
                            bitmap.getWidth(), bitmap.getHeight()));
                } else {
                    Toast.makeText(this, R.string.photo_cancelled, Toast.LENGTH_SHORT).show();
                }
            });

    // 4) NC1: Bộ xin nhiều quyền cùng lúc
    private final ActivityResultLauncher<String[]> xinHaiQuyen = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean contactsGranted = result.getOrDefault(Manifest.permission.READ_CONTACTS, false);
                
                boolean isCameraGranted = cameraGranted != null && cameraGranted;
                boolean isContactsGranted = contactsGranted != null && contactsGranted;
                
                String msg = "Kết quả NC1:\nCAMERA: " + (isCameraGranted ? "Đã cấp" : "Từ chối") +
                             "\nREAD_CONTACTS: " + (isContactsGranted ? "Đã cấp" : "Từ chối");
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                capNhatTrangThai();
            });

    // 5) NC2: Bộ xin quyền danh bạ
    private final ActivityResultLauncher<String> xinQuyenDanhBa = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    hienThiDanhBa();
                } else {
                    Toast.makeText(this, "Bạn đã từ chối quyền đọc danh bạ!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        imgAnh = findViewById(R.id.imgAnh);
        tvTrangThai = findViewById(R.id.tvTrangThai);
        Button btnChupAnh = findViewById(R.id.btnChupAnh);
        Button btnThongBao = findViewById(R.id.btnThongBao);
        Button btnXinHaiQuyen = findViewById(R.id.btnXinHaiQuyen);
        Button btnDocDanhBa = findViewById(R.id.btnDocDanhBa);
        Button btnCaiDat = findViewById(R.id.btnCaiDat);

        taoKenhThongBao();

        btnChupAnh.setOnClickListener(v -> kiemTraRoiChup());
        btnThongBao.setOnClickListener(v -> kiemTraRoiGuiThongBao());
        btnXinHaiQuyen.setOnClickListener(v -> xinHaiQuyen.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_CONTACTS
        }));
        btnDocDanhBa.setOnClickListener(v -> kiemTraRoiDocDanhBa());
        btnCaiDat.setOnClickListener(v -> moCaiDatUngDung());
    }

    @Override
    protected void onResume() {
        super.onResume();
        capNhatTrangThai();   // người dùng có thể vừa đổi quyền trong Cài đặt rồi quay lại
    }

    // =====================  LUỒNG XIN QUYỀN CAMERA  =====================

    /** Mẫu chuẩn 3 nhánh mà Google khuyến nghị. */
    private void kiemTraRoiChup() {
        int trangThai = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (trangThai == PackageManager.PERMISSION_GRANTED) {
            // Nhánh 1: đã có quyền → dùng ngay
            moCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Nhánh 2: đã từng từ chối → giải thích lý do trước khi hỏi lại
            hienGiaiThich();
        } else {
            // Nhánh 3: lần đầu (hoặc đã bị chặn vĩnh viễn) → gọi thẳng hộp thoại hệ thống
            xinQuyenCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void hienGiaiThich() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.rationale_title)
                .setMessage(R.string.rationale_camera)
                .setPositiveButton(R.string.agree,
                        (d, w) -> xinQuyenCamera.launch(Manifest.permission.CAMERA))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void hienMoCaiDat() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.blocked_title)
                .setMessage(R.string.blocked_camera)
                .setPositiveButton(R.string.open_settings, (d, w) -> moCaiDatUngDung())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void moCamera() {
        chupAnh.launch(null);
    }

    /** Mở đúng trang thông tin ứng dụng của app này trong Cài đặt. */
    private void moCaiDatUngDung() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }

    // =====================  LUỒNG THÔNG BÁO  =====================

    private void kiemTraRoiGuiThongBao() {
        // Trước Android 13 không có quyền POST_NOTIFICATIONS → gửi được ngay
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            guiThongBao();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            guiThongBao();
        } else {
            xinQuyenThongBao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Từ Android 8 (API 26) mọi thông báo đều phải thuộc về một kênh. */
    private void taoKenhThongBao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kenh = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            kenh.setDescription(getString(R.string.channel_desc));
            getSystemService(NotificationManager.class).createNotificationChannel(kenh);
        }
    }

    private void guiThongBao() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.noti_title))
                .setContentText(getString(R.string.noti_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(this).notify(NOTI_ID, b.build());
        } catch (SecurityException e) {
            // Người dùng vừa tắt quyền ở Cài đặt trong lúc app đang chạy
            Log.w(TAG, "Thiếu quyền gửi thông báo", e);
        }
    }

    // =====================  HIỂN THỊ TRẠNG THÁI  =====================

    private void capNhatTrangThai() {
        boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean thongBao = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        String s = getString(R.string.status_format,
                camera ? getString(R.string.granted) : getString(R.string.denied),
                thongBao ? getString(R.string.granted) : getString(R.string.denied));
        tvTrangThai.setText(s);
        Log.d(TAG, s.replace("\n", " | "));
    }

    // =====================  LUỒNG DANH BẠ (NC2) =====================

    private void kiemTraRoiDocDanhBa() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            hienThiDanhBa();
        } else {
            xinQuyenDanhBa.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void hienThiDanhBa() {
        StringBuilder sb = new StringBuilder("5 Liên hệ đầu tiên:\n");
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                null, null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            int count = 0;
            if (cursor.moveToFirst()) {
                do {
                    int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        String name = cursor.getString(nameIndex);
                        sb.append("- ").append(name).append("\n");
                        count++;
                    }
                } while (cursor.moveToNext() && count < 5);
            }
            if (count == 0) {
                sb.append("(Danh bạ trống)");
            }
            cursor.close();
        } else {
            sb.append("Lỗi khi đọc danh bạ");
        }

        // Hiện lên TextView hoặc Toast
        tvTrangThai.setText(sb.toString());
    }
}
