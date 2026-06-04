import app.ticketData.TicketsInfo;

/**
 * 負責將使用者輸入的關鍵字條件與爬蟲抓到的票務資訊做比對。
 * 純邏輯類別，不依賴爬蟲實作或任何 UI 元件。
 */
public class TicketMatcher {

    private final String area;   // 空字串代表不限區域
    private final int price;     // 0 代表不限票價

    /**
     * @param keyword MainApp 組出的條件字串，格式為 "日期 區域 票價"，
     *                各欄位可為空，空白分隔。例："2026-05-20 特A區 4800"
     */
    public TicketMatcher(String keyword) {
        String parsedArea  = "";
        int    parsedPrice = 0;

        if (keyword != null && !keyword.isBlank()) {
            for (String token : keyword.trim().split("\\s+")) {
                if (token.isEmpty()) continue;

                if (token.matches("\\d+")) {
                    // 純數字 → 票價
                    parsedPrice = Integer.parseInt(token);
                } else {
                    // 其餘文字 → 區域（若有多個非數字 token 則以最後一個為準）
                    parsedArea = token;
                }
            }
        }

        this.area  = parsedArea;
        this.price = parsedPrice;
    }

    /**
     * 判斷指定票務資訊是否符合全部設定的篩選條件。
     * 空條件（date / area 為空字串，price 為 0）會自動跳過對應欄位的比對。
     *
     * @param ticket 爬蟲取得的單筆票務資訊
     * @return 符合所有非空條件且狀態為 SELLING 時回傳 true
     */
    public boolean matches(TicketsInfo ticket) {
        // 1. 必須為販售中狀態
        if (ticket.getTicketStatus() != TicketsInfo.StatusType.SELLING) {
            return false;
        }

        String ticketType = ticket.getTicketType();

        // 2. 區域比對（有設條件才比）
        if (!area.isEmpty() && !ticketType.contains(area)) {
            return false;
        }

        // 3. 票價比對（有設條件才比）
        if (price > 0 && ticket.getTicketPrice() != price) {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Getters（供測試或 log 使用）
    // -------------------------------------------------------------------------

    public String getArea()  { return area;  }
    public int    getPrice() { return price; }

    @Override
    public String toString() {
        return String.format("TicketMatcher{area='%s', price=%d}", area, price);
    }
}
