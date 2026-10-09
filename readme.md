# A7 Permission App (A7_231A010060)

## Các bài tập nâng cao 

Trong dự án có triển khai 2 bài tập nâng cao là NC1 và NC2

### 1. NC1: Xin hai quyền cùng lúc và xử lý từng quyền riêng
- Sử dụng `ActivityResultContracts.RequestMultiplePermissions()` để yêu cầu cấp nhiều quyền cùng một lúc (ở đây là 2 quyền `CAMERA` và `READ_CONTACTS`).
- Xử lý mảng kết quả trả về của từng quyền, sau đó hiển thị thông báo bằng `Toast` cho người dùng và cập nhật lại giao diện trạng thái.

### 2. NC2: Thêm quyền READ_CONTACTS: đọc và hiển thị 5 liên hệ đầu tiên
- Xin quyền đọc danh bạ (`Manifest.permission.READ_CONTACTS`).
- Xử lý logic truy cập vào Content Provider của thiết bị sau khi được cấp quyền.
- Sử dụng `ContentResolver` truy vấn vào `ContactsContract.Contacts.CONTENT_URI` để đọc và lấy ra tên hiển thị của 5 liên hệ đầu tiên (được sắp xếp theo bảng chữ cái alphabet).
- Trình bày kết quả danh sách các liên hệ đọc được lên màn hình.
