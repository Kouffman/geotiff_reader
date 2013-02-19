package tiff;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;

/**
 */
public class ImageFileDirectory {
    /**
     */
    private int count;

    /**
     */
    HashMap<Integer,ImageFileField> fields;

    /**
     */
    private long next_IFD_offset;

    /**
     * @return 
     */
    public int getCount() {
        return fields.size();
    }

    /**
     * @param tag : tag of field to be retrieved
     * @return IFF
     */
    public ImageFileField getField(int tag)throws Exception {
    	if(fields.containsKey(new Integer(tag)))
        return fields.get(new Integer(tag));
    	else throw new Exception("Tag Not Found");
    }
    /**
     * @param IFF : IFF to be set
     */
    public void setField(ImageFileField IFF){
    	fields.put(new Integer(IFF.tag), IFF);
    }

    /**
     * @return offset of next IFD (long)
     */
    public long getNext_IFD_offset() {
        return next_IFD_offset;
    }

    /**
     * @param fstream 
     * @param start 
     * @return ImageFileDirectory
     * @throws IOException 
     */
    public static ImageFileDirectory parseIFD(RandomAccessFile fstream, long start, boolean byte_order) throws IOException {
//    	System.out.println("Pointer : "+fstream.getFilePointer());
    	fstream.seek(start);
    	ImageFileDirectory IFD = new ImageFileDirectory();
    	ImageFileField IFF;
    	
    	byte[] buffer = new  byte[2];
    	
    	fstream.read(buffer);
    	
    	IFD.count = Types.getShort(buffer, 0,byte_order);
//    	System.out.println("IFD Count : "+IFD.count);
    	
    	HashMap<Integer,ImageFileField> fields= new HashMap<Integer,ImageFileField>();
    	for(int i = 0; i < IFD.count ; i++){
    		IFF = ImageFileField.parseIFF(fstream,byte_order);
    		System.out.println(IFF);
    		fields.put(new Integer(IFF.tag), IFF);//Integer not created
    	}
    	IFD.fields=fields;
        return IFD;
    }
    public byte[] getAsBytes(){
    	byte[] result = new byte[this.count * 12 + 2];
    	Types.arrayInsert(result,0,Types.getShortAsBytes(this.count, ImageFileHeader.getByte_order()));
    	Integer[] tags = new Integer[1];
    	tags  = fields.keySet().toArray(tags);
    	int tp = 2;
    	Arrays.sort(tags);
    	
    	for(int tag : tags){
    		//System.out.println(fields.get(tag)+"\n");
    		Types.arrayInsert(result, tp, fields.get(tag).getAsBytes());
    		tp+=12;
    	}
    	
    	return result;
    }
    public String toString(){
    	return "IFD : "+count+" Next IFD Offset : "+next_IFD_offset;                                         
    }

	public void setNext_IFD_offset(long next_IFD_offset) {
		this.next_IFD_offset = next_IFD_offset;
	}
}
