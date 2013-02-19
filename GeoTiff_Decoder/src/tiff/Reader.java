package tiff;

import java.io.File;

/**
 */
public class Reader {
    /**
     * @param arg 
     * @throws Exception 
     */
    public static void main(String...arg) throws Exception{
    	File source = new File("sample.tif");
    	File target =new File("sample.tif");
    	ImageProcessor impro=new ImageProcessor();
    	BufferedTiffImage bufferedtif = impro.read( source );    	
    	impro.write(bufferedtif, target );
    }
}

