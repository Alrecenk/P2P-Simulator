/* A variable length array of bytes with convenient methods for sequentially/randomly reading/writing primitive types and other streams to files and sockets.
 * Written by Alrecenk 2010.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ByteStream{
	public byte data[];
	public int filledsize = 0 ;//the highest byte index written to so far + 1 (usually the number of bytes written)

	//where to read or write when doing so sequentially
	public int readpointer=0 ;
	public int writepointer=0 ;

	public ByteStream(int size){
		data = new byte[size];
	}

	public ByteStream(){
		data=new byte[100] ;
	}

	public ByteStream(byte c[]){
		data=c ;
		filledsize = data.length ;
		writepointer = filledsize ;
	}

	public ByteStream(ByteStream s){
		data = new byte[s.data.length];
		for(int k=0;k<data.length;k++){
			data[k] = s.data[k] ;
		}
		filledsize = s.filledsize;
		readpointer = s.readpointer ;
		writepointer = s.writepointer ;
	}

	// Creates a ByteStream wrapper for a BitStream assuming the writepointer is at the end of the stream
	public ByteStream(BitStream s){
		int size = (int)(s.writecounter+7)/8 ;
		data = new byte[size] ;
		for(int k=0;k<data.length;k++){
			data[k] = s.data.b[k] ;
		}
		writepointer = size ;
		readpointer = 0 ;
		filledsize = size ;
	}

	// Converts byte stream to a BitStream.
	public synchronized BitStream converttobits(){
		BitStream s = new BitStream();
		s.data = new ByteList(data);
		s.writecounter = filledsize*8 ;
		s.readcounter = 0 ;
		return s ;
	}

	// Read the next byte in the stream.
	public synchronized byte readByte(){
		byte d = data[readpointer];
		readpointer++;
		return d ;
	}
	// Write the next byte in the stream.
	public synchronized void writeByte(byte b){
		if(writepointer>=data.length){
			doubleLength();
		}
		data[writepointer] = b;
		writepointer++;
		if(writepointer>filledsize)filledsize=writepointer;//keep track of furthest area written
	}

	// Read the next two bytes as a signed short.
	public synchronized short readShort(){
		short s = (short)(((data[readpointer]&0xff)<<8) | (data[readpointer+1]&0xff)) ;//most significant byte first
		readpointer+=2;
		return s ;
	}

	// Write the given short into the next two bytes.
	public synchronized void writeShort(short s){
		if(writepointer+1>=data.length){//make sure there is room to write
			doubleLength();
		}
		data[writepointer] = (byte)((s&0xff00)>>>8) ;//most significant byte first
		data[writepointer+1] = (byte)(s&0xff) ;
		writepointer+=2;
		if(writepointer>filledsize)filledsize=writepointer;//keep track of furthest area written
	}

	// Read the next 4 bytes as a signed int.
	public synchronized int readInt(){
		int i = ((data[readpointer]&0xff)<<24) | ((data[readpointer+1]&0xff)<<16) | ((data[readpointer+2]&0xff)<<8)| ((data[readpointer+3]&0xff));//most significant byte first
		readpointer+=4;
		return i ;		
	}

	// Write the given signed int to the next 4 bytes.
	public synchronized void writeInt(int i){
		if(writepointer+3>=data.length){//make sure there is room to write
			doubleLength();
		}
		data[writepointer] = (byte)((i&0xff000000)>>>24) ;//most significant byte first
		data[writepointer+1] = (byte)((i&0xff0000)>>>16) ;
		data[writepointer+2] = (byte)((i&0xff00)>>>8) ;
		data[writepointer+3] = (byte)(i&0xff) ;
		writepointer+=4;
		if(writepointer>filledsize)filledsize=writepointer;//keep track of furthest area written
	}

	// Read the next 8 bytes as a signed long.
	public synchronized long readLong(){

		long i = (((long)data[readpointer]&0xff)<<56) | 
				(((long)data[readpointer+1]&0xff)<<48) | 
				(((long)data[readpointer+2]&0xff)<<40) | 
				(((long)data[readpointer+3]&0xff)<<32) | 
				(((long)data[readpointer+4]&0xff)<<24) | 
				(((long)data[readpointer+5]&0xff)<<16) | 
				(((long)data[readpointer+6]&0xff)<<8) | 
				(((long)data[readpointer+7]&0xff))  ;
		readpointer+=8;
		return i ;		
	}

	// Write the given signed long to the next 8 bytes.
	public synchronized void writeLong(long l){
		if(writepointer+7>=data.length){//make sure there is room to write
			doubleLength();
		}
		data[writepointer] = (byte)((l>>>56)&0xff) ;//most significant byte first
		data[writepointer+1] = (byte)((l>>>48)&0xff) ;
		data[writepointer+2] = (byte)((l>>>40)&0xff) ;
		data[writepointer+3] = (byte)((l>>>32)&0xff) ;
		data[writepointer+4] = (byte)((l>>>24)&0xff) ;
		data[writepointer+5] = (byte)((l>>>16)&0xff) ;
		data[writepointer+6] = (byte)((l>>>8)&0xff) ;
		data[writepointer+7] = (byte)(l&0xff) ;
		writepointer+=8;
		if(writepointer>filledsize)filledsize=writepointer;//keep track of furthest area written
	}

	// Read the next 4 bytes as a float.
	public synchronized float readFloat(){
		return Float.intBitsToFloat(readInt()) ;
	}

	// Write the next 4 bytes as a float.
	public synchronized void writeFloat(float f){
		writeInt(Float.floatToIntBits(f));
	}
	
	// Read a byte at a specific location.
	public final byte readByte(int k){		
		return data[k];
	}

	// Write a byte at a specific location.
	public synchronized void writeByte(byte b, int writepointer){
		if(writepointer>=data.length){
			doubleLength();
		}
		data[writepointer] = b;
		writepointer++;
		if(writepointer>filledsize)filledsize=writepointer;//keep track of furthest area written
	}

	// Read a short at a specific location.
	public short readShort(int readpointer){
		short s = (short)(((data[readpointer]&0xff)<<8) | (data[readpointer+1]&0xff)) ;//most significant byte first
		readpointer+=2;
		return s ;
	}

	// Write a short at a specific location.
	public synchronized void writeShort(short s, int wp){
		if(wp+1>=data.length){//make sure there is room to write
			doubleLength();
		}
		data[wp] = (byte)((s&0xff00)>>>8) ;//most significant byte first
		data[wp+1] = (byte)(s&0xff) ;
		if(wp>filledsize)filledsize=wp;//keep track of furthest area written
	}

	// Read an int at a specific location.
	public int readInt(int rp){
		int i = ((data[rp]&0xff)<<24) | ((data[rp+1]&0xff)<<16) | ((data[rp+2]&0xff)<<8)| ((data[rp+3]&0xff));//most significant byte first
		//readpointer+=4;
		return i ;		
	}

	// Write an int at a specific location.
	public synchronized void writeInt(int i, int wp){
		if(wp+3>=data.length){//make sure there is room to write
			doubleLength();
		}
		data[wp] = (byte)((i&0xff000000)>>>24) ;//most significant byte first
		data[wp+1] = (byte)((i&0xff0000)>>>16) ;
		data[wp+2] = (byte)((i&0xff00)>>>8) ;
		data[wp+3] = (byte)(i&0xff) ;
		if(wp+4>filledsize)filledsize=wp+4;//keep track of furthest area written
	}

	// Read a float at a specific location.
	public synchronized float readFloat(int rp){
		return Float.intBitsToFloat(readInt(rp)) ;
	}

	// Write a float at a specific location.
	public synchronized void writeFloat(float f, int wp){
		writeInt(Float.floatToIntBits(f),wp);
	}

	// Reads a string. Strings written as null will return as "".
	public synchronized String readString(){
		int length = readInt();
		char c[] = new char[length];
		for(int k=0;k<c.length;k++){
			c[k] = (char) readShort();
		}
		return new String(c);
	}

	// Writes a string.
	public synchronized void writeString(String s){
		if(s == null){
			writeInt(0);
			return;
		}
		int length = s.length();
		writeInt(length);
		char c[] = s.toCharArray(); ;
		for(int k=0;k<c.length;k++){
			writeShort((short)c[k]);
		}
	}

	// Returns the current filled size of the data for this stream.
	public final int filledSize(){
		return filledsize ;
	}

	// Return the total array data allocated for this stream.
	public final int arraySize(){
		return data.length ;
	}

	// Extend length of data array to the given length(must be longer than current length).
	public synchronized void extendToLength(int newlength){
		if(newlength > data.length){
			byte b2[] = new byte[newlength];
			for(int k=0;k<data.length;k++){
				b2[k] = data[k];
			}
			data = b2 ;
		}
	}

	// Double length of data array.
	public synchronized void doubleLength(){
		byte b2[] = new byte[data.length*2];
		for(int k=0;k<data.length;k++){
			b2[k] = data[k];
		}
		data = b2 ;
	}

	// Cuts the length of the ByteStream to just long enough to hold the things that have already been written.
	public synchronized void clearExtraSpace(){
		byte b2[] = new byte[filledsize];
		for(int k=0;k<b2.length;k++){
			b2[k] = data[k];
		}
		data = b2 ;
	}
	
	// Return all of the bytes that have been written.
	public synchronized byte[] getBytes(){
		byte b2[] = new byte[filledsize];
		for(int k=0;k<b2.length;k++){
			b2[k] = data[k];
		}
		return b2;
	}

	// Appends a ByteStream onto the end of this one.
	public synchronized void append(ByteStream bs){
		byte d2[] = new byte[filledsize+bs.filledsize];
		for(int k=0;k<filledsize;k++){
			d2[k] = data[k];
		}
		for(int k=0;k<bs.filledsize;k++){
			d2[k+filledsize] = bs.data[k] ;
		}
		data = d2 ;
		filledsize += bs.filledsize ;
		writepointer = filledsize ;
	}

	// Reads a subset of this stream into another stream
	// starting at readpointer and reading length.
	// DOES NOT do any bounds checking for speed reasons, so be careful.
	public synchronized ByteStream readstream(int length){
		byte data2[] = new byte[length];
		for(int k=0;k<data2.length;k++){
			data2[k] = data[readpointer+k];
		}
		readpointer+=length ;
		return new ByteStream(data2) ;
	}

	// Reads a file into a ByteStream.
	public static ByteStream read(File f){
		try{
			FileInputStream d = new FileInputStream(f);
			ByteStream b = new ByteStream(d.available());
			d.read(b.data,0,b.data.length) ;
			b.filledsize = b.data.length ;
			b.writepointer = b.filledsize ;

			return b ;
		}catch(Exception e){
			System.err.println("Failed to read ByteStream from file: " + e ) ;
			e.printStackTrace() ;
			return null ;
		}
	}

	// Write this ByteStream to a file.
	public void write(File f){
		try{
			FileOutputStream d = new FileOutputStream(f);
			d.write(data,0,filledsize);
			d.close();
		}catch(Exception e){
			System.err.println("Failed to write ByteStream to file: " + e ) ;
			e.printStackTrace() ;
		}
	}

	// A unit test of basic functions.
	public static void main(String args[]){
		ByteStream stream = new ByteStream();
		stream.writeByte((byte)-75) ;
		stream.writeByte((byte)102) ;
		stream.writeShort((short)20007) ;
		stream.writeShort((short)-21002) ;
		stream.writeString("Hello world!");
		stream.writeInt(17926549);
		stream.writeInt(-7926549);
		stream.writeFloat(0.8765f);
		System.out.println(stream.readByte());
		System.out.println(stream.readByte());
		System.out.println(stream.readShort());
		System.out.println(stream.readShort());
		System.out.println(stream.readString());
		System.out.println(stream.readInt());
		System.out.println(stream.readInt());
		System.out.println(stream.readFloat());
	}
}