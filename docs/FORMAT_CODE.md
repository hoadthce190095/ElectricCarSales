# Quy tắc trình bày code

Mã nguồn được trình bày theo một kiểu thống nhất để dễ đọc, sửa và giải thích khi báo cáo.

## Kiểu đang dùng

- Thụt lề bằng 4 dấu cách.
- Dấu `{` mở cùng dòng với tên hàm hoặc điều kiện.
- Các khối `if`, `else`, `for`, `while` và `do` có đủ `{ }`, kể cả khi chỉ có một câu lệnh.
- Giữ `else if` liền nhau và có dòng trống giữa các hàm.
- `return` nằm ở dòng riêng trong thân hàm; getter/setter cũng xuống dòng như các hàm khác.
- Dòng thông thường dài tối đa 120 ký tự. Chuỗi SQL, URL và nội dung văn bản được giữ nguyên giá trị.
- JavaScript dùng dấu chấm phẩy và dấu nháy kép cho chuỗi.
- JSP thụt lề theo cấp thẻ; CSS xuống dòng từng thuộc tính; SQL xuống dòng các cột và danh sách giá trị dài.

## Chỉnh trong IntelliJ

Mở project như hướng dẫn hiện có. File `.editorconfig` ở thư mục gốc chứa quy tắc thụt lề và trình bày Java. Trong IntelliJ, chọn **Code → Reformat Code** hoặc nhấn **Ctrl + Alt + L** để format file đang sửa.

Getter/setter được trình bày như mẫu:

```java
public List<LookupDTO> getRoles() {
    return roles;
}

public void setRoles(List<LookupDTO> roles) {
    this.roles = roles;
}
```

Nếu IntelliJ hiển thị cả getter trên một dòng dù file đã xuống dòng, kiểm tra **Settings → Editor → General → Code Folding → Java** và bỏ chọn **One-line methods**, **Simple property accessors**. Có thể mở phần đang thu gọn bằng **Code → Folding → Expand All**. Xem [hướng dẫn Code Folding của JetBrains](https://www.jetbrains.com/help/idea/code-folding-settings.html) và [cách tắt thu gọn getter/setter](https://intellij-support.jetbrains.com/hc/en-us/community/posts/206806125-How-to-remove-this-annoying-feature-in-IntelliJ-IDEA).

## Format Java bằng Maven

Chạy tại thư mục gốc với JDK 24:

```powershell
mvn -f ElectricCarSales/pom.xml formatter:format
```

Quy tắc Maven nằm ở `ElectricCarSales/config/java-formatter.xml`, áp dụng cho mã Java chính và mã kiểm thử. Lệnh format được chạy khi cần; build thông thường không tự sửa file.

Kiểm tra file đã đúng kiểu trình bày:

```powershell
mvn -f ElectricCarSales/pom.xml formatter:validate
```

## Format phần web

File `.prettierrc.json` ở thư mục gốc chứa quy tắc cho CSS, JavaScript và JSON. Khi có Node.js, có thể chạy:

```powershell
npx prettier --write "ElectricCarSales/src/main/webapp/**/*.{css,js,json}" "ElectricCarSales/src/test/javascript/*.cjs"
```

JSP dùng formatter JSP của IntelliJ để giữ nguyên page directive, JSTL và EL. Node.js chỉ phục vụ format và kiểm tra JavaScript. Website chạy bằng Java và Tomcat.

## Kiểm tra sau khi sửa

```powershell
mvn -f ElectricCarSales/pom.xml clean verify
node ElectricCarSales/src/test/javascript/frontend-checks.cjs
node ElectricCarSales/src/test/javascript/admin-report-checks.cjs
node ElectricCarSales/src/test/javascript/homepage-checks.cjs
```

Sau lần format này, mã Java và JavaScript đã được đối chiếu cây cú pháp và giá trị chuỗi; SQL và PowerShell đã được đối chiếu các token. Việc thêm `{ }` vào các khối ngắn giữ nguyên câu lệnh bên trong. Bộ kiểm thử của project kiểm tra lại hành vi của ứng dụng.
