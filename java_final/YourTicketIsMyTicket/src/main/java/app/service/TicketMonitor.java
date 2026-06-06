package app.service;

import app.model.MonitorEvent;
import app.util.TicketMatcher;
import app.crawler.KktixCrawler;
import app.ticketData.TicketList;
import app.ticketData.TicketsInfo;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * 票務監控核心。
 * 負責管理 KktixCrawler 的生命週期，並以固定間隔輪詢票務狀態。
 * 比對邏輯委由 {@link TicketMatcher} 處理；
 * 結果以 {@link MonitorEvent} 封裝後透過 callback 回報給 UI 層。
 *
 * <p><b>兩階段啟動流程：</b>
 * <ol>
 *   <li>{@link #prepareMonitoring(Runnable)} — 開啟瀏覽器（導向 KKTIX 首頁），
 *       等待使用者手動登入。瀏覽器就緒後呼叫 {@code onReady}。</li>
 *   <li>{@link #beginMonitoring(int)} — 使用者確認登入後呼叫，
 *       將頁面導向目標票務 URL 並啟動定時輪詢。</li>
 * </ol>
 */
public class TicketMonitor {

    /** KKTIX 登入頁：Phase 1 開啟瀏覽器時先導向此頁，讓使用者登入。 */
    private static final String KKTIX_HOME = "https://kktix.com/users/sign_in";

    private final String url;
    private final String keyword;
    private final Consumer<MonitorEvent> callback;

    private Timer timer;
    private KktixCrawler crawler;

    /** 防止同一輪 TimerTask 重疊執行（crawlTickets 內有 sleep，可能超過 interval）*/
    private volatile boolean isCrawling = false;
    /** 防止瀏覽器關閉後重複呼叫 stopMonitoring */
    private volatile boolean isStopped  = false;

    private final Random random = new Random();

    public TicketMonitor(String url, String keyword, Consumer<MonitorEvent> callback) {
        this.url      = url;
        this.keyword  = keyword;
        this.callback = callback;
    }

    // -------------------------------------------------------------------------
    // Phase 1：開啟瀏覽器，等待使用者登入
    // -------------------------------------------------------------------------

    /**
     * Phase 1：在背景開啟 Chrome 並導向 KKTIX 首頁，讓使用者可手動登入。
     * 瀏覽器成功連線後呼叫 {@code onReady}（在 JavaFX Application Thread 執行）。
     *
     * <p>若瀏覽器啟動失敗，會透過 callback 回報錯誤訊息，且不呼叫 {@code onReady}。
     *
     * @param onReady 瀏覽器就緒後的回呼，通常用於切換 UI 狀態為「等待登入確認」
     */
    public void prepareMonitoring(Runnable onReady) {
        isStopped = false;

        Thread initThread = new Thread(() -> {
            try {
                crawler = new KktixCrawler(KKTIX_HOME);
                crawler.startCrawler();

                Platform.runLater(() -> {
                    callback.accept(MonitorEvent.log(
                        "瀏覽器已開啟，請完成登入。"));
                    onReady.run();
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                    callback.accept(MonitorEvent.log("瀏覽器啟動失敗：" + e.getMessage()))
                );
            }
        }, "crawler-init-thread");

        initThread.setDaemon(true);
        initThread.start();
    }

    // -------------------------------------------------------------------------
    // Phase 2：使用者確認登入後，navigate 並開始輪詢
    // -------------------------------------------------------------------------

    /**
     * Phase 2：將瀏覽器導向目標票務 URL，並啟動定時輪詢。
     * 必須在 {@link #prepareMonitoring(Runnable)} 的 {@code onReady} 回呼之後呼叫。
     *
     * @param intervalSeconds 每次輪詢的基準間隔秒數（實際會加入隨機浮動）
     */
    public void beginMonitoring(int intervalSeconds) {
        if (isStopped || crawler == null) return;

        TicketMatcher matcher = new TicketMatcher(keyword);

        Thread navigateThread = new Thread(() -> {
            try {
                // 將頁面導向真正的票務 URL
                crawler.getBuyTicketPage().navigate(url);

                Platform.runLater(() ->
                    callback.accept(MonitorEvent.log(
                        "開始監控目標：" + url + "　條件：" + matcher))
                );

                // 頁面就緒後啟動定時器，首輪立即執行
                timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        crawlAndCheck(matcher);
                        scheduleNextCrawl(matcher, intervalSeconds);
                    }
                }, 0L);

            } catch (Exception e) {
                Platform.runLater(() ->
                    callback.accept(MonitorEvent.log("導向票務頁面失敗：" + e.getMessage()))
                );
            }
        }, "crawler-navigate-thread");

        navigateThread.setDaemon(true);
        navigateThread.start();
    }

    // -------------------------------------------------------------------------
    // 停止監控
    // -------------------------------------------------------------------------

    /**
     * 停止監控，並關閉爬蟲所開啟的瀏覽器。
     * 可在 Phase 1（等待登入）或 Phase 2（輪詢中）任意時機呼叫。
     */
    public void stopMonitoring() {
        if (isStopped) return;
        isStopped = true;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (crawler != null) {
            final KktixCrawler crawlerRef = crawler;
            crawler = null;
            // 在背景等待目前這輪爬取結束後再關閉，避免強制中斷 Playwright
            Thread closeThread = new Thread(() -> {
                while (isCrawling) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
                try {
                    crawlerRef.closeCrawler();
                } catch (Exception e) {
                    System.err.println("closeCrawler error：" + e);
                }
            }, "crawler-close-thread");
            closeThread.setDaemon(true);
            closeThread.start();
        }
    }

    // -------------------------------------------------------------------------
    // 內部：定時輪詢邏輯
    // -------------------------------------------------------------------------

    /**
     * 以隨機延遲排定下一輪爬取。
     * 每輪結束後才計算下次延遲，間隔不固定，模擬人工重整頁面的節奏。
     */
    private void scheduleNextCrawl(TicketMatcher matcher, int baseSeconds) {
        if (isStopped || timer == null) return;
        long delayMs = nextDelayMs(baseSeconds);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                crawlAndCheck(matcher);
                scheduleNextCrawl(matcher, baseSeconds); // 本輪結束後才排下一輪
            }
        }, delayMs);
    }

    /**
     * 計算下一輪的隨機延遲（毫秒）。
     * 範圍為基準秒數的 60 %～140 %，讓間隔自然浮動。
     */
    private long nextDelayMs(int baseSeconds) {
        double factor = 0.6 + random.nextDouble() * 0.8; // [0.6, 1.4)
        return (long) (baseSeconds * 1000 * factor);
    }

    /**
     * 刷新頁面並抓取票務資訊，將每筆票與 matcher 比對後透過 callback 回報結果。
     * 若上一輪尚未完成則直接跳過，避免並行執行。
     */
    private void crawlAndCheck(TicketMatcher matcher) {
        if (isCrawling || isStopped) return;
        isCrawling = true;

        try {
            if (isStopped) return;          // 進入後再確認一次
            crawler.refreshPage();
            if (isStopped) return;          // refreshPage 結束後確認
            TicketList ticketList = crawler.crawlTickets();
            if (isStopped) return;          // crawlTickets 結束後確認

            if (!ticketList.haveTicket()) {
                Platform.runLater(() ->
                    callback.accept(MonitorEvent.log("暫時無法取得票務資訊，繼續等待..."))
                );
                return;
            }

            List<TicketsInfo> tickets = ticketList.getTicketsInfo();
            boolean found = false;

            for (TicketsInfo ticket : tickets) {
                final String type   = ticket.getTicketType();
                final String price  = ticket.getTicketPrice();
                final TicketsInfo.StatusType status = ticket.getTicketStatus();

                if (matcher.matches(ticket)) {
                    Platform.runLater(() ->
                        callback.accept(MonitorEvent.ticketFound(
                            "找到符合條件的釋票：" + type + "　NT$" + price))
                    );
                    found = true;
                    break;
                }

                // 若此票在關鍵字條件（區域 / 票價）上符合，但狀態為已售完，則特別提示
                if (status == TicketsInfo.StatusType.SOLD_OUT && matchesIgnoreStatus(matcher, ticket)) {
                    Platform.runLater(() ->
                        callback.accept(MonitorEvent.log(
                            "已售完：" + type + "　NT$" + price))
                    );
                }
            }

            if (!found) {
                Platform.runLater(() ->
                    callback.accept(MonitorEvent.log("未找到符合條件的票"))
                );
            }

        } catch (NullPointerException e) {
            Platform.runLater(() ->
                callback.accept(MonitorEvent.log("頁面解析異常"))
            );
        } catch (Exception e) {
            if (isBrowserClosed(e)) {
                Platform.runLater(() -> {
                    callback.accept(MonitorEvent.log("瀏覽器已關閉，停止監控。"));
                    stopMonitoring();
                });
            } else {
                Platform.runLater(() ->
                    callback.accept(MonitorEvent.log("監控發生錯誤：" + e.getMessage()))
                );
            }
        } finally {
            isCrawling = false;
        }
    }

    /**
     * 判斷異常是否為「瀏覽器/頁面已被關閉」。
     */
    private boolean isBrowserClosed(Exception e) {
        String cls = e.getClass().getName().toLowerCase();
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return cls.contains("playwright") && (
            msg.contains("closed") ||
            msg.contains("disconnect") ||
            msg.contains("crash") ||
            msg.contains("target")
        );
    }

    /**
     * 判斷票券是否在「區域 / 票價」條件上符合，但忽略售出狀態。
     */
    private boolean matchesIgnoreStatus(TicketMatcher matcher, TicketsInfo ticket) {
        String ticketType = ticket.getTicketType();

        if (!matcher.getArea().isEmpty() && !ticketType.contains(matcher.getArea())) {
            return false;
        }

        if (matcher.getPrice() > 0 && ticket.getTicketPrice() != matcher.getPrice()) {
            return false;
        }

        return true;
    }
}