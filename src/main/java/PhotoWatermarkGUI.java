import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class PhotoWatermarkGUI extends JFrame {
    private static final int THUMB_WIDTH = 80;
    private static final int THUMB_HEIGHT = 80;
    private static final int WATERMARK_TAB_PANEL_MIN_HEIGHT = 280;

    // Force a component tree to opaque white
    private void forceAllWhite(Component c) {
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            jc.setOpaque(true);
            jc.setBackground(Color.WHITE);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                forceAllWhite(child);
            }
        }
    }

    private DefaultListModel<ImageIcon> imageListModel;
    private JList<ImageIcon> imageList;
    private JTextField outputFolderField;
    private File outputFolder;
    private List<File> importedFiles = new ArrayList<>();
    private JComboBox<String> formatComboBox;
    private JComboBox<String> namingRuleComboBox;
    private JTextField prefixField;
    private JTextField suffixField;
    private JLabel prefixLabel;
    private JLabel suffixLabel;
    private JSlider jpegQualitySlider;
    private JTextField widthField;
    private JTextField heightField;
    private JSlider scaleSlider; // Changed from JTextField
    private JLabel selectedImageSizeLabel;
    private JLabel widthLabel;
    private JLabel heightLabel;
    private JComboBox<String> positionComboBox;
    private JTextField fontSizeField;
    private JPanel advancedPanel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JTextField watermarkTextField;
    private JComboBox<String> fontComboBox;
    private JSlider opacitySlider;
    private JCheckBox shadowCheckBox;
    private JCheckBox strokeCheckBox;
    private JCheckBox boldCheckBox;
    private JCheckBox italicCheckBox;
    private Color selectedColor = Color.BLACK;
    private JPanel colorPanel; // 用于颜色选择的面板，供预览监听
    private JSlider rotationSlider; // 新增：旋转角度

    // Fields for image watermark
    private File watermarkImageFile;
    private JLabel watermarkImagePathLabel;
    private JSlider imageWatermarkOpacitySlider;
    private JSlider imageWatermarkScaleSlider;

    // 水印模式选择框
    private JComboBox<String> watermarkModeComboBox;

    // 预览区JPanel
    private JPanel previewPanel;
    private BufferedImage previewImage;
    private BufferedImage originalImage; // 原始选中图片
    private BufferedImage watermarkImageBuffered; // 图片水印缓存
    private int customX = -1, customY = -1; // 自定义位置（以图像像素为单位，文本为包围盒左上角，图片水印为左上角）
    // Preview worker for async rendering
    private volatile SwingWorker<PreviewResult, Void> previewWorker;
    private volatile int previewTaskSeq = 0;

    // 模板管理相关
    private JComboBox<WatermarkTemplate> templateComboBox; // 模板下拉
    private List<WatermarkTemplate> templates = new ArrayList<>(); // 模板列表

    // Result holder for preview generation
    private static class PreviewResult {
        final BufferedImage image;
        final int origW, origH, newW, newH;
        PreviewResult(BufferedImage image, int origW, int origH, int newW, int newH) {
            this.image = image; this.origW = origW; this.origH = origH; this.newW = newW; this.newH = newH;
        }
    }

    public PhotoWatermarkGUI() {
        setTitle("照片水印工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 700)); // 扩展宽度以适应三列

        // 初始化图片列表模型与组件（在使用前）
        imageListModel = new DefaultListModel<>();
        imageList = new JList<>(imageListModel);
        imageList.setCellRenderer(new ImageListCellRenderer());
        imageList.setFixedCellWidth(THUMB_WIDTH + 20);
        imageList.setFixedCellHeight(THUMB_HEIGHT + 30);

        // 主Panel采用BoxLayout横向三列
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        // 左侧图片列表
        JPanel listPanel = createImageListPanel();
        // 固定左侧列表区宽度：略大于缩略图宽度
        int listWidth = THUMB_WIDTH + 60; // 略大于缩略图（从+40调整为+60）
        listPanel.setPreferredSize(new Dimension(listWidth, 700));
        listPanel.setMinimumSize(new Dimension(listWidth, 0));
        listPanel.setMaximumSize(new Dimension(listWidth, Integer.MAX_VALUE));
        mainPanel.add(listPanel);

        // 中间预览区
        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (previewImage != null) {
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    int imgWidth = previewImage.getWidth();
                    int imgHeight = previewImage.getHeight();
                    // 缩放图片以适应预览区
                    double scale = Math.min((double)panelWidth/imgWidth, (double)panelHeight/imgHeight);
                    int drawWidth = (int)(imgWidth * scale);
                    int drawHeight = (int)(imgHeight * scale);
                    g.drawImage(previewImage, (panelWidth-drawWidth)/2, (panelHeight-drawHeight)/2, drawWidth, drawHeight, null);
                }
            }
        };
        // 让中间预览区可伸缩，占用剩余空间
        previewPanel.setPreferredSize(new Dimension(400, 700));
        previewPanel.setMinimumSize(new Dimension(200, 0));
        previewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        previewPanel.setBorder(BorderFactory.createTitledBorder("实时预览区"));
        previewPanel.setBackground(Color.WHITE);
        // 支持自定义拖拽定位
        previewPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (originalImage == null) return;
                Object sel = positionComboBox != null ? positionComboBox.getSelectedItem() : null;
                if (sel == null || !"CUSTOM".equals(sel.toString())) return;
                Rectangle r = getImageDrawRect();
                if (r == null) return;
                if (!r.contains(e.getPoint())) return;
                int baseW = (previewImage != null ? previewImage.getWidth() : originalImage.getWidth());
                int baseH = (previewImage != null ? previewImage.getHeight() : originalImage.getHeight());
                double scale = r.getWidth() / (double) baseW;
                int imgX = (int)Math.round((e.getX() - r.x) / scale);
                int imgY = (int)Math.round((e.getY() - r.y) / scale);
                Dimension d = getCurrentWatermarkBounds();
                int nx = imgX - d.width / 2;
                int ny = imgY - d.height / 2;
                // Clamp in base image space
                nx = Math.max(0, Math.min(nx, baseW - d.width));
                ny = Math.max(0, Math.min(ny, baseH - d.height));
                // Convert back to original image coordinate space for storage
                double fx = baseW / (double) originalImage.getWidth();
                double fy = baseH / (double) originalImage.getHeight();
                customX = (int)Math.round(nx / fx);
                customY = (int)Math.round(ny / fy);
                updatePreview();
            }
        });
        previewPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // 与拖拽相同逻辑，按下即可定位
                if (originalImage == null) return;
                Object sel = positionComboBox != null ? positionComboBox.getSelectedItem() : null;
                if (sel == null || !"CUSTOM".equals(sel.toString())) return;
                Rectangle r = getImageDrawRect();
                if (r == null || !r.contains(e.getPoint())) return;
                int baseW = (previewImage != null ? previewImage.getWidth() : originalImage.getWidth());
                int baseH = (previewImage != null ? previewImage.getHeight() : originalImage.getHeight());
                double scale = r.getWidth() / (double) baseW;
                int imgX = (int)Math.round((e.getX() - r.x) / scale);
                int imgY = (int)Math.round((e.getY() - r.y) / scale);
                Dimension d = getCurrentWatermarkBounds();
                int nx = imgX - d.width / 2;
                int ny = imgY - d.height / 2;
                nx = Math.max(0, Math.min(nx, baseW - d.width));
                ny = Math.max(0, Math.min(ny, baseH - d.height));
                double fx = baseW / (double) originalImage.getWidth();
                double fy = baseH / (double) originalImage.getHeight();
                customX = (int)Math.round(nx / fx);
                customY = (int)Math.round(ny / fy);
                updatePreview();
            }
        });
        mainPanel.add(previewPanel);

        // 右侧参数区
        JPanel rightPanel = createRightPanel(); // 将原有参数区构建逻辑提取为方法
        // 固定右侧参数区宽度
        int rightWidth = 500; // 进一步加宽，确保“选择”按钮可见
        rightPanel.setPreferredSize(new Dimension(rightWidth, 700));
        rightPanel.setMinimumSize(new Dimension(rightWidth, 0));
        rightPanel.setMaximumSize(new Dimension(rightWidth, Integer.MAX_VALUE));
        mainPanel.add(rightPanel);

        // 顶部Panel不变
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton importButton = new JButton("导入图片");
        topPanel.add(importButton);
        importButton.addActionListener(e -> openFileChooser());
        statusLabel = new JLabel("已导入: 0 张图片");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        add(mainPanel, BorderLayout.CENTER);

        // 启用拖拽导入图片功能（将文件拖入窗口任意位置即可）
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = dtde.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (data instanceof java.util.List) {
                            java.util.List<?> items = (java.util.List<?>) data;
                            java.util.List<File> files = new java.util.ArrayList<>();
                            for (Object item : items) {
                                if (item instanceof File) {
                                    files.add((File) item);
                                }
                            }
                            if (!files.isEmpty()) {
                                handleImportedFiles(files);
                            }
                        }
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    Logger.getLogger(PhotoWatermarkGUI.class.getName()).log(Level.SEVERE, null, ex);
                    dtde.dropComplete(false);
                }
            }
        }, true);

        // 图片列表点击事件：刷新预览区
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = imageList.getSelectedIndex();
                if (selectedIndex != -1 && selectedIndex < importedFiles.size()) {
                    File selectedFile = importedFiles.get(selectedIndex);
                    try {
                        originalImage = ImageIO.read(selectedFile);
                        updatePreview();
                        if (originalImage != null) {
                            selectedImageSizeLabel.setText(String.format("选中尺寸: %d x %d", originalImage.getWidth(), originalImage.getHeight()));
                        } else {
                            selectedImageSizeLabel.setText("选中尺寸: N/A");
                        }
                    } catch (IOException ex) {
                        selectedImageSizeLabel.setText("选中尺寸: N/A");
                        originalImage = null;
                        previewImage = null;
                        updatePreview();
                    }
                } else {
                    originalImage = null;
                    previewImage = null;
                    updatePreview();
                }
            }
        });

        // 水印参数相关控件变动时刷新预览区
        // 例如：fontSizeField, watermarkTextField, positionComboBox, opacitySlider 等
        fontSizeField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        watermarkTextField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        positionComboBox.addItemListener(e -> {
            // 已有的监听调用updatePreview，这里补充：当切换为CUSTOM时，初始化到居中
            Object sel = positionComboBox.getSelectedItem();
            if (sel != null && "CUSTOM".equals(sel.toString()) && originalImage != null) {
                // 计算当前水印包围盒尺寸，置于中心
                Dimension d = getCurrentWatermarkBounds();
                int W = originalImage.getWidth();
                int H = originalImage.getHeight();
                customX = Math.max(0, (W - d.width) / 2);
                customY = Math.max(0, (H - d.height) / 2);
            }
            updatePreview();
        });
        opacitySlider.addChangeListener(e -> updatePreview());
        colorPanel.addPropertyChangeListener("background", e -> updatePreview());
        if (boldCheckBox != null) boldCheckBox.addActionListener(e -> updatePreview());
        if (italicCheckBox != null) italicCheckBox.addActionListener(e -> updatePreview());
        if (fontComboBox != null) fontComboBox.addItemListener(e -> updatePreview());
        if (watermarkModeComboBox != null) watermarkModeComboBox.addActionListener(e -> updatePreview());
        if (imageWatermarkScaleSlider != null) imageWatermarkScaleSlider.addChangeListener(e -> updatePreview());
        if (imageWatermarkOpacitySlider != null) imageWatermarkOpacitySlider.addChangeListener(e -> updatePreview());
        // 新增监听：缩放比例、目标宽高、旋转角度 变动时刷新预览
        if (scaleSlider != null) scaleSlider.addChangeListener(e -> {
            JSlider s = (JSlider) e.getSource();
            if (!s.getValueIsAdjusting()) updatePreview();
        });
        if (rotationSlider != null) rotationSlider.addChangeListener(e -> {
            JSlider s = (JSlider) e.getSource();
            if (!s.getValueIsAdjusting()) updatePreview();
        });
        if (widthField != null) widthField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        if (heightField != null) heightField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        // ...可继续添加其他参数控件的监听...

        // 调整窗口大小并居中显示
        pack();
        setLocationRelativeTo(null);

        // 加载模板并刷新下拉（在 UI 构建后）
        templates = TemplateManager.loadTemplates();
        refreshTemplateComboBox();
        // 启动时尝试加载上次会话或应用默认模板
        loadLastSessionOrDefault();
        // 关闭前保存当前会话
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { saveLastSession(); }
        });
    }

    // 加载上次会话或默认模板
    private void loadLastSessionOrDefault() {
        LastSession s = LastSessionManager.load();
        if (s != null) {
            // 优先用 lastTemplateId 找模板
            if (s.lastTemplateId != null) {
                WatermarkTemplate match = null;
                for (WatermarkTemplate t : templates) {
                    if (s.lastTemplateId.equals(t.id)) { match = t; break; }
                }
                if (match != null) {
                    templateComboBox.setSelectedItem(match);
                    applyTemplateToUI(match);
                    return;
                }
            }
            // 回退到快照
            if (s.lastSettings != null) {
                applyTemplateToUI(s.lastSettings);
            }
        } else {
            // 没有会话文件：若有模板则默认应用第一个
            if (!templates.isEmpty()) {
                templateComboBox.setSelectedIndex(0);
                applyTemplateToUI(templates.get(0));
            }
        }
    }

    // 保存当前会话
    private void saveLastSession() {
        try {
            LastSession session = new LastSession();
            WatermarkTemplate selected = (WatermarkTemplate) templateComboBox.getSelectedItem();
            if (selected != null) session.lastTemplateId = selected.id; else session.lastTemplateId = null;
            session.lastSettings = snapshotCurrentSettings();
            session.savedAt = System.currentTimeMillis();
            LastSessionManager.save(session);
        } catch (Exception ignore) { }
    }

    // 快照当前 UI 设置（不持久化 id 与时间）
    private WatermarkTemplate snapshotCurrentSettings() {
        WatermarkTemplate t = new WatermarkTemplate();
        t.id = null; // 会话快照不需要 id
        t.name = "__LAST_SESSION__";
        t.description = "自动保存的上次会话设置";
        t.createdAt = t.updatedAt = System.currentTimeMillis();
        buildTemplateFromUI(t);
        return t;
    }

    // 预览区刷新方法：绘制水印（支持九宫格位置与图片水印）
    private void updatePreview() {
        if (originalImage == null) {
            previewImage = null;
            previewPanel.repaint();
            return;
        }
        // Snapshot current UI state (on EDT)
        final BufferedImage orig = originalImage;
        final BufferedImage wmImg = watermarkImageBuffered;
        final boolean isTextMode = watermarkModeComboBox == null || watermarkModeComboBox.getSelectedIndex() == 0;
        final String pos = positionComboBox != null ? (String) positionComboBox.getSelectedItem() : "CENTER";
        final int fontSize = parseInt(fontSizeField != null ? fontSizeField.getText() : "150");
        final boolean bold = boldCheckBox != null && boldCheckBox.isSelected();
        final boolean italic = italicCheckBox != null && italicCheckBox.isSelected();
        final String fontNameRaw = fontComboBox != null ? (String) fontComboBox.getSelectedItem() : "SansSerif";
        final String fontName = (fontNameRaw == null || "系统字体".equals(fontNameRaw)) ? "SansSerif" : fontNameRaw;
        final float textOpacity = opacitySlider != null ? opacitySlider.getValue() / 100f : 1.0f;
        final Color color = selectedColor;
        final boolean shadow = shadowCheckBox != null && shadowCheckBox.isSelected();
        final boolean stroke = strokeCheckBox != null && strokeCheckBox.isSelected();
        final String watermarkText = watermarkTextField != null ? watermarkTextField.getText() : "";
        final int width = parseInt(widthField != null ? widthField.getText() : "0");
        final int height = parseInt(heightField != null ? heightField.getText() : "0");
        final double scale = scaleSlider != null ? scaleSlider.getValue() / 100.0 : 1.0;
        final double wmScale = imageWatermarkScaleSlider != null ? imageWatermarkScaleSlider.getValue() / 100.0 : 0.5;
        final float wmOpacity = imageWatermarkOpacitySlider != null ? imageWatermarkOpacitySlider.getValue() / 100f : 0.8f;
        final int cx = customX, cy = customY;
        final double rotationDegrees = rotationSlider != null ? rotationSlider.getValue() : 0;

        // Cancel previous worker if running
        SwingWorker<PreviewResult, Void> old = previewWorker;
        if (old != null && !old.isDone()) {
            old.cancel(true);
        }
        final int seq = ++previewTaskSeq;
        previewWorker = new SwingWorker<PreviewResult, Void>() {
            @Override protected PreviewResult doInBackground() {
                if (isCancelled() || orig == null) return null;
                return computePreview(orig, wmImg, isTextMode, pos, fontSize, bold, italic, fontName, textOpacity, color, shadow, stroke, watermarkText, cx, cy, width, height, scale, wmScale, wmOpacity, rotationDegrees);
            }
            @Override protected void done() {
                if (isCancelled() || seq != previewTaskSeq) return; // superseded
                try {
                    PreviewResult res = get();
                    if (res == null) return;
                    previewImage = res.image;
                    if (selectedImageSizeLabel != null) {
                        selectedImageSizeLabel.setText(String.format("选中尺寸: %d x %d，目标尺寸: %d x %d", res.origW, res.origH, res.newW, res.newH));
                    }
                    previewPanel.repaint();
                } catch (Exception ignore) { }
            }
        };
        previewWorker.execute();
    }

    // Heavy preview computation off the EDT
    private PreviewResult computePreview(BufferedImage original, BufferedImage watermarkImg, boolean isTextMode, String pos,
                                         int fontSize, boolean bold, boolean italic, String fontName, float textOpacity,
                                         Color color, boolean shadow, boolean stroke, String watermarkText,
                                         int customX, int customY, int targetWInput, int targetHInput, double scale,
                                         double wmScale, float wmOpacity, double rotationDegrees) {
        int origW = original.getWidth();
        int origH = original.getHeight();
        int newW = origW, newH = origH;
        if (scale > 0 && Math.abs(scale - 1.0) > 1e-9) {
            newW = Math.max(1, (int) Math.round(origW * scale));
            newH = Math.max(1, (int) Math.round(origH * scale));
        } else {
            if (targetWInput > 0) newW = targetWInput;
            if (targetHInput > 0) newH = targetHInput;
        }
        if (Thread.currentThread().isInterrupted()) return null;
        // Scale/copy base
        BufferedImage base;
        if (newW != origW || newH != origH) {
            Image scaled = original.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            base = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gb = base.createGraphics();
            gb.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gb.drawImage(scaled, 0, 0, null);
            gb.dispose();
        } else {
            base = new BufferedImage(origW, origH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = base.createGraphics();
            gc.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gc.drawImage(original, 0, 0, null);
            gc.dispose();
        }
        if (Thread.currentThread().isInterrupted()) return null;

        Graphics2D g2d = base.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        String posSafe = (pos == null ? "CENTER" : pos);
        int W = base.getWidth();
        int H = base.getHeight();
        int margin = 20;
        double fx = W / (double) origW;
        double fy = H / (double) origH;

        if (isTextMode) {
            int style = (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, textOpacity))));
            g2d.setFont(new Font(fontName, style, fontSize));
            g2d.setColor(color);
            FontMetrics fm = g2d.getFontMetrics();
            int tw = fm.stringWidth(watermarkText);
            int th = fm.getAscent();
            int x, y;
            if ("CUSTOM".equals(posSafe)) {
                int topX = (int) Math.round(customX * fx);
                int topY = (int) Math.round(customY * fy);
                topX = Math.max(0, Math.min(topX, W - tw));
                topY = Math.max(0, Math.min(topY, H - th));
                x = topX; y = topY + th;
            } else {
                switch (posSafe) {
                    case "TOP_LEFT": x = margin; y = margin + th; break;
                    case "TOP_CENTER": x = (W - tw) / 2; y = margin + th; break;
                    case "TOP_RIGHT": x = W - tw - margin; y = margin + th; break;
                    case "MIDDLE_LEFT": x = margin; y = (H + th) / 2; break;
                    case "CENTER": x = (W - tw) / 2; y = (H + th) / 2; break;
                    case "MIDDLE_RIGHT": x = W - tw - margin; y = (H + th) / 2; break;
                    case "BOTTOM_LEFT": x = margin; y = H - margin; break;
                    case "BOTTOM_CENTER": x = (W - tw) / 2; y = H - margin; break;
                    case "BOTTOM_RIGHT": x = W - tw - margin; y = H - margin; break;
                    default: x = (W - tw) / 2; y = (H + th) / 2; break;
                }
            }
            double centerX = x + tw / 2.0;
            double centerY = y - th / 2.0; // baseline center
            AffineTransform oldTx = g2d.getTransform();
            g2d.rotate(Math.toRadians(rotationDegrees), centerX, centerY);
            if (shadow) {
                g2d.setColor(new Color(0, 0, 0, 120));
                g2d.drawString(watermarkText, x + 2, y + 2);
                g2d.setColor(color);
            }
            if (stroke) {
                g2d.setColor(Color.BLACK);
                g2d.drawString(watermarkText, x - 1, y);
                g2d.drawString(watermarkText, x + 1, y);
                g2d.drawString(watermarkText, x, y - 1);
                g2d.drawString(watermarkText, x, y + 1);
                g2d.setColor(color);
            }
            g2d.drawString(watermarkText, x, y);
            g2d.setTransform(oldTx);
        } else if (watermarkImg != null) {
            int w = Math.max(1, (int) (watermarkImg.getWidth() * wmScale));
            int h = Math.max(1, (int) (watermarkImg.getHeight() * wmScale));
            Image scaled = watermarkImg.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, wmOpacity))));
            int x, y;
            if ("CUSTOM".equals(posSafe)) {
                int topX = (int) Math.round(customX * fx);
                int topY = (int) Math.round(customY * fy);
                topX = Math.max(0, Math.min(topX, W - w));
                topY = Math.max(0, Math.min(topY, H - h));
                x = topX; y = topY;
            } else {
                switch (posSafe) {
                    case "TOP_LEFT": x = margin; y = margin; break;
                    case "TOP_CENTER": x = (W - w) / 2; y = margin; break;
                    case "TOP_RIGHT": x = W - w - margin; y = margin; break;
                    case "MIDDLE_LEFT": x = margin; y = (H - h) / 2; break;
                    case "CENTER": x = (W - w) / 2; y = (H - h) / 2; break;
                    case "MIDDLE_RIGHT": x = W - w - margin; y = (H - h) / 2; break;
                    case "BOTTOM_LEFT": x = margin; y = H - h - margin; break;
                    case "BOTTOM_CENTER": x = (W - w) / 2; y = H - h - margin; break;
                    case "BOTTOM_RIGHT": x = W - w - margin; y = H - h - margin; break;
                    default: x = (W - w) / 2; y = (H - h) / 2; break;
                }
            }
            // Rotate around center and draw once
            double centerX = x + w / 2.0;
            double centerY = y + h / 2.0;
            AffineTransform oldTx = g2d.getTransform();
            g2d.rotate(Math.toRadians(rotationDegrees), centerX, centerY);
            g2d.drawImage(scaled, x, y, null);
            g2d.setTransform(oldTx);
        }
        g2d.dispose();
        return new PreviewResult(base, origW, origH, newW, newH);
    }

    // 计算当前水印包围盒尺寸（文本包围盒/图片水印缩放后的尺寸）
    private Dimension getCurrentWatermarkBounds() {
        boolean isTextMode = watermarkModeComboBox == null || watermarkModeComboBox.getSelectedIndex() == 0;
        if (isTextMode) {
            String text = watermarkTextField != null ? watermarkTextField.getText() : "";
            int fontSize = 150;
            try { fontSize = Integer.parseInt(fontSizeField.getText()); } catch (Exception ignore) {}
            boolean bold = boldCheckBox != null && boldCheckBox.isSelected();
            boolean italic = italicCheckBox != null && italicCheckBox.isSelected();
            int style = (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);
            String fontName = fontComboBox != null ? (String) fontComboBox.getSelectedItem() : "SansSerif";
            if (fontName == null || "系统字体".equals(fontName)) fontName = "SansSerif";
            BufferedImage tmp = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = tmp.createGraphics();
            g.setFont(new Font(fontName, style, fontSize));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();
            g.dispose();
            return new Dimension(Math.max(1, tw), Math.max(1, th));
        } else {
            if (watermarkImageBuffered == null) return new Dimension(1,1);
            double scale = imageWatermarkScaleSlider != null ? imageWatermarkScaleSlider.getValue() / 100.0 : 0.5;
            int w = Math.max(1, (int)(watermarkImageBuffered.getWidth() * scale));
            int h = Math.max(1, (int)(watermarkImageBuffered.getHeight() * scale));
            return new Dimension(w, h);
        }
    }

    // 获取图像在预览面板中的绘制矩形
    private Rectangle getImageDrawRect() {
        if (originalImage == null) return null;
        int panelWidth = previewPanel.getWidth();
        int panelHeight = previewPanel.getHeight();
        int imgWidth = (previewImage != null ? previewImage.getWidth() : originalImage.getWidth());
        int imgHeight = (previewImage != null ? previewImage.getHeight() : originalImage.getHeight());
        double scale = Math.min((double)panelWidth/imgWidth, (double)panelHeight/imgHeight);
        int drawWidth = (int)(imgWidth * scale);
        int drawHeight = (int)(imgHeight * scale);
        int x = (panelWidth - drawWidth) / 2;
        int y = (panelHeight - drawHeight) / 2;
        return new Rectangle(x, y, drawWidth, drawHeight);
    }

    // 简单文档监听器工具类：去掉abstract以便直接实例化
    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable onChange;
        public SimpleDocumentListener(Runnable onChange) { this.onChange = onChange; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }

    // 参数区构建方法（原rightPanel相关代码迁移至此）
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(350, 700));
        rightPanel.setBackground(Color.WHITE);
        // 基础选项
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        gridPanel.add(new JLabel("输出文件夹:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2; // 让文本框+按钮占据两列
        JPanel outputFolderPanel = new JPanel(new BorderLayout(6, 0));
        outputFolderField = new JTextField();
        outputFolderField.setEditable(false);
        outputFolderField.setColumns(10); // 进一步减小列数给按钮留出足够空间
        outputFolderPanel.add(outputFolderField, BorderLayout.CENTER);
        JButton chooseOutputFolderButton = new JButton("选择");
        chooseOutputFolderButton.addActionListener(e -> chooseOutputFolder());
        outputFolderPanel.add(chooseOutputFolderButton, BorderLayout.EAST);
        gridPanel.add(outputFolderPanel, gbc);
        gbc.gridwidth = 1; // 重置
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("输出格式:"), gbc);
        gbc.gridx = 1;
        formatComboBox = new JComboBox<>(new String[]{"JPEG", "PNG"});
        gridPanel.add(formatComboBox, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("命名规则:"), gbc);
        gbc.gridx = 1;
        namingRuleComboBox = new JComboBox<>(new String[]{"保留原文件名", "添加前缀", "添加后缀", "添加前后缀"});
        gridPanel.add(namingRuleComboBox, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        prefixLabel = new JLabel("前缀:");
        gridPanel.add(prefixLabel, gbc);
        gbc.gridx = 1;
        prefixField = new JTextField("wm_");
        prefixField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(prefixField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        suffixLabel = new JLabel("后缀:");
        gridPanel.add(suffixLabel, gbc);
        gbc.gridx = 1;
        suffixField = new JTextField("_watermarked");
        suffixField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(suffixField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("水印字体大小:"), gbc);
        gbc.gridx = 1;
        fontSizeField = new JTextField("150");
        fontSizeField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(fontSizeField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("水印颜色:"), gbc);
        gbc.gridx = 1;
        colorPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(selectedColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        colorPanel.setPreferredSize(new Dimension(40, 28));
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        colorPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color newColor = JColorChooser.showDialog(PhotoWatermarkGUI.this, "选择水印颜色", selectedColor);
                if (newColor != null) {
                    selectedColor = newColor;
                    colorPanel.repaint();
                    updatePreview(); // 颜色改变时立即刷新预览
                }
            }
        });
        gridPanel.add(colorPanel, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("水印位置:"), gbc);
        gbc.gridx = 1;
        positionComboBox = new JComboBox<>(new String[]{
            "TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
            "MIDDLE_LEFT", "CENTER", "MIDDLE_RIGHT",
            "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT",
            "CUSTOM"
        });
        gridPanel.add(positionComboBox, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("JPEG质量:"), gbc);
        gbc.gridx = 1;
        jpegQualitySlider = new JSlider(0, 100, 90);
        jpegQualitySlider.setMajorTickSpacing(20);
        jpegQualitySlider.setPaintTicks(true);
        jpegQualitySlider.setPaintLabels(true);
        jpegQualitySlider.setPreferredSize(new Dimension(120, 45)); // Increased height
        gridPanel.add(jpegQualitySlider, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        // 根据格式启用/禁用质量滑块
        formatComboBox.addItemListener(e -> {
            String format = (String) formatComboBox.getSelectedItem();
            jpegQualitySlider.setEnabled("JPEG".equals(format));
        });
        gbc.gridwidth = 1;
        
        // 为命名规则添加监听器（此处 prefixLabel/suffixLabel 已初始化）
        namingRuleComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedRule = (String) e.getItem();
                boolean showPrefix = "添加前缀".equals(selectedRule) || "添加前后缀".equals(selectedRule);
                boolean showSuffix = "添加后缀".equals(selectedRule) || "添加前后缀".equals(selectedRule);
                prefixLabel.setVisible(showPrefix);
                prefixField.setVisible(showPrefix);
                suffixLabel.setVisible(showSuffix);
                suffixField.setVisible(showSuffix);
                gridPanel.revalidate();
                gridPanel.repaint();
            }
        });
        
        // 初始化可见性
        String initialRule = (String) namingRuleComboBox.getSelectedItem();
        boolean showPrefixInitially = "添加前缀".equals(initialRule) || "添加前后缀".equals(initialRule);
        boolean showSuffixInitially = "添加后缀".equals(initialRule) || "添加前后缀".equals(initialRule);
        prefixLabel.setVisible(showPrefixInitially);
        prefixField.setVisible(showPrefixInitially);
        suffixLabel.setVisible(showSuffixInitially);
        suffixField.setVisible(showSuffixInitially);
        
        // 水印模式选择
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("水印模式:"), gbc);
        gbc.gridx = 1;
        watermarkModeComboBox = new JComboBox<>(new String[]{"文字水印", "图片水印"});
        gridPanel.add(watermarkModeComboBox, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        // --- 文字水印参数组件 ---
        gbc.gridx = 0; gbc.gridy++;
        JLabel textLabel = new JLabel("水印文本:");
        gridPanel.add(textLabel, gbc);
        gbc.gridx = 1;
        watermarkTextField = new JTextField("© 2025 by YourName");
        watermarkTextField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(watermarkTextField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel fontLabel = new JLabel("字体选择:");
        gridPanel.add(fontLabel, gbc);
        gbc.gridx = 1;
        fontComboBox = new JComboBox<>(new String[]{"系统字体", "Arial", "Courier New", "Georgia", "Times New Roman"});
        gridPanel.add(fontComboBox, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel styleLabel = new JLabel("字体样式:");
        gridPanel.add(styleLabel, gbc);
        gbc.gridx = 1;
        boldCheckBox = new JCheckBox("粗体");
        gridPanel.add(boldCheckBox, gbc);
        gbc.gridx = 2;
        italicCheckBox = new JCheckBox("斜体");
        gridPanel.add(italicCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel opacityLabel = new JLabel("透明度:");
        gridPanel.add(opacityLabel, gbc);
        gbc.gridx = 1;
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.setOpaque(true);
        opacitySlider.setBackground(Color.WHITE);
        opacitySlider.setPreferredSize(new Dimension(120, 45));
        gridPanel.add(opacitySlider, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel shadowStrokeLabel = new JLabel("阴影/描边:");
        gridPanel.add(shadowStrokeLabel, gbc);
        gbc.gridx = 1;
        shadowCheckBox = new JCheckBox("阴影");
        gridPanel.add(shadowCheckBox, gbc);
        gbc.gridx = 2;
        strokeCheckBox = new JCheckBox("描边");
        gridPanel.add(strokeCheckBox, gbc);

        // --- 图片水印参数组件 ---
        gbc.gridx = 0; gbc.gridy++;
        JLabel imageLabel = new JLabel("水印图片:");
        gridPanel.add(imageLabel, gbc);
        gbc.gridx = 1;
        JButton selectWatermarkImageButton = new JButton("选择图片");
        selectWatermarkImageButton.addActionListener(e -> chooseWatermarkImage());
        gridPanel.add(selectWatermarkImageButton, gbc);
        gbc.gridx = 2;
        watermarkImagePathLabel = new JLabel("未选择图片");
        gridPanel.add(watermarkImagePathLabel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel imageScaleLabel = new JLabel("缩放比例(%):");
        gridPanel.add(imageScaleLabel, gbc);
        gbc.gridx = 1;
        imageWatermarkScaleSlider = new JSlider(10, 100, 50);
        imageWatermarkScaleSlider.setMajorTickSpacing(30);
        imageWatermarkScaleSlider.setPaintTicks(true);
        imageWatermarkScaleSlider.setPaintLabels(true);
        imageWatermarkScaleSlider.setOpaque(true);
        imageWatermarkScaleSlider.setBackground(Color.WHITE);
        imageWatermarkScaleSlider.setPreferredSize(new Dimension(120, 45));
        gridPanel.add(imageWatermarkScaleSlider, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel imageOpacityLabel = new JLabel("透明度(%):");
        gridPanel.add(imageOpacityLabel, gbc);
        gbc.gridx = 1;
        imageWatermarkOpacitySlider = new JSlider(0, 100, 80);
        imageWatermarkOpacitySlider.setMajorTickSpacing(20);
        imageWatermarkOpacitySlider.setPaintTicks(true);
        imageWatermarkOpacitySlider.setPaintLabels(true);
        imageWatermarkOpacitySlider.setOpaque(true);
        imageWatermarkOpacitySlider.setBackground(Color.WHITE);
        imageWatermarkOpacitySlider.setPreferredSize(new Dimension(120, 45));
        gridPanel.add(imageWatermarkOpacitySlider, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        // 旋转角度
        gbc.gridx = 0; gbc.gridy++;
        JLabel rotationLabel = new JLabel("旋转角度(°):");
        gridPanel.add(rotationLabel, gbc);
        gbc.gridx = 1;
        rotationSlider = new JSlider(-180, 180, 0);
        rotationSlider.setMajorTickSpacing(90);
        rotationSlider.setMinorTickSpacing(15);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setPaintLabels(true);
        rotationSlider.setPreferredSize(new Dimension(220, 45));
        gridPanel.add(rotationSlider, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        // 初始只显示文字水印参数
        imageLabel.setVisible(false);
        selectWatermarkImageButton.setVisible(false);
        watermarkImagePathLabel.setVisible(false);
        imageScaleLabel.setVisible(false);
        imageWatermarkScaleSlider.setVisible(false);
        imageOpacityLabel.setVisible(false);
        imageWatermarkOpacitySlider.setVisible(false);

        // 切换水印模式时显示/隐藏相关参数
        watermarkModeComboBox.addActionListener(e -> {
            boolean isText = watermarkModeComboBox.getSelectedIndex() == 0;
            textLabel.setVisible(isText);
            watermarkTextField.setVisible(isText);
            fontLabel.setVisible(isText);
            fontComboBox.setVisible(isText);
            styleLabel.setVisible(isText);
            boldCheckBox.setVisible(isText);
            italicCheckBox.setVisible(isText);
            opacityLabel.setVisible(isText);
            opacitySlider.setVisible(isText);
            shadowStrokeLabel.setVisible(isText);
            shadowCheckBox.setVisible(isText);
            strokeCheckBox.setVisible(isText);
            imageLabel.setVisible(!isText);
            selectWatermarkImageButton.setVisible(!isText);
            watermarkImagePathLabel.setVisible(!isText);
            imageScaleLabel.setVisible(!isText);
            imageWatermarkScaleSlider.setVisible(!isText);
            imageOpacityLabel.setVisible(!isText);
            imageWatermarkOpacitySlider.setVisible(!isText);
        });

        // Remove gray separator; use a white spacer instead
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(8, 0, 4, 0);
        JPanel spacer = new JPanel();
        spacer.setOpaque(true);
        spacer.setBackground(Color.WHITE);
        spacer.setPreferredSize(new Dimension(1, 12));
        gridPanel.add(spacer, gbc);

        // Following rows default insets
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 6, 6, 6); // reset to default insets for following rows

        // 宽度
        gbc.gridx = 0; gbc.gridy++;
        widthLabel = new JLabel("宽度(像素):");
        gridPanel.add(widthLabel, gbc);
        gbc.gridx = 1;
        widthField = new JTextField("0");
        widthField.setToolTipText("0表示保持原始宽度");
        widthField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(widthField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        // 高度
        gbc.gridx = 0; gbc.gridy++;
        heightLabel = new JLabel("高度(像素):");
        gridPanel.add(heightLabel, gbc);
        gbc.gridx = 1;
        heightField = new JTextField("0");
        heightField.setToolTipText("0表示保持原始高度");
        heightField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(heightField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        // 选中图片尺寸标签
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 3;
        selectedImageSizeLabel = new JLabel("选中尺寸: N/A");
        selectedImageSizeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gridPanel.add(selectedImageSizeLabel, gbc);
        gbc.gridwidth = 1;

        // 缩放比例
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gridPanel.add(new JLabel("缩放比例(%):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        scaleSlider = new JSlider(10, 200, 100);
        scaleSlider.setMajorTickSpacing(50);
        scaleSlider.setMinorTickSpacing(10);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setToolTipText("设置图片缩放比例，100%为原始大小");
        scaleSlider.setPreferredSize(new Dimension(220, 45));
        scaleSlider.setMaximumSize(new Dimension(220, 45));
        gridPanel.add(scaleSlider, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1; // Reset gridwidth
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 1;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);

        // 高级选项面板
        advancedPanel = new JPanel();
        advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.Y_AXIS));
        advancedPanel.setBackground(Color.WHITE);
        advancedPanel.add(gridPanel);
        JScrollPane advancedScrollPane = new JScrollPane(advancedPanel);
        advancedScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        advancedScrollPane.getViewport().setBackground(Color.WHITE);
        rightPanel.add(advancedScrollPane, BorderLayout.CENTER);

        // 导出按钮面板
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        exportPanel.setBackground(Color.WHITE);
        JButton exportButton = new JButton("导出图片");
        exportButton.setBackground(Color.WHITE);
        exportButton.setForeground(new Color(0, 123, 255));
        exportButton.setFocusPainted(false);
        exportButton.setFont(exportButton.getFont().deriveFont(Font.BOLD, 14));
        exportButton.setPreferredSize(new Dimension(120, 36));
        exportButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        exportButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 导出按钮事件，增加粗体/斜体参数
        exportButton.addActionListener(e -> {
            boolean bold = boldCheckBox.isSelected();
            boolean italic = italicCheckBox.isSelected();
            exportImages(bold, italic);
        });
        exportPanel.add(exportButton);
        rightPanel.add(exportPanel, BorderLayout.SOUTH);

        // 模板管理面板 (置于顶部north容器)
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(Color.WHITE);
        // 进度条放最上
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        northContainer.add(progressBar);
        JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        templatePanel.setBackground(Color.WHITE);
        templatePanel.setBorder(BorderFactory.createTitledBorder("模板管理"));
        templateComboBox = new JComboBox<>();
        templateComboBox.setPreferredSize(new Dimension(160, 28));
        JButton applyTemplateBtn = new JButton("应用模板");
        JButton saveTemplateBtn = new JButton("保存模板");
        JButton deleteTemplateBtn = new JButton("删除模板");
        applyTemplateBtn.addActionListener(e -> {
            WatermarkTemplate t = (WatermarkTemplate) templateComboBox.getSelectedItem();
            if (t != null) {
                applyTemplateToUI(t);
            } else {
                JOptionPane.showMessageDialog(this, "没有选择模板", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        saveTemplateBtn.addActionListener(e -> saveTemplateFlow());
        deleteTemplateBtn.addActionListener(e -> deleteTemplateFlow());
        templatePanel.add(templateComboBox);
        templatePanel.add(applyTemplateBtn);
        templatePanel.add(saveTemplateBtn);
        templatePanel.add(deleteTemplateBtn);
        northContainer.add(templatePanel);
        rightPanel.add(northContainer, BorderLayout.NORTH);

        return rightPanel;
    }

    // 重新加入：左侧图片列表面板创建方法（之前在编辑中被移除）
    private JPanel createImageListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("已导入图片列表"));
        JScrollPane listScrollPane = new JScrollPane(imageList);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeButton = new JButton("删除选中");
        removeButton.addActionListener(e -> removeSelectedImages());
        buttonPanel.add(removeButton);
        JButton clearButton = new JButton("清空列表");
        clearButton.addActionListener(e -> clearAllImages());
        buttonPanel.add(clearButton);
        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        return listPanel;
    }

    private void saveTemplateFlow() {
        String name = JOptionPane.showInputDialog(this, "输入模板名称", "保存模板", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return; // 取消
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "名称不能为空");
            return;
        }
        // 查找是否重名
        WatermarkTemplate existing = null;
        for (WatermarkTemplate t : templates) {
            if (name.equals(t.name)) { existing = t; break; }
        }
        if (existing != null) {
            int ans = JOptionPane.showConfirmDialog(this, "已存在同名模板，是否覆盖?", "确认", JOptionPane.YES_NO_OPTION);
            if (ans != JOptionPane.YES_OPTION) return;
            buildTemplateFromUI(existing); // 覆盖内容
            existing.updatedAt = System.currentTimeMillis();
        } else {
            WatermarkTemplate t = new WatermarkTemplate();
            t.id = UUID.randomUUID().toString();
            t.name = name;
            t.description = ""; // 可后续扩展
            t.createdAt = System.currentTimeMillis();
            t.updatedAt = t.createdAt;
            buildTemplateFromUI(t);
            templates.add(t);
        }
        if (TemplateManager.saveTemplates(templates)) {
            refreshTemplateComboBox();
            JOptionPane.showMessageDialog(this, "模板保存成功");
        } else {
            JOptionPane.showMessageDialog(this, "模板保存失败", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteTemplateFlow() {
        WatermarkTemplate t = (WatermarkTemplate) templateComboBox.getSelectedItem();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "请选择要删除的模板");
            return;
        }
        int ans = JOptionPane.showConfirmDialog(this, "确认删除模板: " + t.name + "?", "删除确认", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        for (Iterator<WatermarkTemplate> it = templates.iterator(); it.hasNext();) {
            WatermarkTemplate w = it.next();
            if (w.id != null && w.id.equals(t.id)) { it.remove(); break; }
        }
        if (TemplateManager.saveTemplates(templates)) {
            refreshTemplateComboBox();
        } else {
            JOptionPane.showMessageDialog(this, "删除后保存失败", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTemplateComboBox() {
        if (templateComboBox == null) return;
        templateComboBox.removeAllItems();
        templates.sort((a,b) -> a.name == null ? 1 : b.name == null ? -1 : a.name.compareToIgnoreCase(b.name));
        for (WatermarkTemplate t : templates) templateComboBox.addItem(t);
    }

    private void buildTemplateFromUI(WatermarkTemplate t) {
        // 通用 / 导出参数
        t.namingRule = (String) namingRuleComboBox.getSelectedItem();
        t.prefix = prefixField.getText();
        t.suffix = suffixField.getText();
        t.outputFormat = (String) formatComboBox.getSelectedItem();
        t.jpegQuality = jpegQualitySlider.getValue();
        t.targetWidth = parseInt(widthField.getText());
        t.targetHeight = parseInt(heightField.getText());
        t.scalePercent = scaleSlider.getValue();
        t.position = (String) positionComboBox.getSelectedItem();
        t.rotationDegrees = rotationSlider != null ? rotationSlider.getValue() : 0;
        if ("CUSTOM".equals(t.position)) {
            t.customX = customX;
            t.customY = customY;
        } else {
            t.customX = null; t.customY = null;
        }
        // 模式
        boolean isText = watermarkModeComboBox.getSelectedIndex() == 0;
        t.mode = isText ? "TEXT" : "IMAGE";
        if (isText) {
            t.text = watermarkTextField.getText();
            t.fontName = (String) fontComboBox.getSelectedItem();
            t.fontSize = parseInt(fontSizeField.getText());
            t.bold = boldCheckBox.isSelected();
            t.italic = italicCheckBox.isSelected();
            t.color = colorToHex(selectedColor);
            t.textOpacity = opacitySlider.getValue();
            t.shadow = shadowCheckBox.isSelected();
            t.stroke = strokeCheckBox.isSelected();
            // 清理图片字段
            t.watermarkImagePath = null;
            t.watermarkScale = 0.5;
            t.watermarkOpacity = 80;
        } else {
            t.watermarkImagePath = watermarkImageFile != null ? watermarkImageFile.getAbsolutePath() : null;
            t.watermarkScale = imageWatermarkScaleSlider.getValue() / 100.0;
            t.watermarkOpacity = imageWatermarkOpacitySlider.getValue();
            // 清理文本字段
            t.text = watermarkTextField.getText(); // 仍保留文本以便模式切换时展示原文本
            t.fontName = (String) fontComboBox.getSelectedItem();
            t.fontSize = parseInt(fontSizeField.getText());
            t.bold = boldCheckBox.isSelected();
            t.italic = italicCheckBox.isSelected();
            t.color = colorToHex(selectedColor);
            t.textOpacity = opacitySlider.getValue();
            t.shadow = shadowCheckBox.isSelected();
            t.stroke = strokeCheckBox.isSelected();
        }
    }

    private void applyTemplateToUI(WatermarkTemplate t) {
        if (t == null) return;
        namingRuleComboBox.setSelectedItem(t.namingRule);
        prefixField.setText(t.prefix != null ? t.prefix : "");
        suffixField.setText(t.suffix != null ? t.suffix : "");
        formatComboBox.setSelectedItem(t.outputFormat != null ? t.outputFormat : "JPEG");
        jpegQualitySlider.setValue(t.jpegQuality > 0 ? t.jpegQuality : 90);
        widthField.setText(String.valueOf(t.targetWidth));
        heightField.setText(String.valueOf(t.targetHeight));
        scaleSlider.setValue(t.scalePercent > 0 ? t.scalePercent : 100);
        positionComboBox.setSelectedItem(t.position != null ? t.position : "CENTER");
        if (rotationSlider != null) rotationSlider.setValue((int)Math.round(t.rotationDegrees));
        if ("CUSTOM".equals(t.position)) {
            customX = t.customX != null ? t.customX : 0;
            customY = t.customY != null ? t.customY : 0;
        }
        // 模式
        if ("IMAGE".equalsIgnoreCase(t.mode)) {
            watermarkModeComboBox.setSelectedIndex(1);
        } else {
            watermarkModeComboBox.setSelectedIndex(0);
        }
        // 文本参数
        if (t.text != null) watermarkTextField.setText(t.text);
        if (t.fontName != null) fontComboBox.setSelectedItem(t.fontName);
        fontSizeField.setText(String.valueOf(t.fontSize > 0 ? t.fontSize : 150));
        boldCheckBox.setSelected(t.bold);
        italicCheckBox.setSelected(t.italic);
        if (t.color != null) {
            try { selectedColor = parseHexColor(t.color); } catch (Exception ignore) {}
            colorPanel.repaint();
        }
        if (t.textOpacity >= 0 && t.textOpacity <= 100) opacitySlider.setValue(t.textOpacity);
        shadowCheckBox.setSelected(t.shadow);
        strokeCheckBox.setSelected(t.stroke);
        // 图片参数
        if (t.watermarkImagePath != null && !t.watermarkImagePath.isEmpty()) {
            File f = new File(t.watermarkImagePath);
            if (f.exists()) {
                watermarkImageFile = f;
                try { watermarkImageBuffered = ImageIO.read(f); } catch (IOException ex) { watermarkImageBuffered = null; }
                watermarkImagePathLabel.setText(f.getName());
                watermarkImagePathLabel.setToolTipText(f.getAbsolutePath());
            }
        }
        if (t.watermarkScale > 0) imageWatermarkScaleSlider.setValue((int)Math.round(t.watermarkScale * 100));
        if (t.watermarkOpacity >= 0 && t.watermarkOpacity <= 100) imageWatermarkOpacitySlider.setValue(t.watermarkOpacity);
        updatePreview();
    }

    private String colorToHex(Color c) { return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()); }
    private Color parseHexColor(String hex) {
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() == 6) {
            int r = Integer.parseInt(h.substring(0,2),16);
            int g = Integer.parseInt(h.substring(2,4),16);
            int b = Integer.parseInt(h.substring(4,6),16);
            return new Color(r,g,b);
        }
        throw new IllegalArgumentException("Bad hex color: " + hex);
    }

    // 删除选中的图片
    private void removeSelectedImages() {
        int[] selectedIndices = imageList.getSelectedIndices();
        // 从后往前删除，避免索引变化问题
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            int index = selectedIndices[i];
            imageListModel.removeElementAt(index);
            importedFiles.remove(index);
        }
        updateStatusLabel();
    }
    
    // 清空所有图片
    private void clearAllImages() {
        imageListModel.clear();
        importedFiles.clear();
        updateStatusLabel();
        // Reset the label when the list is cleared
        selectedImageSizeLabel.setText("选中尺寸: N/A");
    }
    
    // 更新状态标签
    private void updateStatusLabel() {
        statusLabel.setText(String.format("已导入: %d 张图片", importedFiles.size()));
    }

    private void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif", "bmp"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            if (files.length == 0) {
                File singleFile = fileChooser.getSelectedFile();
                if (singleFile != null) {
                    handleImportedFiles(List.of(singleFile));
                }
            } else {
                handleImportedFiles(List.of(files));
            }
        }
    }

    private void handleImportedFiles(List<File> files) {
        for (File file : files) {
            if (file.isFile() && isImageFile(file)) {
                addImage(file);
                importedFiles.add(file);
            } else if (file.isDirectory()) {
                File[] subFiles = file.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        if (subFile.isFile() && isImageFile(subFile)) {
                            addImage(subFile);
                            importedFiles.add(subFile);
                        }
                    }
                }
            }
        }
        // After processing all files, update the status once so the count is correct
        updateStatusLabel();
    }

    // 辅助方法：获取文件后缀
    private String getFileSuffix(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return dot == -1 ? "" : n.substring(dot + 1).toLowerCase();
    }

    // 辅助方法：读取首帧图片，兼容tiff/bmp
    private BufferedImage readFirstImage(File file) throws IOException {
        String suffix = getFileSuffix(file);
        java.util.Iterator<javax.imageio.ImageReader> it = javax.imageio.ImageIO.getImageReadersBySuffix(suffix);
        if (it.hasNext()) {
            javax.imageio.ImageReader reader = it.next();
            try (javax.imageio.stream.ImageInputStream iis = javax.imageio.ImageIO.createImageInputStream(file)) {
                reader.setInput(iis, true, true);
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        return javax.imageio.ImageIO.read(file);
    }

    // 修改后的addImage方法，支持tiff/bmp缩略图
    private void addImage(File file) {
        try {
            BufferedImage original = readFirstImage(file);
            if (original == null) throw new IOException("无法读取: " + file.getName());
            int ow = original.getWidth();
            int oh = original.getHeight();
            if (ow <= 0 || oh <= 0) throw new IOException("尺寸异常: " + file.getName());
            double scale = Math.min((double) THUMB_WIDTH / ow, (double) THUMB_HEIGHT / oh);
            int tw = Math.max(1, (int) Math.round(ow * scale));
            int th = Math.max(1, (int) Math.round(oh * scale));
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, tw, th, null);
            g.dispose();
            ImageIcon icon = new ImageIcon(thumb);
            icon.setDescription(file.getName());
            imageListModel.addElement(icon);
        } catch (Exception ex) {
            // 回退旧方案
            try {
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(THUMB_WIDTH, THUMB_HEIGHT, Image.SCALE_SMOOTH);
                icon = new ImageIcon(img);
                icon.setDescription(file.getName());
                imageListModel.addElement(icon);
            } catch (Exception ignore) {
                JOptionPane.showMessageDialog(this, "无法加载图片: " + file.getName(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 自定义渲染器
    private static class ImageListCellRenderer extends JLabel implements ListCellRenderer<ImageIcon> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ImageIcon> list, ImageIcon value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(value);
            setText(value.getDescription());
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
            setPreferredSize(new Dimension(100, 120));
            setOpaque(true);
            setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
            return this;
        }
    }

    public String getSelectedOutputFormat() {
        return (String) formatComboBox.getSelectedItem();
    }

    private void chooseWatermarkImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("图片文件 (PNG, JPG, GIF)", "png", "jpg", "jpeg", "gif"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            watermarkImageFile = fileChooser.getSelectedFile();
            watermarkImagePathLabel.setText(watermarkImageFile.getName());
            watermarkImagePathLabel.setToolTipText(watermarkImageFile.getAbsolutePath());
            try {
                watermarkImageBuffered = ImageIO.read(watermarkImageFile);
            } catch (IOException ex) {
                watermarkImageBuffered = null;
            }
            updatePreview();
        }
    }

    private void chooseOutputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            outputFolder = chooser.getSelectedFile();
            outputFolderField.setText(outputFolder.getAbsolutePath());
        }
    }

    // 修改导出方法签名，增加粗体/斜体参数
    private void exportImages(boolean bold, boolean italic) {
        if (outputFolder == null) {
            JOptionPane.showMessageDialog(this, "请先选择输出文件夹！");
            return;
        }
        for (File file : importedFiles) {
            if (file.getParentFile().equals(outputFolder)) {
                JOptionPane.showMessageDialog(this, "禁止导出到原文件夹: " + file.getParent());
                return;
            }
        }
        String namingRule = (String) namingRuleComboBox.getSelectedItem();
        String prefix = prefixField.getText();
        String suffix = suffixField.getText();
        int jpegQuality = jpegQualitySlider.getValue();
        String format = getSelectedOutputFormat();
        int width = parseInt(widthField.getText());
        int height = parseInt(heightField.getText());
        double scale = scaleSlider.getValue() / 100.0;
        double rotationDegrees = rotationSlider != null ? rotationSlider.getValue() : 0.0;

        boolean isTextWatermark = watermarkModeComboBox.getSelectedIndex() == 0;

        if (isTextWatermark) {
            int fontSize = parseInt(fontSizeField.getText());
            String positionStr = (String) positionComboBox.getSelectedItem();
            String watermarkText = watermarkTextField.getText();
            String fontName = (String) fontComboBox.getSelectedItem();
            int opacity = opacitySlider.getValue();
            boolean shadow = shadowCheckBox.isSelected();
            boolean stroke = strokeCheckBox.isSelected();

            for (File file : importedFiles) {
                String baseName = file.getName();
                String nameNoExt = baseName.contains(".") ? baseName.substring(0, baseName.lastIndexOf('.')) : baseName;
                String ext = format.equals("PNG") ? ".png" : ".jpg";
                String outName = nameNoExt;
                // 根据命名规则添加前缀/后缀
                if ("添加前缀".equals(namingRule) || "添加前后缀".equals(namingRule)) {
                    outName = prefix + outName;
                }
                if ("添加后缀".equals(namingRule) || "添加前后缀".equals(namingRule)) {
                    outName = outName + suffix;
                }
                outName += ext;
                File outFile = new File(outputFolder, outName);
                if ("CUSTOM".equals(positionStr)) {
                    // 自定义坐标：文本包围盒左上角 customX/customY
                    PhotoWatermarkApp.processImageGUICustom(file, outFile, fontSize, selectedColor, format, jpegQuality, width, height, scale, watermarkText, fontName, opacity, shadow, stroke, bold, italic, customX, customY, rotationDegrees);
                } else {
                    PhotoWatermarkApp.processImageGUI(file, outFile, fontSize, selectedColor, positionStr, format, jpegQuality, width, height, scale, watermarkText, fontName, opacity, shadow, stroke, bold, italic, rotationDegrees);
                }
            }
        } else {
            if (watermarkImageFile == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个图片水印！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String positionStr = (String) positionComboBox.getSelectedItem();
            double watermarkScale = imageWatermarkScaleSlider.getValue() / 100.0;
            int watermarkOpacity = imageWatermarkOpacitySlider.getValue();

            for (File file : importedFiles) {
                String baseName = file.getName();
                String nameNoExt = baseName.contains(".") ? baseName.substring(0, baseName.lastIndexOf('.')) : baseName;
                String ext = format.equals("PNG") ? ".png" : ".jpg";
                String outName = nameNoExt;
                // 根据命名规则添加前缀/后缀
                if ("添加前缀".equals(namingRule) || "添加前后缀".equals(namingRule)) {
                    outName = prefix + outName;
                }
                if ("添加后缀".equals(namingRule) || "添加前后缀".equals(namingRule)) {
                    outName = outName + suffix;
                }
                outName += ext;
                File outFile = new File(outputFolder, outName);
                if ("CUSTOM".equals(positionStr)) {
                    PhotoWatermarkApp.processImageWithImageWatermarkCustom(file, outFile, watermarkImageFile, format, jpegQuality, width, height, scale, watermarkScale, watermarkOpacity, customX, customY, rotationDegrees);
                } else {
                    PhotoWatermarkApp.processImageWithImageWatermark(file, outFile, watermarkImageFile, positionStr, format, jpegQuality, width, height, scale, watermarkScale, watermarkOpacity, rotationDegrees);
                }
            }
        }


        JOptionPane.showMessageDialog(this, "导出完成！");
    }
    
    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
    
    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
    
    private void exportImage(File src, File dest, String format, int jpegQuality, int width, int height, double scale) {
        // 方法未使用，暂时保留以备后续需求
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PhotoWatermarkGUI().setVisible(true));
    }
    
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".gif") || 
               name.endsWith(".bmp") || name.endsWith(".tif") || 
               name.endsWith(".tiff");
    }
}
