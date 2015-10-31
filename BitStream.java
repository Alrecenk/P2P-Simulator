/* This is a stream for reading and writing at the bit level. It's designed to be used with compressionalgorithms. 
 * If you just want to write standard types use ByteStream.
 * Written by Alrecenk 2010.
 */

import java.io.*;
public class BitStream{

	public ByteList data = new ByteList();
	public int readcounter = 0 ;
	public int writecounter = 0 ;

	// Reads an integer 0 or 1 for the next bit.
	private int readBitsimple(){
		int index = readcounter/8;
		int bit = readcounter%8 ;
		byte b = data.get(index);
		readcounter++;
		return (b>>bit)&1 ;
	}

	// Reads an integer 0 or 1 for the next bit.
	// Does the same as above but slightly optimized.
	public int readBit(){
		int bit = (data.get(readcounter>>3)>>(readcounter&7))& 1 ;
		readcounter++;
		return bit ;
	}

	// Reads a bit at the given location.
	public int readBit(int loc){
		return (data.get(loc>>3)>>(loc&7))& 1 ;
	}

	// Reads the next "bits" bits and converts them to an unsigned int.
	public int readUnsignedInt(int bits){
		if(bits>0){
			int n = readBit() ;
			for(int k=1;k<bits;k++){
				n = (n<<1) | readBit();
			}
			return n ;
		}
		return 0 ;

	}

	// Reads one bit to determine sign (1 is positive, 0 is negative)
	// then reads the next "bits" bits and converts them to an unsigned 
	// int to be used with the sign.
	public int readSignedInt(int bits){
		int sign = readBit();
		int number = readUnsignedInt(bits);
		if(sign==0){
			number*=-1;
		}
		return number ;

	}

	// When passed an integer 0 or 1 writes it at the end of the stream.
	private void writeBitSimple(int value){
		int index = writecounter/8;
		int bit = writecounter%8 ;
		while(index >=data.arraysize()){
			data.doublelength();
		}
		byte b = data.get(index);
		int mask = 1 << bit ;
		b = (byte)(b-(b&mask) + (value<<bit)) ;
		data.set(index,b);
		writecounter++;
	}


	// When passed an integer 0 or 1 writes it at the end of the stream.
	// Same as above but slightly more efficient.
	public void writeBit(int value){
		int index = writecounter>>3;
		int bit = writecounter&7 ;
		while(index >=data.arraysize()){
			data.doublelength();
		}
		byte b = data.get(index) ;
		data.set(index,(byte)(b-(b&(1<<bit)) + (value<<bit)));
		writecounter++;
	}

	// Writes the least significant "bits" bits of i to the stream as an unsigned int.
	public void writeUnsignedInt(int i, int bits){
		for(int k=bits-1;k>=0;k--){
			writeBit((i>>k)&1);
		}
	}
	
	// Writes one bit to determine sign (1 is positive, 0 is negative)
	// then writes the next "bits" bits as an unsigned int to be used with the sign.
	public void writeSignedInt(int i, int bits){
		if(i>0){
			writeBit(1);
		}else{
			writeBit(0);
		}writeUnsignedInt(Math.abs(i),bits);
	}

	// Appends bits from a BitStream up to that BitStream's write-counter.
	public void appendBitStream(BitStream o){
		o.readcounter = 0 ;
		for(int k=0;k<o.writecounter ;k++){
			this.writeBit(o.readBit()) ;
		}
	}

	// Reads the next "bits" bits from this stream or to the writecounter, 
	// whichever is smaller and puts them into their own stream
	// incrementing the counters on both streams.
	public BitStream readSubStream(int bits){
		BitStream s = new BitStream() ;
		bits = Math.min(bits,writecounter-readcounter) ;
		for(int k=0;k<bits;k++){
			s.writeBit(readBit()) ;
		}
		return s ;
	}

	// Reads a file and converts it to a bitstream.
	public static BitStream read(File f){
		try{
			FileInputStream d = new FileInputStream(f);
			BitStream b = new BitStream();
			b.data = new ByteList(d.available());
			d.read(b.data.b,0,b.data.b.length) ;
			b.data.size = b.data.b.length ;			
			b.writecounter = b.data.filledsize()*8 ;
			//System.out.println("Stream Length: " + b.writecounter);
			return b ;
		}catch(Exception e){
			e.printStackTrace() ;
			return null ;
		}
	}

	// Writes this entire bitstream to a file (ignoring read pointer).
	public void write(File f){
		try{
			FileOutputStream d = new FileOutputStream(f);
			d.write(data.b,0,data.filledsize()+1);
			d.close();
		}catch(Exception e){
			e.printStackTrace() ;
		}
	}

	// Unit tests for BitStream.
	public static void main(String args[]){

		BitStream s = new BitStream();
		int amount = 35 ;
		System.out.print(amount + " raw bits:");
		System.out.print("\nwrite:");
		for(int k=0;k<amount;k++){
			if(Math.random()<.5){
				s.writeBit(0) ;
				System.out.print("0");
			}else{
				s.writeBit(1) ;
				System.out.print("1");	
			}
		}

		System.out.print("\n");
		int bits =8 ;
		int aui=35;
		System.out.print( aui + " " + bits + " bit numbers:");
		System.out.print("\nwrite:");
		for(int k=0;k<aui;k++){
			int i = (int)(Math.random()*(1<<bits)) ;
			s.writeUnsignedInt(i,bits) ;
			s.writeBit(1);
			System.out.print(i +", ");
		}


		s.write(new File("testBitStream.bit"));
		BitStream t = BitStream.read(new File("testBitStream.bit"));

		System.out.print("\nread :");
		for(int k=0;k<amount;k++){
			int i = t.readBit();
			if(i==0){
				System.out.print("0");
			}else{
				System.out.print("1");	
			}
		}

		System.out.print("\nread :");

		for(int k=0;k<aui;k++){
			int i = t.readUnsignedInt(bits);
			int q = t.readBit() ;
			System.out.print(i +", ");
		}

		System.out.print("\n");
		System.out.print("bits: " + s.writecounter);
		System.out.print("\n");
		System.out.print("bytes: " + s.data.filledsize());
	}
}

// Utility class used by BitStream.
class ByteList{
	public byte b[];
	public int size = 0 ;

	public ByteList(int size){
		b = new byte[size];
	}

	public ByteList(){
		b=new byte[100] ;
	}

	public ByteList(byte c[]){
		b=c ;
		size = b.length ;
	}

	public final int filledsize(){
		return size ;
	}

	public final int arraysize(){
		return b.length ;
	}

	public final byte get(int k){		
		return b[k];
	}
	public final void set(int k, byte a){
		b[k] = a ;
		if(k>size)size=k;
	}

	public final void extendtolength(int newlength){
		if(newlength > b.length){
			byte b2[] = new byte[newlength];
			for(int k=0;k<b.length;k++){
				b2[k] = b[k];
			}
			b = b2 ;
		}
	}

	public final void doublelength(){
		byte b2[] = new byte[b.length*2];
		for(int k=0;k<b.length;k++){
			b2[k] = b[k];
		}
		b = b2 ;
	}
}