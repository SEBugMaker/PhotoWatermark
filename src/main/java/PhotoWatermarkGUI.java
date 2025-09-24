import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    // Fields for image watermark
    private File watermarkImageFile;
    private JLabel watermarkImagePathLabel;
    private JSlider imageWatermarkOpacitySlider;
    private JSlider imageWatermarkScaleSlider;

    public PhotoWatermarkGUI() {
        setTitle("照片水印工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(900, 700));

        imageListModel = new DefaultListModel<>();
        imageList = new JList<>(imageListModel);
        imageList.setCellRenderer(new ImageListCellRenderer());
        imageList.setFixedCellWidth(THUMB_WIDTH + 20);
        imageList.setFixedCellHeight(THUMB_HEIGHT + 30);
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = imageList.getSelectedIndex();
                if (selectedIndex != -1 && selectedIndex < importedFiles.size()) {
                    File selectedFile = importedFiles.get(selectedIndex);
                    try {
                        BufferedImage img = ImageIO.read(selectedFile);
                        selectedImageSizeLabel.setText(String.format("选中尺寸: %d x %d", img.getWidth(), img.getHeight()));
                    } catch (IOException ex) {
                        selectedImageSizeLabel.setText("选中尺寸: N/A");
                        Logger.getLogger(PhotoWatermarkGUI.class.getName()).log(Level.SEVERE, "Error reading image dimensions", ex);
                    }
                } else {
                    selectedImageSizeLabel.setText("选中尺寸: N/A");
                }
            }
        });

        // 只保留图片列表，移除图片展示区
        JPanel listPanel = createImageListPanel();
        add(listPanel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton importButton = new JButton("导入图片");
        topPanel.add(importButton);
        importButton.addActionListener(e -> openFileChooser());
        
        // 添加状态标签
        statusLabel = new JLabel("已导入: 0 张图片");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        topPanel.add(statusLabel);
        
        add(topPanel, BorderLayout.NORTH);

        // 拖拽支持
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
                } catch (Exception ex) {
                    Logger.getLogger(PhotoWatermarkGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, true);

        // 右侧参数区
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(500, 700));
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
        outputFolderField = new JTextField();
        outputFolderField.setEditable(false);
        outputFolderField.setPreferredSize(new Dimension(180, 28));
        gridPanel.add(outputFolderField, gbc);
        gbc.gridx = 2;
        JButton chooseOutputFolderButton = new JButton("选择");
        gridPanel.add(chooseOutputFolderButton, gbc);
        chooseOutputFolderButton.addActionListener(e -> chooseOutputFolder());
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
        fontSizeField = new JTextField("36");
        fontSizeField.setPreferredSize(new Dimension(120, 28));
        gridPanel.add(fontSizeField, gbc);
        gbc.gridx = 2;
        gridPanel.add(new JLabel(""), gbc);
        gbc.gridx = 0; gbc.gridy++;
        gridPanel.add(new JLabel("水印颜色:"), gbc);
        gbc.gridx = 1;
        JPanel colorPanel = new JPanel() {
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
            "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"
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
        JComboBox<String> watermarkModeComboBox = new JComboBox<>(new String[]{"文字水印", "图片水印"});
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

        // 导入进度条
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        rightPanel.add(progressBar, BorderLayout.NORTH);

        add(rightPanel, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }
    
    // 创建图片列表面板
    private JPanel createImageListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("已导入图片列表"));
        listPanel.setPreferredSize(new Dimension(350, 200));
        
        JScrollPane listScrollPane = new JScrollPane(imageList);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        
        // 添加删除按钮
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
    }

    private void addImage(File file) {
        try {
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
            Image img = icon.getImage().getScaledInstance(THUMB_WIDTH, THUMB_HEIGHT, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
            icon.setDescription(file.getName());
            imageListModel.addElement(icon);
            updateStatusLabel();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "无法加载图片: " + file.getName(), "错误", JOptionPane.ERROR_MESSAGE);
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

        // 根据水印模式选择分支
        boolean isTextWatermark = true;
        JComboBox<String> watermarkModeComboBox = null;
        for (Component comp : ((Container) namingRuleComboBox.getParent()).getComponents()) {
            if (comp instanceof JComboBox && ((JComboBox<?>) comp).getItemCount() == 2) {
                watermarkModeComboBox = (JComboBox<String>) comp;
                break;
            }
        }
        if (watermarkModeComboBox != null) {
            isTextWatermark = watermarkModeComboBox.getSelectedIndex() == 0;
        }

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
                PhotoWatermarkApp.processImageGUI(file, outFile, fontSize, selectedColor, positionStr, format, jpegQuality, width, height, scale, watermarkText, fontName, opacity, shadow, stroke, bold, italic);
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
                PhotoWatermarkApp.processImageWithImageWatermark(file, outFile, watermarkImageFile, positionStr, format, jpegQuality, width, height, scale, watermarkScale, watermarkOpacity);
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
