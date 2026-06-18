(1) 專案安裝及實行步驟：
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
windows，則輸入：
jpackage --type exe `
        --name "YourTicketIsMyTicket" `
        --input target `
        --dest .. `
        --main-class app.main.Launcher `
        --main-jar YourTicketIsMyTicket-1.0-SNAPSHOT.jar `
        --win-shortcut