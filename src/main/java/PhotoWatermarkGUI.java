import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhotoWatermarkGUI extends JFrame {
    private static final int THUMB_WIDTH = 80;
    private static final int THUMB_HEIGHT = 80;
    
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
    private JTextField scaleField;
    private JComboBox<String> colorComboBox;
    private JComboBox<String> positionComboBox;
    private JTextField fontSizeField;
    private JCheckBox showAdvancedBox = new JCheckBox("显示高级选项");
    private JPanel advancedPanel;
    private JProgressBar progressBar;
    private JLabel statusLabel;

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
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    handleImportedFiles(files);
                } catch (Exception ex) {
                    Logger.getLogger(PhotoWatermarkGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, true);

        // 右侧参数区
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(400, 700));
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
        colorComboBox = new JComboBox<>(new String[]{"WHITE", "BLACK", "RED", "GREEN", "BLUE", "YELLOW"});
        gridPanel.add(colorComboBox, gbc);
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
        gbc.gridwidth = 1;
        
        // 为命名规则添加监听器
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
        
        // 高级选项
        advancedPanel = new JPanel(new GridBagLayout());
        advancedPanel.setBackground(Color.WHITE);
        GridBagConstraints advGbc = new GridBagConstraints();
        advGbc.insets = new Insets(6, 6, 6, 6);
        advGbc.fill = GridBagConstraints.HORIZONTAL;
        advGbc.gridx = 0; advGbc.gridy = 0;
        advancedPanel.add(new JLabel("JPEG质量:"), advGbc);
        advGbc.gridx = 1;
        jpegQualitySlider = new JSlider(0, 100, 80);
        jpegQualitySlider.setMajorTickSpacing(20);
        jpegQualitySlider.setPaintTicks(true);
        jpegQualitySlider.setPaintLabels(true);
        advancedPanel.add(jpegQualitySlider, advGbc);
        advGbc.gridx = 0; advGbc.gridy++;
        advancedPanel.add(new JLabel("宽度(像素):"), advGbc);
        advGbc.gridx = 1;
        widthField = new JTextField();
        widthField.setPreferredSize(new Dimension(120, 28));
        advancedPanel.add(widthField, advGbc);
        advGbc.gridx = 0; advGbc.gridy++;
        advancedPanel.add(new JLabel("高度(像素):"), advGbc);
        advGbc.gridx = 1;
        heightField = new JTextField();
        heightField.setPreferredSize(new Dimension(120, 28));
        advancedPanel.add(heightField, advGbc);
        advGbc.gridx = 0; advGbc.gridy++;
        advancedPanel.add(new JLabel("缩放比例(%):"), advGbc);
        advGbc.gridx = 1;
        scaleField = new JTextField();
        scaleField.setPreferredSize(new Dimension(120, 28));
        advancedPanel.add(scaleField, advGbc);
        advancedPanel.setVisible(false);
        showAdvancedBox.addActionListener(e -> advancedPanel.setVisible(showAdvancedBox.isSelected()));
        
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        showAdvancedBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        showAdvancedBox.setBackground(Color.WHITE);
        gridPanel.add(showAdvancedBox, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 3;
        gridPanel.add(advancedPanel, gbc);
        gbc.gridwidth = 1;
        
        JPanel paramPanel = new JPanel();
        paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
        paramPanel.setBackground(Color.WHITE);
        paramPanel.add(gridPanel);
        JScrollPane paramScrollPane = new JScrollPane(paramPanel);
        paramScrollPane.setPreferredSize(new Dimension(380, 500));
        paramScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightPanel.add(paramScrollPane, BorderLayout.CENTER);

        // 导出按钮面板
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        exportPanel.setBackground(Color.WHITE);
        JButton exportButton = new JButton("导出图片");
        exportButton.setBackground(new Color(0, 123, 255));
        exportButton.setForeground(Color.WHITE);
        exportButton.setFocusPainted(false);
        exportButton.setFont(exportButton.getFont().deriveFont(Font.BOLD, 14));
        exportButton.setPreferredSize(new Dimension(120, 36));
        exportButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 86, 179), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        exportButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportButton.addActionListener(e -> exportImages());
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
        List<ImageIcon> selectedValues = imageList.getSelectedValuesList();
        for (ImageIcon icon : selectedValues) {
            imageListModel.removeElement(icon);
            // 同时从导入文件列表中移除
            String fileName = icon.getDescription();
            importedFiles.removeIf(file -> file.getName().equals(fileName));
        }
        updateStatusLabel();
    }
    
    // 清空所有图片
    private void clearAllImages() {
        imageListModel.clear();
        importedFiles.clear();
        updateStatusLabel();
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

    private void chooseOutputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            outputFolder = chooser.getSelectedFile();
            outputFolderField.setText(outputFolder.getAbsolutePath());
        }
    }

    private void exportImages() {
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
        double scale = parseDouble(scaleField.getText()) / 100.0;
        int fontSize = parseInt(fontSizeField.getText());
        String colorStr = (String) colorComboBox.getSelectedItem();
        String positionStr = (String) positionComboBox.getSelectedItem();
        for (File file : importedFiles) {
            String baseName = file.getName();
            String nameNoExt = baseName.contains(".") ? baseName.substring(0, baseName.lastIndexOf('.')) : baseName;
            String ext = format.equals("PNG") ? ".png" : ".jpg";
            String outName = nameNoExt;
            if (namingRule != null && namingRule.equals("添加前缀")) outName = prefix + outName;
            if (namingRule.equals("添加后缀")) outName = outName + suffix;
            outName += ext;
            File outFile = new File(outputFolder, outName);
            PhotoWatermarkApp.processImageGUI(file, outFile, fontSize, colorStr, positionStr, format, jpegQuality, width, height, scale);
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