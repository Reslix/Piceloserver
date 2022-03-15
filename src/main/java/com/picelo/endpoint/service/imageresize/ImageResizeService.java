package com.picelo.endpoint.service.imageresize;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageResizeService {

    private final Scheduler scheduler;

    public ImageResizeService() {
        this.scheduler = Schedulers.boundedElastic();
    }

    public Mono<byte[]> getResizedImage(final ImageResizeRequest request) {
        var maxDimension = request.maxDimension();
        return Mono.just(request.imageArray()).subscribeOn(scheduler).flatMap(image -> {
            try {
                ImageReader reader = ImageIO.getImageReadersBySuffix(request.type()[2]).next();
                reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(image)));
                IIOMetadata metadata = reader.getImageMetadata(0);
                BufferedImage original = reader.read(0);
                var height = original.getHeight();
                var width = original.getWidth();
                double ratio;
                BufferedImage resized;
                if (width < height) {
                    ratio = (float) maxDimension / height;
                    var targetWidth = Double.valueOf(width * ratio).intValue();
                    resized = new BufferedImage(targetWidth, maxDimension, original.getType());
                    Graphics2D graphics2D = resized.createGraphics();
                    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics2D.drawImage(original, 0, 0, targetWidth, maxDimension, null);
                    graphics2D.dispose();
                } else {
                    ratio = (float) maxDimension / width;
                    var targetHeight = Double.valueOf(height * ratio).intValue();
                    resized = new BufferedImage(maxDimension, targetHeight, original.getType());
                    Graphics2D graphics2D = resized.createGraphics();
                    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics2D.drawImage(original, 0, 0, maxDimension, targetHeight, null);
                    graphics2D.dispose();
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(out);
                ImageWriter writer = ImageIO.getImageWritersBySuffix(request.type()[2]).next();
                writer.setOutput(imageOutputStream);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
                if (param instanceof JPEGImageWriteParam) {
                    ((JPEGImageWriteParam) param).setOptimizeHuffmanTables(true);
                }
                writer.write(null, new IIOImage(resized, null, metadata), param);
                writer.dispose();
                return Mono.just(out.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Mono.empty();
        });
    }

}
