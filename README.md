# PhotoWatermark

PhotoWatermark is a powerful and easy-to-use tool for adding watermarks to your photos. It supports both text and image watermarks, batch processing, and EXIF metadata extraction. The project provides a user-friendly GUI and command-line interface for flexible usage.

## Features
- **Text Watermark:** Add customizable text watermarks with font, color, size (default now 150), opacity, position, shadow, stroke, and rotation options.
- **Image Watermark:** Overlay an image watermark with adjustable scale, opacity, position, and rotation.
- **Rotation Support:** (New) Arbitrary angle rotation (-180° to 180°) for both text and image watermarks via a rotation slider.
- **Batch Processing:** Import multiple images and export watermarked results in bulk.
- **EXIF Support:** Automatically extract and use photo metadata (e.g., shooting date) as watermark text.
- **Format Conversion:** Export images as JPEG or PNG with quality settings.
- **Custom Naming Rules:** Add prefixes/suffixes to output filenames.
- **Graphical User Interface:** Intuitive GUI for easy operation with real-time asynchronous preview (optimized for performance).
- **Command-Line Support:** CLI for advanced and automated workflows.

## Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/SEBugMaker/PhotoWatermark.git
   ```
2. Install dependencies (metadata-extractor, xmpcore) in `lib/`.
3. Build the project using Maven:
   ```sh
   mvn clean package
   ```
4. Run the application:
   - GUI:
     ```sh
     java -jar PhotoWatermark.jar
     ```
   - Command-line:
     ```sh
     java -cp target/classes PhotoWatermarkApp
     ```

## Usage
### GUI
- Import images via drag-and-drop or file chooser.
- Choose watermark mode: Text or Image.
- (New) Adjust Rotation using the rotation slider (-180° to 180°) for both text and image watermarks.
- Default text watermark font size is now 150 (you can change it in the input box).
- For custom placement, choose position `CUSTOM` and drag the watermark directly in the preview pane.
- Configure scale / width / height (scale takes precedence if > 0 and not 100%).
- Export with selected format (JPEG/PNG) and naming rules.

### Command-Line
- Run the app and follow prompts to set watermark options and process images.
- (Rotation currently available in GUI workflows; CLI rotation can be added similarly if needed.)

## Dependencies
- Java 8+
- [metadata-extractor](https://github.com/drewnoakes/metadata-extractor)
- [xmpcore](https://github.com/adobe/XMP-Toolkit-SDK)

## Contributing
Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request. For major changes, open an issue first to discuss your ideas.

## License
This project is licensed under the MIT License.
