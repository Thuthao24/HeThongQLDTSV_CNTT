# Module 15: thông báo chỉ trên website

Bản cập nhật PR #1 sau đợt 17C xóa toàn bộ mã gửi/giám sát email thông báo thường,
không giữ lớp no-op hoặc worker disabled. CRUD, đối tượng nhận, chuông và trạng
thái đọc/chưa đọc giữ nguyên. Các URL `/morong/email-thongbao` và `/quanly`, API
retry không còn handler; rule denyAll `/morong/**` chặn truy cập.

Các thay đổi bắt buộc gồm cả lớp/profile/script đã tồn tại từ base của PR;
chỉ xóa ba service được sửa trong PR cũ sẽ không đủ để dọn chức năng.

## Giữ nguyên và không mang vào PR

Không đưa thay đổi thống kê 17B, tìm kiếm 17A, dashboard hoặc nghiệp vụ khác.
Email kết quả và các test tương ứng được giữ nguyên theo phiên bản trên branch.
Branch hiện chưa có bản chuyển Mailjet của workspace local: không nhập phần
Module 16 vào PR Module 15 này. Thu Thảo tích hợp Mailjet bằng hạng mục riêng;
cleanup này không sửa transport hoặc cấu hình SMTP dùng chung cho kết quả.
Không gửi email thật khi kiểm thử; H2 và fake sender được dùng cho hồi quy.

## CSDL: hạng mục riêng của Thu Thảo

Giữ nguyên V003, V004, V005. Không đưa V006 chưa duyệt vào PR này. Không DROP,
đổi trạng thái hay xóa lịch sử email. Ứng dụng mới không tạo/sử dụng emailthongbao.
Fresh JPA/H2 không sinh bảng; schema MySQL đã có có thể giữ lại lịch sử cho tới
khi DBA xác minh và phê duyệt backup/dọn riêng. V004/V005 vẫn cần kế hoạch triển
khai riêng đã duyệt để phục vụ đối tượng nhận và read/unread của Module 15.

Dự án không có Flyway/Liquibase hoặc startup runner thực thi thư mục
`database/migrations/pending`; SQL tại đó là đề xuất duyệt thủ công. Cấu hình
JPA mặc định `ddl-auto=none`. Compose chỉ mount schema gốc cho lần tạo volume
MySQL mới, không mount thư mục pending. Không chạy compose/init/reset để test PR.

## Kiểm tra tích hợp

Chạy `mvn clean test` với JDK 26 trên H2; clean là cần thiết để xóa class/resource
cũ. Kiểm tra `NoticeEmailRemovalTests` và toàn bộ test Module 15/16. Thông báo
mới không gọi sender và không tạo email kết quả nhầm. Test có bảng lịch sử H2
xác nhận các dòng cũ không bị ứng dụng xử lý. Không dùng MySQL thật.
