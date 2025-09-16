package com.example;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Base64;
import java.util.prefs.Preferences;

public class Main {
    private JFrame frame;
    private JComboBox<String> portComboBox;
    private JTextArea textArea;
    private SerialPort[] serialPorts;
    private JComboBox<String> baudRateComboBox;
    private JComboBox<String> fontSizeComboBox;
    private JLabel selectedFileLabel;
    private File selectedFile;
    private JComboBox<String> paperFeedComboBox;
    private JComboBox<String> ditheringComboBox;
    private JComboBox<String> barcodeTypeComboBox;
    private JTextField barcodeTextField;
    private JComboBox<String> templateComboBox;
    private JComboBox<String> languageComboBox;
    private JSlider brightnessSlider;
    private JSlider contrastSlider;
    private JCheckBox addTimestampCheckBox;
    private JCheckBox addBorderCheckBox;
    private JTextField headerTextField;
    private JTextField footerTextField;
    
    private Preferences prefs;
    private Map<String, String> templates;
    private int printCounter = 0;

    public static void main(String[] args) {
        try {
            System.loadLibrary("jSerialComm");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Native library not loaded, continuing...");
        }
        
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();
            main.createAndShowGUI();
            main.loadSettings(); // GUI oluşturulduktan sonra ayarları yükle
        });
    }

    private void createAndShowGUI() {
        prefs = Preferences.userNodeForPackage(Main.class);
        loadTemplates();
        // loadSettings() buradan kaldırıldı, main metodunda çağrılacak
        
        frame = new JFrame("X6 Termal Yazıcı Kontrolü - Professional Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);
        frame.setLayout(new BorderLayout());

        // Tabbed pane for organization
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Basic Settings
        tabbedPane.addTab("Temel Ayarlar", createBasicSettingsPanel());
        
        // Tab 2: Advanced Settings
        tabbedPane.addTab("Gelişmiş Ayarlar", createAdvancedSettingsPanel());
        
        // Tab 3: Barcode & Templates
        tabbedPane.addTab("Barkod & Şablonlar", createBarcodeTemplatePanel());
        
        // Tab 4: Image Settings
        tabbedPane.addTab("Görsel Ayarlar", createImageSettingsPanel());

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(createButtonPanel(), BorderLayout.SOUTH);

        refreshPorts();
        frame.setVisible(true);
    }

    private JPanel createBasicSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        
        JLabel titleLabel = new JLabel("X6 Bluetooth Termal Yazıcı - Professional Edition", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.add(new JLabel("Port:"));
        portComboBox = new JComboBox<>();
        portComboBox.setPreferredSize(new Dimension(300, 25));
        portPanel.add(portComboBox);
        
        JButton refreshButton = new JButton("Yenile");
        refreshButton.addActionListener(e -> refreshPorts());
        portPanel.add(refreshButton);
        
        JPanel baudRatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        baudRatePanel.add(new JLabel("Baud Rate:"));
        baudRateComboBox = new JComboBox<>(new String[]{
            "9600", "19200", "38400", "57600", "115200"
        });
        baudRateComboBox.setSelectedItem("9600");
        baudRatePanel.add(baudRateComboBox);
        
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        settingsPanel.add(new JLabel("Yazı Boyutu:"));
        fontSizeComboBox = new JComboBox<>(new String[]{
            "12", "14", "16", "18", "20", "24", "28"
        });
        fontSizeComboBox.setSelectedItem("16");
        settingsPanel.add(fontSizeComboBox);
        
        settingsPanel.add(new JLabel("Kağıt Boşluğu:"));
        paperFeedComboBox = new JComboBox<>(new String[]{
            "3 satır", "5 satır", "8 satır", "10 satır"
        });
        paperFeedComboBox.setSelectedItem("5 satır");
        settingsPanel.add(paperFeedComboBox);
        
        JPanel ditheringPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ditheringPanel.add(new JLabel("Dithering:"));
        ditheringComboBox = new JComboBox<>(new String[]{
            "Yok", "Basit", "Floyd-Steinberg", "Random"
        });
        ditheringComboBox.setSelectedItem("Floyd-Steinberg");
        ditheringPanel.add(ditheringComboBox);
        
        topPanel.add(titleLabel);
        topPanel.add(portPanel);
        topPanel.add(baudRatePanel);
        topPanel.add(settingsPanel);
        topPanel.add(ditheringPanel);

        // Text area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel textLabel = new JLabel("Yazdırılacak Metin:");
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(950, 200));
        
        // File selection panel
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectFileButton = new JButton("Resim/PDF Seç");
        selectFileButton.addActionListener(e -> selectFile());
        
        selectedFileLabel = new JLabel("Dosya seçilmedi");
        filePanel.add(selectFileButton);
        filePanel.add(selectedFileLabel);
        
        centerPanel.add(textLabel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(filePanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createAdvancedSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Language selection
        panel.add(new JLabel("Dil:"));
        languageComboBox = new JComboBox<>(new String[]{"Türkçe", "English"});
        panel.add(languageComboBox);
        
        // Header text
        panel.add(new JLabel("Üst Bilgi:"));
        headerTextField = new JTextField("=== X6 YAZICI ===");
        panel.add(headerTextField);
        
        // Footer text
        panel.add(new JLabel("Alt Bilgi:"));
        footerTextField = new JTextField("Teşekkür ederiz!");
        panel.add(footerTextField);
        
        // Timestamp
        panel.add(new JLabel("Zaman Damgası:"));
        addTimestampCheckBox = new JCheckBox("Ekle");
        addTimestampCheckBox.setSelected(true);
        panel.add(addTimestampCheckBox);
        
        // Border
        panel.add(new JLabel("Kenarlık:"));
        addBorderCheckBox = new JCheckBox("Ekle");
        panel.add(addBorderCheckBox);
        
        // Printer settings
        panel.add(new JLabel("Yazıcı Isısı:"));
        JComboBox<String> heatComboBox = new JComboBox<>(new String[]{"Düşük", "Orta", "Yüksek"});
        panel.add(heatComboBox);
        
        return panel;
    }

    private JPanel createBarcodeTemplatePanel() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Barcode type
        panel.add(new JLabel("Barkod Türü:"));
        barcodeTypeComboBox = new JComboBox<>(new String[]{
            "QR Code", "Code128", "Code39", "EAN-13", "UPC-A"
        });
        panel.add(barcodeTypeComboBox);
        
        // Barcode text
        panel.add(new JLabel("Barkod Metni:"));
        barcodeTextField = new JTextField("1234567890");
        panel.add(barcodeTextField);
        
        // Templates
        panel.add(new JLabel("Şablonlar:"));
        templateComboBox = new JComboBox<>(templates.keySet().toArray(new String[0]));
        panel.add(templateComboBox);
        
        // Template management buttons
        JPanel templateButtonPanel = new JPanel(new FlowLayout());
        JButton saveTemplateButton = new JButton("Şablon Kaydet");
        saveTemplateButton.addActionListener(e -> saveTemplate());
        JButton loadTemplateButton = new JButton("Şablon Yükle");
        loadTemplateButton.addActionListener(e -> loadTemplate());
        
        templateButtonPanel.add(saveTemplateButton);
        templateButtonPanel.add(loadTemplateButton);
        panel.add(new JLabel("Şablon İşlemleri:"));
        panel.add(templateButtonPanel);
        
        return panel;
    }

    private JPanel createImageSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Brightness
        panel.add(new JLabel("Parlaklık:"));
        brightnessSlider = new JSlider(0, 200, 100);
        panel.add(brightnessSlider);
        
        // Contrast
        panel.add(new JLabel("Kontrast:"));
        contrastSlider = new JSlider(0, 200, 100);
        panel.add(contrastSlider);
        
        // Image filters
        panel.add(new JLabel("Filtreler:"));
        JComboBox<String> filterComboBox = new JComboBox<>(new String[]{
            "Yok", "Gri Tonlama", "Negatif", "Sepia", "Kenar Belirleme"
        });
        panel.add(filterComboBox);
        
        // Preview button
        JButton previewButton = new JButton("Ön İzleme");
        previewButton.addActionListener(e -> showPreview());
        panel.add(new JLabel("Ön İzleme:"));
        panel.add(previewButton);
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton printButton = new JButton("Metin Yazdır");
        printButton.setFont(new Font("Arial", Font.BOLD, 14));
        printButton.addActionListener(e -> printText());
        
        JButton printFileButton = new JButton("Dosya Yazdır");
        printFileButton.addActionListener(e -> printFile());
        
        JButton printBarcodeButton = new JButton("Barkod Yazdır");
        printBarcodeButton.addActionListener(e -> printBarcode());
        
        JButton testButton = new JButton("Test Yazısı");
        testButton.addActionListener(e -> {
            textArea.setText("=== X6 YAZICI TEST ===\n" +
                    "Bu bir test yazısıdır.\n" +
                    "Termal yazıcı çalışıyor!\n" +
                    "-----------------------\n" +
                    "Java ile yazdırma başarılı!\n" +
                    "Türkçe karakter test: ıİğĞüÜşŞöÖçÇ");
        });
        
        JButton clearButton = new JButton("Temizle");
        clearButton.addActionListener(e -> {
            textArea.setText("");
            selectedFile = null;
            selectedFileLabel.setText("Dosya seçilmedi");
        });

        JButton settingsButton = new JButton("Ayarları Kaydet");
        settingsButton.addActionListener(e -> saveSettings());

        buttonPanel.add(printButton);
        buttonPanel.add(printFileButton);
        buttonPanel.add(printBarcodeButton);
        buttonPanel.add(testButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(settingsButton);

        return buttonPanel;
    }

    private void refreshPorts() {
        serialPorts = SerialPort.getCommPorts();
        portComboBox.removeAllItems();
        
        for (SerialPort port : serialPorts) {
            portComboBox.addItem(port.getSystemPortName() + " - " + port.getDescriptivePortName());
        }
        
        if (portComboBox.getItemCount() > 0) {
            portComboBox.setSelectedIndex(0);
        }
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Resim Dosyaları", "jpg", "jpeg", "png", "bmp", "gif"));
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Dosyaları", "pdf"));
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            selectedFileLabel.setText(selectedFile.getName());
        }
    }

    private void printText() {
        String text = textArea.getText();
        if (text.isEmpty()) {
            showAlert("Hata", "Lütfen yazdırılacak metni girin.");
            return;
        }

        int selectedIndex = portComboBox.getSelectedIndex();
        if (selectedIndex < 0) {
            showAlert("Hata", "Lütfen bir port seçin.");
            return;
        }

        int baudRate = Integer.parseInt((String) baudRateComboBox.getSelectedItem());
        SerialPort comPort = serialPorts[selectedIndex];
        comPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 0);

        if (comPort.openPort()) {
            try {
                OutputStream out = comPort.getOutputStream();
                int fontSize = Integer.parseInt((String) fontSizeComboBox.getSelectedItem());
                
                // Add header and footer
                String fullText = headerTextField.getText() + "\n\n" + text + "\n\n" + footerTextField.getText();
                if (addTimestampCheckBox.isSelected()) {
                    String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
                    fullText += "\n\nTarih: " + timestamp;
                }
                
                sendAsBitmap(out, fullText, fontSize);
                feedPaper(out);
                
                out.flush();
                Thread.sleep(500);
                
                showAlert("Bilgi", "Metin yazdırma komutları gönderildi.\nBaud Rate: " + baudRate);

            } catch (Exception ex) {
                showAlert("Hata", "Yazdırma hatası: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                comPort.closePort();
            }
        } else {
            showAlert("Hata", "Port açılamadı!");
        }
    }

    private void printFile() {
        if (selectedFile == null) {
            showAlert("Hata", "Lütfen yazdırılacak bir dosya seçin.");
            return;
        }

        int selectedIndex = portComboBox.getSelectedIndex();
        if (selectedIndex < 0) {
            showAlert("Hata", "Lütfen bir port seçin.");
            return;
        }

        int baudRate = Integer.parseInt((String) baudRateComboBox.getSelectedItem());
        SerialPort comPort = serialPorts[selectedIndex];
        comPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 0);

        if (comPort.openPort()) {
            try {
                OutputStream out = comPort.getOutputStream();
                
                String fileName = selectedFile.getName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    printPDFFile(out, selectedFile);
                } else {
                    printImageFile(out, selectedFile);
                }
                
                feedPaper(out);
                out.flush();
                Thread.sleep(500);
                
                showAlert("Bilgi", "Dosya yazdırma komutları gönderildi.\nBaud Rate: " + baudRate);

            } catch (Exception ex) {
                showAlert("Hata", "Dosya yazdırma hatası: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                comPort.closePort();
            }
        } else {
            showAlert("Hata", "Port açılamadı!");
        }
    }

    private void printPDFFile(OutputStream out, File pdfFile) throws Exception {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 150);
                String ditheringType = (String) ditheringComboBox.getSelectedItem();
                BufferedImage ditheredImage = applyDithering(image, ditheringType);
                
                int width = 384;
                int height = (int) (ditheredImage.getHeight() * ((double) width / ditheredImage.getWidth()));
                
                BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = scaledImage.createGraphics();
                g.drawImage(ditheredImage, 0, 0, width, height, null);
                g.dispose();
                
                int bytesPerLine = (width + 7) / 8;
                byte xL = (byte) (bytesPerLine & 0xFF);
                byte xH = (byte) ((bytesPerLine >> 8) & 0xFF);
                byte yL = (byte) (height & 0xFF);
                byte yH = (byte) ((height >> 8) & 0xFF);
                
                byte[] cmdHeader = new byte[] { 0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH };
                byte[] imageData = convertImageToRasterData(scaledImage, bytesPerLine);
                
                out.write(0x1B);
                out.write(0x40);
                out.write(cmdHeader);
                out.write(imageData);
                
                if (page < document.getNumberOfPages() - 1) {
                    feedPaper(out);
                }
            }
        } catch (IOException e) {
            throw new Exception("PDF işleme hatası: " + e.getMessage());
        }
    }

    private void printImageFile(OutputStream out, File imageFile) throws Exception {
        try {
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage == null) {
                throw new IOException("Resim yüklenemedi: " + imageFile.getName());
            }
            
            String ditheringType = (String) ditheringComboBox.getSelectedItem();
            BufferedImage ditheredImage = applyDithering(originalImage, ditheringType);
            
            int width = 384;
            int height = (int) (ditheredImage.getHeight() * ((double) width / ditheredImage.getWidth()));
            
            BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = scaledImage.createGraphics();
            g.drawImage(ditheredImage, 0, 0, width, height, null);
            g.dispose();
            
            int bytesPerLine = (width + 7) / 8;
            byte xL = (byte) (bytesPerLine & 0xFF);
            byte xH = (byte) ((bytesPerLine >> 8) & 0xFF);
            byte yL = (byte) (height & 0xFF);
            byte yH = (byte) ((height >> 8) & 0xFF);
            
            byte[] cmdHeader = new byte[] { 0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH };
            byte[] imageData = convertImageToRasterData(scaledImage, bytesPerLine);
            
            out.write(0x1B);
            out.write(0x40);
            out.write(cmdHeader);
            out.write(imageData);
            
        } catch (IOException e) {
            throw new Exception("Resim işleme hatası: " + e.getMessage());
        }
    }

    private BufferedImage applyDithering(BufferedImage image, String ditheringType) {
        if ("Yok".equals(ditheringType)) {
            BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = grayImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            
            BufferedImage binaryImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g2d = binaryImage.createGraphics();
            g2d.drawImage(grayImage, 0, 0, null);
            g2d.dispose();
            
            return binaryImage;
        }
        else if ("Basit".equals(ditheringType)) {
            return simpleDithering(image);
        }
        else if ("Floyd-Steinberg".equals(ditheringType)) {
            return floydSteinbergDithering(image);
        }
        else if ("Random".equals(ditheringType)) {
            return randomDithering(image);
        }
        else {
            return simpleDithering(image);
        }
    }

    private BufferedImage simpleDithering(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        
        for (int y = 0; y < grayImage.getHeight(); y++) {
            for (int x = 0; x < grayImage.getWidth(); x++) {
                int rgb = grayImage.getRGB(x, y);
                int gray = rgb & 0xFF;
                
                int threshold = 128;
                if ((x + y) % 2 == 0) {
                    threshold -= 20;
                } else {
                    threshold += 20;
                }
                
                int newColor = (gray > threshold) ? 0xFFFFFF : 0x000000;
                result.setRGB(x, y, newColor);
            }
        }
        
        return result;
    }

    private BufferedImage floydSteinbergDithering(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        int[][] pixels = new int[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = grayImage.getRGB(x, y);
                pixels[y][x] = rgb & 0xFF;
            }
        }
        
        for (int y = 0; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int oldPixel = pixels[y][x];
                int newPixel = oldPixel < 128 ? 0 : 255;
                pixels[y][x] = newPixel;
                
                int error = oldPixel - newPixel;
                
                if (x + 1 < width) pixels[y][x + 1] += error * 7 / 16;
                if (y + 1 < height) {
                    if (x - 1 >= 0) pixels[y + 1][x - 1] += error * 3 / 16;
                    pixels[y + 1][x] += error * 5 / 16;
                    if (x + 1 < width) pixels[y + 1][x + 1] += error * 1 / 16;
                }
            }
        }
        
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = pixels[y][x] < 128 ? 0x000000 : 0xFFFFFF;
                result.setRGB(x, y, value);
            }
        }
        
        return result;
    }

    private BufferedImage randomDithering(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Random random = new Random();
        
        for (int y = 0; y < grayImage.getHeight(); y++) {
            for (int x = 0; x < grayImage.getWidth(); x++) {
                int rgb = grayImage.getRGB(x, y);
                int gray = rgb & 0xFF;
                
                int threshold = 128 + (random.nextInt(65) - 32);
                
                int newColor = (gray > threshold) ? 0xFFFFFF : 0x000000;
                result.setRGB(x, y, newColor);
            }
        }
        
        return result;
    }

    private void sendAsBitmap(OutputStream out, String text, int fontSize) throws Exception {
        int width = 384;
        int height = calculateTextHeight(text, width, fontSize);
        
        BufferedImage image = textToImage(text, width, height, fontSize);
        
        int bytesPerLine = (width + 7) / 8;
        byte xL = (byte) (bytesPerLine & 0xFF);
        byte xH = (byte) ((bytesPerLine >> 8) & 0xFF);
        byte yL = (byte) (height & 0xFF);
        byte yH = (byte) ((height >> 8) & 0xFF);
        
        byte[] cmdHeader = new byte[] { 0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH };
        byte[] imageData = convertImageToRasterData(image, bytesPerLine);
        
        out.write(0x1B);
        out.write(0x40);
        out.write(cmdHeader);
        out.write(imageData);
    }

    private int calculateTextHeight(String text, int width, int fontSize) {
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tempImage.createGraphics();
        FontMetrics metrics = g.getFontMetrics(font);
        
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                String testLine = currentLine + (currentLine.length() > 0 ? " " : "") + word;
                int testWidth = metrics.stringWidth(testLine);
                
                if (testWidth > width - 20) {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        if (metrics.stringWidth(word) > width - 20) {
                            StringBuilder currentWord = new StringBuilder();
                            for (char c : word.toCharArray()) {
                                String testChar = currentWord.toString() + c;
                                if (metrics.stringWidth(testChar) > width - 20) {
                                    if (currentWord.length() > 0) {
                                        lines.add(currentWord.toString());
                                        currentWord = new StringBuilder();
                                    }
                                    currentWord.append(c);
                                } else {
                                    currentWord.append(c);
                                }
                            }
                            if (currentWord.length() > 0) {
                                lines.add(currentWord.toString());
                            }
                        } else {
                            lines.add(word);
                        }
                        currentLine = new StringBuilder();
                    }
                } else {
                    currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
                }
            }
            
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        g.dispose();
        
        int lineHeight = metrics.getHeight();
        int totalHeight = lines.size() * lineHeight + 20;
        
        return Math.max(100, totalHeight);
    }

    private BufferedImage textToImage(String text, int width, int height, int fontSize) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        g.setColor(Color.BLACK);
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
        g.setFont(font);
        
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();
        
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                String testLine = currentLine + (currentLine.length() > 0 ? " " : "") + word;
                int testWidth = metrics.stringWidth(testLine);
                
                if (testWidth > width - 20) {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        if (metrics.stringWidth(word) > width - 20) {
                            StringBuilder currentWord = new StringBuilder();
                            for (char c : word.toCharArray()) {
                                String testChar = currentWord.toString() + c;
                                if (metrics.stringWidth(testChar) > width - 20) {
                                    if (currentWord.length() > 0) {
                                        lines.add(currentWord.toString());
                                        currentWord = new StringBuilder();
                                    }
                                    currentWord.append(c);
                                } else {
                                    currentWord.append(c);
                                }
                            }
                            if (currentWord.length() > 0) {
                                lines.add(currentWord.toString());
                            }
                        } else {
                            lines.add(word);
                        }
                        currentLine = new StringBuilder();
                    }
                } else {
                    currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
                }
            }
            
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        int y = metrics.getAscent() + 10;
        for (String line : lines) {
            g.drawString(line, 10, y);
            y += lineHeight;
        }
        
        if (addBorderCheckBox.isSelected()) {
            g.drawRect(5, 5, width - 10, height - 10);
        }
        
        g.dispose();
        return image;
    }

    private byte[] convertImageToRasterData(BufferedImage image, int bytesPerLine) {
        int height = image.getHeight();
        byte[] data = new byte[bytesPerLine * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < bytesPerLine; x++) {
                byte b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int px = x * 8 + bit;
                    if (px < image.getWidth() && y < image.getHeight()) {
                        if (image.getRGB(px, y) == Color.BLACK.getRGB()) {
                            b |= (1 << (7 - bit));
                        }
                    }
                }
                data[y * bytesPerLine + x] = b;
            }
        }
        
        return data;
    }

    private void feedPaper(OutputStream out) throws IOException {
        int feedLines = 5;
        
        String selectedFeed = (String) paperFeedComboBox.getSelectedItem();
        if (selectedFeed != null) {
            if (selectedFeed.startsWith("3")) feedLines = 3;
            else if (selectedFeed.startsWith("5")) feedLines = 5;
            else if (selectedFeed.startsWith("8")) feedLines = 8;
            else if (selectedFeed.startsWith("10")) feedLines = 10;
        }
        
        for (int i = 0; i < feedLines; i++) {
            out.write(0x0A);
        }
    }

    private void printBarcode() {
        String barcodeText = barcodeTextField.getText();
        String barcodeType = (String) barcodeTypeComboBox.getSelectedItem();
        
        if (barcodeText.isEmpty()) {
            showAlert("Hata", "Lütfen barkod metnini girin.");
            return;
        }

        int selectedIndex = portComboBox.getSelectedIndex();
        if (selectedIndex < 0) {
            showAlert("Hata", "Lütfen bir port seçin.");
            return;
        }

        int baudRate = Integer.parseInt((String) baudRateComboBox.getSelectedItem());
        SerialPort comPort = serialPorts[selectedIndex];
        comPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 0);

        if (comPort.openPort()) {
            try {
                OutputStream out = comPort.getOutputStream();
                generateBarcode(out, barcodeText, barcodeType);
                feedPaper(out);
                
                out.flush();
                Thread.sleep(500);
                
                showAlert("Bilgi", "Barkod yazdırma komutları gönderildi.");

            } catch (Exception ex) {
                showAlert("Hata", "Barkod yazdırma hatası: " + ex.getMessage());
            } finally {
                comPort.closePort();
            }
        } else {
            showAlert("Hata", "Port açılamadı!");
        }
    }

    private void generateBarcode(OutputStream out, String text, String type) throws IOException {
        if ("QR Code".equals(type)) {
            BufferedImage qrImage = createQRCode(text);
            printImage(out, qrImage);
        } else {
            out.write(0x1B);
            out.write(0x40);
            out.write(0x1D);
            out.write(0x6B);
            
            if ("Code128".equals(type)) {
                out.write(0x49);
            } else if ("Code39".equals(type)) {
                out.write(0x04);
            } else {
                out.write(0x04);
            }
            
            out.write((byte) text.length());
            out.write(text.getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private BufferedImage createQRCode(String text) {
        int size = 150;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        
        g.drawRect(10, 10, size-20, size-20);
        g.drawString("QR: " + text.substring(0, Math.min(15, text.length())), 20, size/2);
        
        g.dispose();
        return image;
    }

    private void printImage(OutputStream out, BufferedImage image) throws IOException {
        int width = 384;
        int height = (int) (image.getHeight() * ((double) width / image.getWidth()));
        
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = scaledImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        
        int bytesPerLine = (width + 7) / 8;
        byte xL = (byte) (bytesPerLine & 0xFF);
        byte xH = (byte) ((bytesPerLine >> 8) & 0xFF);
        byte yL = (byte) (height & 0xFF);
        byte yH = (byte) ((height >> 8) & 0xFF);
        
        byte[] cmdHeader = new byte[] { 0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH };
        byte[] imageData = convertImageToRasterData(scaledImage, bytesPerLine);
        
        out.write(0x1B);
        out.write(0x40);
        out.write(cmdHeader);
        out.write(imageData);
    }

    private void saveTemplate() {
        String name = JOptionPane.showInputDialog(frame, "Şablon adı:");
        if (name != null && !name.trim().isEmpty()) {
            templates.put(name, textArea.getText());
            templateComboBox.addItem(name);
            showAlert("Başarılı", "Şablon kaydedildi: " + name);
        }
    }

    private void loadTemplate() {
        String selected = (String) templateComboBox.getSelectedItem();
        if (selected != null && templates.containsKey(selected)) {
            textArea.setText(templates.get(selected));
        }
    }

    private void loadTemplates() {
        templates = new HashMap<>();
        templates.put("Fatura", "=== FATURA ===\nÜrün\t\tAdet\tFiyat\n----------------------------\nToplam:\t\t\t0.00 TL");
        templates.put("Fiş", "=== FİŞ ===\nTarih: {date}\n----------------------------");
        templates.put("Etiket", "Ürün: {product}\nSKU: {sku}\nFiyat: {price} TL");
    }

    private void saveSettings() {
        if (baudRateComboBox != null) {
            prefs.put("baudRate", (String) baudRateComboBox.getSelectedItem());
        }
        if (fontSizeComboBox != null) {
            prefs.put("fontSize", (String) fontSizeComboBox.getSelectedItem());
        }
        if (ditheringComboBox != null) {
            prefs.put("dithering", (String) ditheringComboBox.getSelectedItem());
        }
        showAlert("Başarılı", "Ayarlar kaydedildi!");
    }

    private void loadSettings() {
        if (baudRateComboBox != null) {
            baudRateComboBox.setSelectedItem(prefs.get("baudRate", "9600"));
        }
        if (fontSizeComboBox != null) {
            fontSizeComboBox.setSelectedItem(prefs.get("fontSize", "16"));
        }
        if (ditheringComboBox != null) {
            ditheringComboBox.setSelectedItem(prefs.get("dithering", "Floyd-Steinberg"));
        }
    }

    private void showPreview() {
        if (selectedFile != null) {
            try {
                BufferedImage image = ImageIO.read(selectedFile);
                if (image != null) {
                    JFrame previewFrame = new JFrame("Ön İzleme");
                    previewFrame.add(new JLabel(new ImageIcon(image)));
                    previewFrame.pack();
                    previewFrame.setVisible(true);
                }
            } catch (IOException e) {
                showAlert("Hata", "Ön izleme hatası: " + e.getMessage());
            }
        } else {
            showAlert("Bilgi", "Ön izleme için lütfen önce bir dosya seçin.");
        }
    }

    private void showAlert(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}