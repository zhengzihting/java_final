package app.util;

import app.main.MainApp;
import app.ticketData.TicketsInfo;

/**
 * 負責將使用者輸入的關鍵字條件與爬蟲抓到的票務資訊做比對。
 * 純邏輯類別，不依賴爬蟲實作或任何 UI 元件。
 */
public class TicketMatcher {

    private final String area;   // 空字串代表不限區域
    private final String price;  // 空字串代表不限票價

    /**
     * @param keyword MainApp 組出的條件字串，格式為 "區域 票價"
     */
    public TicketMatcher(String keyword) {
        String parsedArea  = "";
        String parsedPrice = "";

        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            int lastSpaceIdx = trimmed.lastIndexOf(" ");
            
            if (lastSpaceIdx - 1 >= 0) {
                String pricePart = trimmed.substring(lastSpaceIdx + 1);
                String areaPart  = trimmed.substring(0, lastSpaceIdx);
                
                if (pricePart.matches("\\d+")) {
                    parsedPrice = pricePart;
                    parsedArea  = areaPart.trim();
                } else {
                    parsedArea  = trimmed;
                }
            } else {
                // 如果中間完全沒有空格
                if (trimmed.matches("\\d+")) {
                    parsedPrice = trimmed;
                } else {
                    parsedArea  = trimmed;
                }
            }
        }

        //核心修正防禦點 1：將區域內的空白壓縮、並轉為純小寫，確保對接時萬無一失
        this.area  = parsedArea.replaceAll("\\s+", "").toLowerCase();
        this.price = parsedPrice;
    }

    /**
     * 判斷指定票務資訊是否符合全部設定的篩選條件。
     */
    public boolean matches(TicketsInfo ticket) {
        // 1. 必須為販售中狀態
        if (ticket.getTicketStatus() != TicketsInfo.StatusType.SELLING) {
            return false;
        }

        //核心修正防禦點 2：同樣將網頁抓到的類型去除所有空格並轉小寫，進行模糊 contain 比對！
        String ticketType = ticket.getTicketType() != null ? ticket.getTicketType().replaceAll("\\s+", "").toLowerCase() : "";

        // 2. 區域比對（去空格、不分大小寫，齒輪完美咬合！）
        if (!area.isEmpty() && !ticketType.contains(area)) {
            return false;
        }

        // 3. 票價比對（有設條件才比）
        String crawlerPrice = ticket.getTicketPrice() != null ? ticket.getTicketPrice().replaceAll("[^0-9.]", "") : "";
        return price.isEmpty() || price.equals(crawlerPrice);
    }

    // -------------------------------------------------------------------------
    // Getters（供測試或 log 使用）
    // -------------------------------------------------------------------------

    public String getArea()  { return area;  }
    public String getPrice() { return price; }

    @Override
    public String toString() {
        return String.format("TicketMatcher{area='%s', price='%s'}", area, price);
    }
}
