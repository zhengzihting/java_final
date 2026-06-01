/**
 * 通知功能的模擬測試資料。
 * 修改此檔案即可調整測試通知的預設內容，不需動到主程式邏輯。
 */
public class NotificationTestHelper {

    // --- 模擬用預設值 ---
    public static final String DEFAULT_URL   = "http://127.0.0.1:8088/";
    public static final String DEFAULT_AREA  = "特A區";
    public static final String DEFAULT_PRICE = "4800";
    public static final String DEFAULT_DATE  = "2026-06-20";

    /** 從使用者輸入欄位建立模擬通知資料，空白時套用預設值。 */
    public static NotificationPayload build(String url, String area, String price, String date) {
        String resolvedUrl   = (url   != null && url.startsWith("http")) ? url   : DEFAULT_URL;
        String resolvedArea  = (area  != null && !area.trim().isEmpty())  ? area  : DEFAULT_AREA;
        String resolvedPrice = (price != null && !price.trim().isEmpty()) ? price : DEFAULT_PRICE;
        String resolvedDate  = (date  != null && !date.trim().isEmpty())  ? date  : DEFAULT_DATE;

        String details = String.format(
            "[模擬] 偵測到釋票 — 區域：%s，票價：NT$ %s，日期：%s",
            resolvedArea, resolvedPrice, resolvedDate
        );

        return new NotificationPayload(resolvedUrl, details);
    }

    /** 封裝單次通知所需的 URL 與說明文字。 */
    public static class NotificationPayload {
        public final String url;
        public final String details;

        public NotificationPayload(String url, String details) {
            this.url     = url;
            this.details = details;
        }
    }
}
