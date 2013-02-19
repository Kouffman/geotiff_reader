package tiff;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 */
public class ImageFileHeader {
    /**
     */
    private static boolean byte_order;

    /**
     */
    private int version;

    /**
     */
    private long next_IFD_offset;

    /**
     * @return 
     */
    public void setNext_IFD_offset(long value){
    	this.next_IFD_offset = value;
    }
    public long getNext_IFD_offset() {
        return this.next_IFD_offset;
    }

    /**
     * @return 
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * @return 
     */
    public static boolean getByte_order() {
        return byte_order;
    }

    /**
     * @param fstream 
     * @param start 
     * @return 
     * @throws IOException 
     */
    @SuppressWarnings("static-access")
	public static ImageFileHeader parseIFH(RandomAccessFile fstream) throws IOException {
    	ImageFileHeader IFH=new ImageFileHeader();
    	byte[] buffer=new byte[8];
    	fstream.read(buffer,0,8);
    	//retrieve BYTE ORDER
		if(buffer[0]==0x49&&buffer[0]==buffer[1]){
			IFH.byte_order=true;
		}
		else if(buffer[0]==0x4D&&buffer[0]==buffer[1]){
			IFH.byte_order=false;
		}
		//retrieve VERSION i.e usually 42
		IFH.version = Types.getShort(buffer, 2, IFH.byte_order);
		
		//retrieve IFD offset
		IFH.next_IFD_offset = Types.getLong(buffer, 4,IFH.byte_order);
        return IFH;
    }
    @SuppressWarnings("static-access")
	public byte[] getAsBytes(){
    	byte[] result = new byte[8];
    	
    	if(this.byte_order){
    		result[0] = 0x49;
    		result[1] = 0x49;
    	}else{
    		result[0] = 0x4D;
    		result[1] = 0x4D;
    	}
    	Types.arrayInsert( result, 2, Types.getShortAsBytes(this.version, this.byte_order));
    	Types.arrayInsert( result, 4, Types.getLongAsBytes(this.next_IFD_offset, this.byte_order));
    	return result;
    }
	public String toString(){
		return "Class: \nImageFileHeader \nFields : \nBYTE ORDER : "+byte_order+"\nVERSION : "+version+"\nNEXT IFD OFFSET : "+next_IFD_offset;
	}

}