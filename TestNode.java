/* A simple example node that just cycles sending empty packets to it's given targets.
 * When a TestNode receives a packet it will add the sender to it's target list.
 * If a TestNode doesn't receive a response from one of its targets for a while it stops sending there.
 */
import java.util.ArrayList;
import java.util.HashMap;

public class TestNode extends Node{

	ArrayList<String> target;
	HashMap<String, Double> lastmessage;
	double wait,lasttime;
	int size;
	int which;

	public TestNode(String id, ArrayList<String> target, double wait, int size){
		super(id);
		this.target= target ;
		this.wait=wait;
		this.size = size;
		lastmessage = new HashMap<String, Double>();
	}

	public void run(){ 
		while(!stopped){
			double time = getTime();
			// Cycles through all targets sending a packet of size "size" every "wait" time.
			if(time - lasttime > wait){
				if(which >= target.size()){
					which = 0;
				} else {
					if(!lastmessage.containsKey(target.get(which))){
						lastmessage.put(target.get(which), getTime());
					}
					double lasttime = lastmessage.get(target.get(which));
					if(getTime() - lasttime > wait*30){
						target.remove(which);
					}else{
						send(target.get(which), new byte[size]);
						which++;
					}
				}
				lasttime = time ;
			}
			// Add any node sending a message here to our targets.
			while(message_queue.peek() !=null){
				Message m = message_queue.poll();
				if(!target.contains(m.from)){
					target.add(m.from) ;
				}
				// Keep track of last time we got a message for removing targets.
				lastmessage.put(m.from, getTime());
			}
			try{Thread.sleep(2); } catch(InterruptedException e){}
		}
	}
}