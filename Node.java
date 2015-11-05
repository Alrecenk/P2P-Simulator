/* Extend this class to implement a new peer to peer application to run in the simulator.
 * The only method you need to override is run(), which should exit gracefully if stopped is set.
 * You can use send(address, message) to send messages in the network.
 * Received messages will be placed in the message_queue you can access at any time.
 * You can override draw if you want to make this node look different in the simulator.
 * Nodes should use getTime() for timing, so their speed adjusts with the network.
*/

import java.awt.Color;
import java.awt.Graphics;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Node implements Runnable{
	
	public String address; // The network address of the node.
	LinkedBlockingQueue<Message> message_queue;// A queue of unprocessed messages received.
	public boolean stopped = false; // Setting to true should cause the run method to exit.
	
	// These variables are managed by the network.
	public Network network;
	public float x,y ;
	public float refill_rate; // bytes per second (recharges flow over time)
	public float maximum_flow; // bytes at maximum flow
	public float flow; // current available flow
	public double last_time; // time flow was last updated
	public double clock_offset; // The difference between this node's clock and the network clock.

	public Node(String address){
		this.address = address;
		message_queue = new LinkedBlockingQueue<Message>();
	}
	
	public void receive(String from, byte[] message) {
		// Note that we don't process in this method because it blocks the network.
		message_queue.add(new Message(from, message));
	}
	
	// Send a message to a node in the network (note: reasonable arrival time is not guaranteed).
	public void send(String to, byte[] message){
		if(network !=null){
			network.sendMessage(address, to, message);
		}
	}
	
	// Use this for any time keeping. It allows the rate of time passage to be adjusted at the network level.
	// It also gives each node clock variability to simulate unsynchronized clocks over the network.
	public double getTime(){
		return network.getTime() + clock_offset;
	}
	
	// Stops the node. the network will stop communicating with a stopped node.
	public void stop(){
		stopped = true;
	}
	
	// This method draws your node. It will be called by the Network draw function.
	// You can override it to make different types of nodes look different (or print app specific info by the node).
	// Keep in mind connections are always to (x,y+-10) though.
	public synchronized void draw(Graphics gr){
		if(!stopped){
			int r=0,g=0;
			float mid = maximum_flow/2;
			if( flow < mid){
				r = (int)(255* (mid - flow) / mid) ;
			} else {
				g =  (int)(255* (flow - mid) / mid) ;
			}
			gr.setColor(new Color(Math.min(r,255),Math.min(g,255),0));
			gr.drawOval((int)x-10,(int)y-10,20,20);
		}
	}
	
// The following two methods are used by Network to manage flow into and out of the node.
// You should not be calling them in your node.
	
	// Returns the time at which the message will arrive considering download rate.
	public double sendTime(int size, double requesttime){
		updateFlow(requesttime); // enforces flow build up cap out when not in use.
		double arrival = requesttime + Math.max((size-flow) / refill_rate, 0);
		flow -= size ; 
		return arrival;
	}
	// Update the stored download flow of this node.
	public void updateFlow(double time){
		flow += refill_rate * (time - last_time);
		if(flow > maximum_flow){
			flow = maximum_flow;
		}
		last_time = time;
	}
	
}
			
