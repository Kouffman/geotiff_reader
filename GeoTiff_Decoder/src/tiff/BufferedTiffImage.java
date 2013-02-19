package tiff;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

public class BufferedTiffImage {
	/*
	 * This class can handle only single page tiff image i.e tiff images having only IFD*/
	ImageFileHeader IFH;
	ImageFileDirectory IFD;
	int[][] pixel;
	
	@SuppressWarnings("static-access")
	BufferedTiffImage(File image) throws Exception{
    	RandomAccessFile fstream = new RandomAccessFile(image,"r");
    	IFH = ImageFileHeader.parseIFH(fstream);	
    	IFD = ImageFileDirectory.parseIFD(fstream, IFH.getNext_IFD_offset(),IFH.getByte_order());
    	this.decodePixels(new RandomAccessFile(image,"r"));
    	fstream.close();
	}
	
	@SuppressWarnings("static-access")
	public void writeToFile(File image) throws Exception{
		RandomAccessFile fstream = new RandomAccessFile( image, "rw");
		
		this.encodePixels(fstream);
		
		fstream = new RandomAccessFile( image, "rw");
//		System.out.println("File Pointer "+fstream.getFilePointer());
		long fp = 0;
		fp = Types.getWordBoundry(8);
		
		IFH.setNext_IFD_offset(fp);
		fp = 0;
		fstream.write(IFH.getAsBytes());
		
		fstream.seek(IFH.getNext_IFD_offset());
	
		IFD.setNext_IFD_offset(0);
		
		fstream.write(IFD.getAsBytes());
		
		Integer[] tags = new Integer[1];
		tags = IFD.fields.keySet().toArray(tags);
		Arrays.sort(tags);
		ImageFileField IFF = null;
		byte[] result = null;
		int temp = 0;
		fp = fstream.getFilePointer();
		int index=0;
		
		for(int tag : tags){
			
			
			IFF = IFD.fields.get(tag);
			
			
			temp = (int) IFH.getNext_IFD_offset()+2;
			temp +=index*12+8;
			fstream.seek(temp);
			
			if(IFF.getCount() * Types.DATATYPE[IFF.getDatatype()] <= 4){
//				System.out.println("Tag : "+IFF.tag+" Value : "+IFF.getValue_offset());
				result = Types.getLongAsBytes(IFF.getValue_offset(), IFH.getByte_order());
				fstream.write(result);		
			 }else{
//				 System.out.println("Tag : "+IFF.tag+" OffSet : "+IFF.getValue_offset());
				 result = Types.getObjectAsBytes(IFF, IFH.getByte_order());
				 fp = Types.getWordBoundry(fp);
				 fstream.write( Types.getLongAsBytes(fp,IFH.getByte_order()) );
				 fstream.seek(fp);
				 fstream.write(result);
				 fp = fstream.getFilePointer();
			}
			index++;
		}	
		fstream.close();
		
	}
	@SuppressWarnings("rawtypes")
	/** decodePixels() method is used to read the pixels stored in tiff image
	 * and store them into 2D java Array or ArrayList.
	 * */
	public void decodePixels(RandomAccessFile fstream) throws Exception{
		
		ArrayList offsets  = this.getStripOffsets();//get List of offsets  of strips in image.
		ArrayList counts   = this.getStripByteCounts();//now get how much bytes are there in each strip.(List of byte counts)
		
		int buffersize = (int)this.getBitsPerSample()/8; //how much is size of buffer needed for one pixel
		
		int row = 0, col = 0;
		
		if(buffersize == 0) buffersize = 1; //buffersize is minimum one byte
		
		long rowsperstrip =	this.getRowsPerStrip();//how many rows are there per strip
		
//		System.out.println("Rows Per Strip : "+rowsperstrip);
//		System.out.println("Size of Offsets : "+offsets.size());
//		System.out.println("ImageLength : "+this.getImageLength());
//		System.out.println("ImageWidth : "+this.getImageWidth());
		
		pixel = new int[(int) this.getImageWidth()][(int) this.getImageLength()]; //allocate the 2d pixel array
		
		for(int offset = 0; offset < offsets.size(); offset++){
			System.out.println("Offset : "+offsets.get(offset));
			fstream.seek( (long) offsets.get(offset));//move file pointer to address of current strip
			byte[] buffer = new byte[buffersize];//create buffer
			long count_max = (long) counts.get( offset );//how much bytes we have to read from above offset i.e size of strip
			
			if(offset + 1 < offsets.size()){
				count_max = (int) (this.getImageLength() % rowsperstrip);
			}
			
			count_max  = ( count_max * 8 ) / this.getBitsPerSample();
//			System.out.println("Bits Per Sample : "+this.getBitsPerSample());
//			System.out.println("Count Max : "+count_max);
			if(this.getBitsPerSample() == 8){
				for(int count = 0 ; count < count_max ; count++){
					fstream.read(buffer);
					row = (int) (count / this.getImageLength());
					col = (int) (count % this.getImageLength());
					if( col == 0) System.out.println();
					pixel [row][col] = buffer[0];
					System.out.print(pixel[row][col]+" ");
				}
			}else if(this.getBitsPerSample() == 4){
				for(int count = 0 ; count < count_max ; count++){
					fstream.read(buffer);
					row = (int) (count / this.getImageLength());
					col = (int) (count % this.getImageLength());
					if( col == 0) System.out.println();
					pixel [row][col] = buffer[0] >> 4;
					System.out.print(pixel[row][col]+" ");
					count++;
					row = (int) (count / this.getImageLength());
					col = (int) (count % this.getImageLength());
					pixel [row][col] =  ( buffer[0] & 0x0000ffff );
					System.out.print(pixel[row][col]+" ");
				}
			}else if(this.getBitsPerSample() == 1){
				for(int count = 0 ; count < count_max ;){
					fstream.read(buffer);
					int x = 8;
					while( x != 0){
						row = (int) (count / this.getImageLength());
						col = (int) (count % this.getImageLength());
						if( col == 0) System.out.println();
						pixel [row][col] = ( buffer[0] >> (x-1) ) & 0x00000001;
						System.out.print(pixel[row][col]+" ");
						x--;
						count++;
					}
				}
			}
		}
	}
	@SuppressWarnings({ })
	public void encodePixels(RandomAccessFile fstream) throws Exception{
		ArrayList<Object> offsets = new ArrayList<Object>();
		ArrayList<Object> counts  = new ArrayList<Object>();
		this.setCompression(1);
		this.setImageLength(this.getImageLength());
		this.setImageWidth(this.getImageWidth());
		this.setRowsPerStrip(this.getImageLength());
		offsets.add((Object)20000);
		counts.add((Object)(int)(this.getImageLength()*this.getImageWidth()/8));
		
		this.setStripOffsets(offsets);
		this.setStripByteCounts(counts);
		
		byte buffer[] = new byte[(int) ((this.getImageLength()*this.getImageWidth()*this.getBitsPerSample())/8) + 1];
		int i = 0;
		for( int r = 0; r < this.getImageLength(); r++){
			for(int c = 0; c < this.getImageWidth(); c++){
				//buffer[i] = 0b00001000;
				buffer[i] = Types.setBit( buffer[i], c%8 , pixel[r][c] );
				if( c%8 == 0 ) i++;
			}
		}
		
		fstream.seek(20000);
		fstream.write(buffer);
		fstream.close();
	}
	public long getNewSubfileType() throws Exception{
		int tag=254;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getSubfileType() throws Exception{
		int tag=255;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getImageWidth() throws Exception{
		int tag=256;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");	
	}
	public void setImageWidth(long width) throws Exception{
		int tag =256;
		if((IFD.fields).containsKey(tag)){
			(IFD.fields).get(tag).getValues().add(0, width);
			(IFD.fields).get(tag).setValue_offset(width);
		}	
	}
	public long getImageLength() throws Exception{
		int tag=257;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public void setImageLength(long length) throws Exception{
		int tag =257;
		if((IFD.fields).containsKey(tag)){
			(IFD.fields).get(tag).getValues().add(0, length);
			(IFD.fields).get(tag).setValue_offset(length);
		}	
	}
	public long getBitsPerSample() throws Exception{
		int tag=258;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getCompression() throws Exception{
		int tag=259;
		if((IFD.fields).containsKey(tag)){
			return (long)(IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public void setCompression(long compression) throws Exception{
		int tag =259;
		if((IFD.fields).containsKey(tag)){
			(IFD.fields).get(tag).getValues().add(0, compression);
			(IFD.fields).get(tag).setValue_offset(compression);
		}	
	}
	public int getPhotometricInterpretation() throws Exception{
		int tag=262;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getThresholding() throws Exception{
		int tag=263;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getCellWidth() throws Exception{
		int tag=264;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getCellLength() throws Exception{
		int tag=265;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getFillOrder() throws Exception{
		int tag=266;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	@SuppressWarnings("rawtypes")
	public ArrayList getStripOffsets() throws Exception{
		int tag=273;
		if((IFD.fields).containsKey(tag)){
			return (ArrayList) (IFD.fields).get(tag).getValues();
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public void setStripOffsets(ArrayList<Object> offsets) throws Exception{
		int tag =273;
		if((IFD.fields).containsKey(tag)){
			(IFD.fields).get(tag).setValues(offsets);
			(IFD.fields).get(tag).setValue_offset((int)offsets.get(0));
		}	
	}
	public int getOrientation() throws Exception{
		int tag=274;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getSamplesPerPixel() throws Exception{
		int tag=277;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getRowsPerStrip() throws Exception{
		int tag=278;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public void setRowsPerStrip(long rowsperstrip) throws Exception{
		int tag =278;
		if((IFD.fields).containsKey(tag)){
			(IFD.fields).get(tag).getValues().add(0, rowsperstrip);
			(IFD.fields).get(tag).setValue_offset(rowsperstrip);
		}	
	}
	@SuppressWarnings("rawtypes")
	public ArrayList getStripByteCounts() throws Exception{
		int tag=279;
		if((IFD.fields).containsKey(tag)){
			return (ArrayList) (IFD.fields).get(tag).getValues();
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public void setStripByteCounts(ArrayList<Object> counts) throws Exception{
		int tag =279;
		if((IFD.fields).containsKey(tag)){
			(IFD.fields).get(tag).setValues(counts);
			(IFD.fields).get(tag).setValue_offset((int)counts.get(0));
		}	
	}
	public long getMinimumSampleValue() throws Exception{
		int tag=280;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getMaximumSampleValue() throws Exception{
		int tag=281;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public double getXResolution() throws Exception{
		int tag=282;
		if((IFD.fields).containsKey(tag)){
			return (double) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public double getYResolution() throws Exception{
		int tag=283;
		if((IFD.fields).containsKey(tag)){
			return (double) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getPlanerConfiguration() throws Exception{
		int tag=284;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public double getXPosition() throws Exception{
		int tag=286;
		if((IFD.fields).containsKey(tag)){
			return (double) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getYPosition() throws Exception{
		int tag=287;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getFreeOffsets() throws Exception{
		int tag=288;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getFreeByteCounts() throws Exception{
		int tag=289;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getGrayResponseUnit() throws Exception{
		int tag=290;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getGrayResponseCurve() throws Exception{
		int tag=291;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getT4Options() throws Exception{
		int tag=292;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public long getT6Options() throws Exception{
		int tag=293;
		if((IFD.fields).containsKey(tag)){
			return (long) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getResolutioUnit() throws Exception{
		int tag=296;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getPageNumber() throws Exception{
		int tag=297;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getColorMap() throws Exception{
		int tag=320;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getHalfToneHints() throws Exception{
		int tag=321;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getInkSet() throws Exception{
		int tag=332;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getNumberOfInks() throws Exception{
		int tag=334;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getDotRange() throws Exception{
		int tag=336;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getExtraSamples() throws Exception{
		int tag=338;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
	public int getSampleFormat() throws Exception{
		int tag=339;
		if((IFD.fields).containsKey(tag)){
			return (int) (IFD.fields).get(tag).getValues().get(0);
		}
		throw new Exception("Tag "+tag+" Not Found");
	}
}
