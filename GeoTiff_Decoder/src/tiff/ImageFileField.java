package tiff;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 */
public class ImageFileField {
    /**
     */
    public int tag;

    /**
     */
    private int datatype;
    
    public void setDatatype(int datatype) {
		this.datatype = datatype;
	}

	/**
     */
    private long count;
    
    /**
     */
    private ArrayList<Object> values;

    public void setValues(ArrayList<Object> values) {
		this.values = values;
	}

	/**
     */
    private long value_offset;

    public void setValue_offset(long value_offset) {
		this.value_offset = value_offset;
	}

	/**
     * @return 
     */
    public int getTag() {
        return tag;
    }

    /**
     * @return 
     */
    public int getDatatype() {
        return datatype;
    }

    /**
     * @return 
     */
    public ArrayList<Object> getValues() {
        return values;
    }
   
  
    /**
     * @return 
     */
    public long getValue_offset() {
        return value_offset;
    }

    /**
     * @param fstream 
     * @param start 
     * @return 
     * @throws IOException 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static ImageFileField parseIFF(RandomAccessFile fstream,boolean byte_order) throws IOException {
    	byte[] buffer = new byte[12];
    	fstream.read(buffer);
    	ImageFileField IFF = new ImageFileField();
    	IFF.tag = Types.getShort(buffer, 0, byte_order);
    	IFF.datatype = Types.getShort(buffer, 2, byte_order);
    	IFF.count = Types.getLong(buffer, 4, byte_order);
    	//for(int i=4;i<8;i++){
    	//	System.out.println("Bytes "+i+(byte)buffer[i]);
    	//}
    	IFF.values = new ArrayList();
    	IFF.value_offset = Types.getLong(buffer, 8, byte_order);
    	if(IFF.count * Types.DATATYPE[IFF.datatype] <= 4){
    		IFF.values.add((Object)new Long(IFF.value_offset));
    	}
    	else{
    		//System.out.println("Values Arrays");
    		long temp = fstream.getFilePointer();
    		int start = 0;
    		//for(int i=0;i<4;i++){
    			fstream.seek(IFF.value_offset);
    			for(long j=0;j<IFF.count;j++){
    				if(IFF.datatype == 2){
    					if(IFF.count == 0){//Single String
    						StringBuffer result = new StringBuffer();
    						buffer = new byte[1];
    						do{
    							fstream.read(buffer);
    							result.append((char )buffer[0]);
    						}while(buffer[0] != 0);
    						//System.out.println("Single String Ascii : "+result.substring(0));
    						IFF.values.add(result.substring(0));
    						
    					}else{//Multiple String
    						for(int i = 0; i < IFF.count; i++){
	    						StringBuffer result = new StringBuffer();
	    						buffer = new byte[1];
	    						do{
	    							fstream.read(buffer);
	    							result.append((char )buffer[0]);
	    						}while(buffer[0] != 0);
	    						//System.out.println("Multi String Ascii : "+result.substring(0));
	    						IFF.values.add(result.substring(0));
	    						i += result.substring(0).length();
    						}
    					}
    					j = IFF.count;
    				}
    				else{
    					buffer = new byte[Types.DATATYPE[IFF.datatype]];//int and long problem here 
            			fstream.read(buffer);
            			IFF.values.add(Types.getObject(buffer,start,IFF.datatype,byte_order));    					
    				}
    			}
    	//	}
    		fstream.seek(temp);
    	}
        return IFF;
    }
    public String toString(){
    	return "IFF :> Tag : "+tag+" Datatype : "+datatype+" Count : "+count+(count * Types.DATATYPE[datatype] <= 4 ? " Value : ":" OffSet : ")+value_offset+" size : "+values.size()+"\n";
    }

	public byte[] getAsBytes() {
		byte[] result = new byte[12];
		Types.arrayInsert(result, 0, Types.getShortAsBytes(this.tag, ImageFileHeader.getByte_order()));
		Types.arrayInsert(result, 2, Types.getShortAsBytes(this.datatype, ImageFileHeader.getByte_order()));
		Types.arrayInsert(result, 4, Types.getLongAsBytes(this.count, ImageFileHeader.getByte_order()));
		return result;
	}

	public long getCount() {
		return count;
	}
}