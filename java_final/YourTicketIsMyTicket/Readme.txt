(1) 專案安裝及實行步驟：
Mac 方法一 自行打包：
Step 1：確保使用者有安裝maven和JDK25

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

Mac 方法二 繞過安全憑證：
Step 1：下載github release中的檔案

Step 2：將app拖移到應用程式資料夾

Step 3：再開啟終端機，輸入：
xattr -cr /Applications/YourTicketIsMyTicket.app

Windows:
Step 1：下載github release中的壓縮檔並解壓縮

Step 2：開啟資料夾中的exe檔

(2) demo前後之新增或修改：
1. 跳窗文字增加票的資訊
2. 變更json及database的生成位置

(3) 其他說明事項
1. 第一次開啟瀏覽器時，需等待1到3分鐘
2. 系統將在背景持續運作，如果要完全關閉，請在選單列icon按右鍵開啟選單，並點選「結束程式」