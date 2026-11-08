---
title: Chính sách quyền riêng tư
permalink: /privacy-vi/
---

Tiếng Việt · [English](privacy.md)

# Chính sách quyền riêng tư: Sổ Xe

Cập nhật lần cuối: 2026-11-08.

Sổ Xe là cuốn sổ ghi chép cho chính xe của bạn. Trang này nêu app lưu những gì, lưu ở đâu và xin những
quyền nào. Mỗi khi một tính năng làm thay đổi điều đó, trang này được cập nhật trong cùng thay đổi.

## Dữ liệu app lưu

| Dữ liệu | Lưu ở đâu | Chia sẻ với |
| --- | --- | --- |
| Xe, các lần đổ xăng, hạng mục và lần bảo dưỡng bạn nhập | Trên điện thoại của bạn, trong cơ sở dữ liệu của app | Không ai |
| Cài đặt (xe đang chọn, bật hoặc tắt nhắc lịch) | Trên điện thoại của bạn | Không ai |
| Báo cáo lỗi (bản phát hành): nội dung lỗi, phiên bản app, phiên bản Android và mẫu máy | Gửi đến Firebase Crashlytics (Google) | Google, với vai trò bên xử lý dữ liệu |
| Bốn sự kiện sử dụng: đã thêm một xe, đã ghi một lần đổ xăng, đã đánh dấu một lần bảo dưỡng, đã mở tab Gần tôi. Chỉ có tên sự kiện, không kèm giá trị nào | Gửi đến Firebase Analytics (Google), cùng với dữ liệu ẩn danh về bản cài đặt app mà Firebase tự thêm | Google, với vai trò bên xử lý dữ liệu |
| Vị trí gần đúng, chỉ khi màn hình Gần tôi đang mở | Gửi mỗi lần tìm kiếm đến Overpass API (overpass-api.de, một dịch vụ công cộng của OpenStreetMap) để tìm địa điểm trong 3 km. Giữ trong bộ nhớ 30 phút để khỏi tìm lại, không bao giờ ghi vào ổ lưu trữ | Dịch vụ Overpass thấy toạ độ và địa chỉ IP của bạn, như mọi yêu cầu web khác |

Tính năng sao lưu tự động của Android có thể đưa dữ liệu này vào bản sao lưu điện thoại trên tài khoản
Google của bạn, như mọi app cho phép sao lưu. Bạn có thể tắt sao lưu trong cài đặt của điện thoại.

App không có tài khoản và không có quảng cáo. Báo cáo lỗi và bốn sự kiện ẩn danh được gửi đến Firebase
ở bản phát hành; chúng không chứa dữ liệu bạn nhập.

## Quyền app xin

| Quyền | Để làm gì | Khi nào xin |
| --- | --- | --- |
| Vị trí (gần đúng hoặc chính xác) | Liệt kê trạm xăng và garage gần bạn | Khi bạn bấm "Cho phép vị trí" ở màn hình Gần tôi |
| Thông báo (Android 13 trở lên) | Nhắc bạn khi hạng mục bảo dưỡng sắp đến hạn hoặc quá hạn | Chỉ khi bạn bấm "Cho phép" ở tab Bảo dưỡng |

Bạn có thể thu hồi quyền trong cài đặt hệ thống, hoặc tắt nhắc lịch ngay trong app.

## Ghi chú cho mục An toàn dữ liệu (Google Play)

- Dữ liệu thu thập: nhật ký lỗi và thông tin chẩn đoán (Crashlytics); bốn sự kiện sử dụng và mã nhận
  dạng bản cài đặt app (Analytics); vị trí gần đúng dùng cho tính năng Gần tôi, được xử lý trên máy và
  gửi đến dịch vụ Overpass công cộng để tìm kiếm. App không lưu nó trên máy chủ nào. Không gắn với danh
  tính: không có tài khoản.
- Dữ liệu chia sẻ: toạ độ của một lần tìm kiếm, gửi cho dịch vụ Overpass để trả lời lần tìm đó.
- Mã hoá khi truyền: có, qua HTTPS.
- Xoá dữ liệu: xoá một xe sẽ xoá dữ liệu của xe đó; gỡ app sẽ xoá toàn bộ dữ liệu trên máy.

## Liên hệ

Câu hỏi, hoặc yêu cầu được biết có gì đang được lưu về bạn: trangsae.fw@gmail.com. Vì app không có tài
khoản và không gửi đi thông tin nào xác định được bạn, những gì có thể tra cứu chỉ là dữ liệu nằm trên
chính điện thoại của bạn, và bạn xem và xoá được tất cả ngay trong app. Bạn cũng có thể mở một issue tại
https://github.com/diiafyra/motoman-android/issues.

## Thay đổi

Nếu một phiên bản mới thay đổi dữ liệu app thu thập hoặc các quyền app xin, trang này được cập nhật
trong cùng bản phát hành, và ngày "Cập nhật lần cuối" ở trên cũng thay đổi.
