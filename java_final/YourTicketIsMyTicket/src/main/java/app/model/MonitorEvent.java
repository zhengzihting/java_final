package app.model;

import app.service.TicketMonitor;
/**
 * 監控事件，由 TicketMonitor 透過 callback 傳回。
 * 以結構化欄位取代魔法字串比對，讓呼叫端可直接讀取 ticketFound
 * 而無需自行解析訊息內容。
 */
public class MonitorEvent {

    /** 給使用者看的日誌訊息（原始文字）。 */
    public final String message;

    /** 若監控到符合條件的票券則為 true，否則為 false。 */
    public final boolean ticketFound;

    public MonitorEvent(String message, boolean ticketFound) {
        this.message = message;
        this.ticketFound = ticketFound;
    }

    // ---- 靜態工廠方法，方便建立常用事件 ----

    /** 建立一般日誌事件（未找到票）。 */
    public static MonitorEvent log(String message) {
        return new MonitorEvent(message, false);
    }

    /** 建立「找到票」事件。 */
    public static MonitorEvent ticketFound(String message) {
        return new MonitorEvent(message, true);
    }
}
