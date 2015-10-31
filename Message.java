// A Message containing data the the address of its sender.
public class Message{
	public String from;
	public byte[] message;

	public Message(String from, byte[] message){
		this.from = from;
		this.message = message;
	}
}