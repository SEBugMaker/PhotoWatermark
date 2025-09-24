import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

public class PhotoWatermarkGUI extends JFrame {
    private DefaultListModel<ImageIcon> imageListModel;
    private JList<ImageIcon> imageList;
    private List<File> importedFiles;
    private JComboBox<String> formatComboBox;

    private JTextField outputFolderField;
    private JButton chooseOutputFolderButton;
    private JComboBox<String> namingRuleComboBox;
    private JTextField prefixField;
    private JTextField suffixField;
    private JSlider jpegQualitySlider;
    private JTextField widthField;
    private JTextField heightField;
    private JTextField scaleField;
    private JButton exportButton;
    private File outputFolder;
    private JTextField fontSizeField;
    private JComboBox<String> colorComboBox;
    private JComboBox<String> positionComboBox;

    public PhotoWatermarkGUI() {
        setTitle("图片水印工具");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        importedFiles = new ArrayList<>();
        imageListModel = new DefaultListModel<>();
        imageList = new JList<>(imageListModel);
        imageList.setCellRenderer(new ImageListCellRenderer());
        JScrollPane scrollPane = new JScrollPane(imageList);
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton importButton = new JButton("导入图片");
        importButton.addActionListener(e -> openFileChooser());
        topPanel.add(importButton);
        formatComboBox = new JComboBox<>(new String[]{"JPEG", "PNG"});
        topPanel.add(new JLabel("输出格式:"));
        topPanel.add(formatComboBox);
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
                    ex.printStackTrace();
                }
            }
        }, true);

        JPanel exportPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        outputFolderField = new JTextField();
        outputFolderField.setEditable(false);
        chooseOutputFolderButton = new JButton("选择输出文件夹");
        chooseOutputFolderButton.addActionListener(e -> chooseOutputFolder());
        exportPanel.add(new JLabel("输出文件夹:"));
        exportPanel.add(outputFolderField);
        exportPanel.add(new JLabel(""));
        exportPanel.add(chooseOutputFolderButton);
        namingRuleComboBox = new JComboBox<>(new String[]{"保留原文件名", "添加前缀", "添加后缀"});
        exportPanel.add(new JLabel("命名规则:"));
        exportPanel.add(namingRuleComboBox);
        prefixField = new JTextField("wm_");
        suffixField = new JTextField("_watermarked");
        exportPanel.add(new JLabel("前缀:"));
        exportPanel.add(prefixField);
        exportPanel.add(new JLabel("后缀:"));
        exportPanel.add(suffixField);
        jpegQualitySlider = new JSlider(0, 100, 80);
        jpegQualitySlider.setMajorTickSpacing(20);
        jpegQualitySlider.setPaintTicks(true);
        jpegQualitySlider.setPaintLabels(true);
        exportPanel.add(new JLabel("JPEG质量:"));
        exportPanel.add(jpegQualitySlider);
        widthField = new JTextField();
        heightField = new JTextField();
        scaleField = new JTextField();
        exportPanel.add(new JLabel("宽度(像素):"));
        exportPanel.add(widthField);
        exportPanel.add(new JLabel("高度(像素):"));
        exportPanel.add(heightField);
        exportPanel.add(new JLabel("缩放比例(%):"));
        exportPanel.add(scaleField);
        fontSizeField = new JTextField("36");
        colorComboBox = new JComboBox<>(new String[]{"WHITE", "BLACK", "RED", "GREEN", "BLUE", "YELLOW"});
        positionComboBox = new JComboBox<>(new String[]{
            "TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
            "MIDDLE_LEFT", "CENTER", "MIDDLE_RIGHT",
            "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"
        });
        exportPanel.add(new JLabel("水印字体大小:"));
        exportPanel.add(fontSizeField);
        exportPanel.add(new JLabel("水印颜色:"));
        exportPanel.add(colorComboBox);
        exportPanel.add(new JLabel("水印位置:"));
        exportPanel.add(positionComboBox);
        exportButton = new JButton("导出图片");
        exportButton.addActionListener(e -> exportImages());
        exportPanel.add(new JLabel(""));
        exportPanel.add(exportButton);
        add(exportPanel, BorderLayout.SOUTH);
    }

    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            handleImportedFiles(List.of(files));
        }
    }

    private void handleImportedFiles(List<File> files) {
        for (File file : files) {
            if (file.isDirectory()) {
                File[] imgs = file.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".bmp");
                });
                if (imgs != null) {
                    for (File img : imgs) addImage(img);
                }
            } else {
                addImage(file);
            }
        }
    }

    private void addImage(File file) {
        if (!importedFiles.contains(file)) {
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
            Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            ImageIcon thumb = new ImageIcon(img);
            thumb.setDescription(file.getName());
            imageListModel.addElement(thumb);
            importedFiles.add(file);
        }
    }

    // 自定义渲染器，显示缩略图和文件名
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
            if (namingRule.equals("添加前缀")) outName = prefix + outName;
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
        try {
            BufferedImage img = javax.imageio.ImageIO.read(src);
            int newW = img.getWidth(), newH = img.getHeight();
            if (scale > 0) {
                newW = (int)(img.getWidth() * scale);
                newH = (int)(img.getHeight() * scale);
            } else {
                if (width > 0) newW = width;
                if (height > 0) newH = height;
            }
            if (newW != img.getWidth() || newH != img.getHeight()) {
                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                BufferedImage scaledImg = new BufferedImage(newW, newH, img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType());
                Graphics2D g2d = scaledImg.createGraphics();
                g2d.drawImage(scaled, 0, 0, null);
                g2d.dispose();
                img = scaledImg;
            }
            if (format.equals("JPEG")) {
                javax.imageio.ImageWriteParam param = new javax.imageio.plugins.jpeg.JPEGImageWriteParam(null);
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(jpegQuality / 100f);
                javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(dest);
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
                ios.close();
                writer.dispose();
            } else {
                javax.imageio.ImageIO.write(img, format, dest);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PhotoWatermarkGUI().setVisible(true));
    }
}
