package com.scryer.endpoint.service.imageresize;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
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
				BufferedImage original = ImageIO.read(new ByteArrayInputStream(image));
				var height = original.getHeight();
				var width = original.getWidth();
				double ratio;
				BufferedImage resized;
				if (width < height) {
					ratio = (float) maxDimension / height;
					var targetWidth = Double.valueOf(width * ratio).intValue();
					resized = new BufferedImage(targetWidth, maxDimension, original.getType());
					Graphics2D graphics2D = resized.createGraphics();
					graphics2D.drawImage(original, 0, 0, targetWidth, maxDimension, null);
					graphics2D.dispose();
				}
				else {
					ratio = (float) maxDimension / width;
					var targetHeight = Double.valueOf(height * ratio).intValue();
					resized = new BufferedImage(maxDimension, targetHeight, original.getType());
					Graphics2D graphics2D = resized.createGraphics();
					graphics2D.drawImage(original, 0, 0, maxDimension, targetHeight, null);
					graphics2D.dispose();
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(resized, request.type()[2], out);
				return Mono.just(out.toByteArray());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return Mono.empty();
		});
	}

}
