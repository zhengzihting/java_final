package app.view;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * iOS 風格雙選滑塊按鈕，用於在「預設音效」與「自訂音效」之間切換。
 *
 * <p>點擊後 <strong>不會</strong> 自動更新狀態，而是呼叫透過
 * {@link #setOnToggleClicked(Runnable)} 注入的回調。
 * 呼叫方決定是否真的切換（例如：FileChooser 被取消時保持原狀）。
 * 需要更新狀態時，請呼叫 {@link #setCustom(boolean)}。</p>
 */
public class SoundToggleSwitch extends StackPane {

    private static final double TRACK_W = 180;
    private static final double TRACK_H = 30;
    private static final double THUMB_W = 84;
    private static final double PAD     = 3;

    // thumb 在 StackPane 座標系中的停靠 X（StackPane 中心為 0）
    private final double leftX  = -(TRACK_W / 2 - THUMB_W / 2 - PAD);
    private final double rightX =  (TRACK_W / 2 - THUMB_W / 2 - PAD);

    private boolean customSelected = false;

    private final Rectangle   track;
    private final Label       thumbLabel;
    private final TranslateTransition anim;

    private Runnable clickHandler;
    private Runnable rightClickHandler;

    public SoundToggleSwitch() {

        // ── 軌道（Pill 背景）──
        track = new Rectangle(TRACK_W, TRACK_H);
        track.setArcWidth(TRACK_H);
        track.setArcHeight(TRACK_H);
        track.setFill(Color.web("#e8eaf6"));

        // ── 背景標籤（始終可見）──
        Label defaultBg = makeTrackLabel("預設");
        Label customBg  = makeTrackLabel("自訂");

        HBox bgLabels = new HBox(defaultBg, customBg);
        bgLabels.setAlignment(Pos.CENTER);
        bgLabels.setMouseTransparent(true);

        // ── 滑塊（Thumb）──
        Rectangle thumbRect = new Rectangle(THUMB_W, TRACK_H - PAD * 2);
        thumbRect.setArcWidth(TRACK_H - PAD * 2);
        thumbRect.setArcHeight(TRACK_H - PAD * 2);
        thumbRect.setFill(Color.WHITE);
        thumbRect.setEffect(new DropShadow(5, 0, 1.5, Color.web("#00000033")));

        thumbLabel = new Label("預設");
        thumbLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3f51b5;");
        thumbLabel.setAlignment(Pos.CENTER);
        thumbLabel.setPrefWidth(THUMB_W);
        thumbLabel.setMouseTransparent(true);

        StackPane thumb = new StackPane(thumbRect, thumbLabel);
        thumb.setPrefSize(THUMB_W, TRACK_H - PAD * 2);
        thumb.setMouseTransparent(true);
        thumb.setTranslateX(leftX);

        // ── 動畫 ──
        anim = new TranslateTransition(Duration.millis(180), thumb);

        // ── 組合 ──
        getChildren().addAll(track, bgLabels, thumb);
        setAlignment(Pos.CENTER);
        setPrefSize(TRACK_W, TRACK_H);
        setMaxSize(TRACK_W, TRACK_H);
        setStyle("-fx-cursor: hand;");

        // ── Hover 效果 ──
        setOnMouseEntered(e -> track.setOpacity(0.85));
        setOnMouseExited(e  -> track.setOpacity(1.0));

        // ── 點擊 ──
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (rightClickHandler != null) rightClickHandler.run();
            } else {
                if (clickHandler != null) clickHandler.run();
            }
        });
    }

    // ── 公開 API ────────────────────────────────────────────────────────────

    /**
     * 設定使用者點擊時呼叫的回調。
     * 回調內部決定是否要呼叫 {@link #setCustom(boolean)} 來更新狀態。
     */
    public void setOnToggleClicked(Runnable handler) {
        this.clickHandler = handler;
    }

    /** 設定右鍵點擊時的回調 */
    public void setOnRightClicked(Runnable handler) {
        this.rightClickHandler = handler;
    }

    /** 目前是否選擇「自訂」狀態 */
    public boolean isCustomSelected() {
        return customSelected;
    }

    /**
     * 程式化切換狀態（含滑動動畫）。
     *
     * @param isCustom true → 滑到「自訂」；false → 滑回「預設」
     */
    public void setCustom(boolean isCustom) {
        this.customSelected = isCustom;
        thumbLabel.setText(isCustom ? "自訂" : "預設");
        track.setFill(isCustom ? Color.web("#c5cae9") : Color.web("#e8eaf6"));
        anim.stop();
        anim.setToX(isCustom ? rightX : leftX);
        anim.play();
    }

    // ── 私有工具 ─────────────────────────────────────────────────────────────

    private Label makeTrackLabel(String text) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(TRACK_W / 2);
        lbl.setAlignment(Pos.CENTER);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");
        return lbl;
    }
}
