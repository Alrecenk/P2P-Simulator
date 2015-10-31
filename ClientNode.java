/* This node can be used to simulate a request from an entity outside your peer to peer network.
 * Use send(to, message) to send messages from the node
 * and receive() to get any arriving messages (returns null if no arrivals).
 */
import java.awt.Color;
import java.awt.Graphics;

public class ClientNode extends Node{

	public ClientNode(String address){
		super(address);
	}

	public void run(){}

	// Pull a message off the message_queue or return null if no messages.
	public Message receive(){
		if(message_queue.peek() !=null){
			return message_queue.poll();
		} else {
			return null;
		}
	}

	// Override draw method to make clients blue squres.
	public synchronized void draw(Graphics gr){
		if(!stopped){
			gr.setColor(Color.blue);
			gr.drawRect((int)x-10,(int)y-10,20,20);
		}
	}
}

