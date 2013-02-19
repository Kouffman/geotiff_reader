package tiff;

import java.io.File;

public class ImageProcessor {
	public BufferedTiffImage read(File tiff_image) throws Exception{
		return new BufferedTiffImage(tiff_image);
	}
	public void write(BufferedTiffImage image,File tiff_image) throws Exception{
		image.writeToFile(tiff_image);
	}
}
