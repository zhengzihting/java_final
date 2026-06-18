(1) 專案安裝及實行步驟：
Step 1：確保使用者有安裝maven和JDK25，且作業系統為mac或windows

Step 2：下載github的程式碼

Step 3：開啟終端機，在YourTicketIsMyTicket的資料夾目錄下輸入
mvn compile clean package

Step 4：
如果os是mac，再輸入：
jpackage --type dmg \
        --name "YourTicketIsMyTicket" \
        --input target \
        --dest .. \
        --main-class app.main.Launcher \
        --main-jar YourTicketIsMyTicket-1.0-SNAPSHOT.jar
windows，則輸入：
jpackage --type exe `
        --name "YourTicketIsMyTicket" `
        --input target `
        --dest .. `
        --main-class app.main.Launcher `
        --main-jar YourTicketIsMyTicket-1.0-SNAPSHOT.jar `
        --win-shortcut

(2) demo前後之新增或修改：
1. 跳窗文字增加票的資訊
2. 變更json及database的生成位置

(3) 其他說明事項
1. 第一次開啟瀏覽器時，需等待1到3分鐘