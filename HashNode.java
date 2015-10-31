/* This is a basic distributed hashtable implementation to demonstrate practical use of the simulator.
 * It will work if you initialize all nodes before making any requests and keep all nodes online.
 * It's not fault tolerant(when a node goes offline its data is lost) and the work distribution is basically random.
 * It's also not particularly efficient (peer connections pay no attention to latency).
 * Don't use this algorithm for real applications.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HashNode extends Node{

	// All of the possible message types.
	static final byte REQUEST = 1; // A request for an object in the table.
	static final byte RESPONSE = 2;// A response of an object in the table.
	static final byte STOREREQUEST = 3;// A request to store an object in the table.
	static final byte PEERREQUEST = 4; // A broadcast to the entire network to generate peers for a new node
	static final byte PEERRESPONSE = 5; // The response of peers

	// Tree depth created by peer selecting algorithm.
	static final int PEERDEPTH = 3;

	// The position of the node from 0 to 1.
	float position;

	// Our list of peers in order from lowest to highest and satisfying the peer properties 
	// (may contain nulls and neighboring duplicates).
	// See getPeerTargets for generation. use peer_list for iterating over peers.
	Peer[] peer;
	ArrayList<Peer> peer_list ; // Same as peer, but no nulls or duplicates.

	HashMap<String, String> table; // Our subset of the distributed hashtable.
	HashMap<String, ArrayList<String>> pending_request; // Keep track of open requests(key -> ID's requesting).
	HashMap<Integer, PeerFan> pending_peer_request;// Keep track of open peer requests(requestID > pending request)
	int requestID = 0 ; // Number peer requests so we can tell them apart easily.

	// Initializes a HashNode wit the given address, key position, and initial network connection.
	// Also add the node to the network with the given position and rate, and send an initial peer request.
	public HashNode(String address, float pos, String connect, Network net, float x, float y, float rate){
		super(address);
		position = pos;
		peer = new Peer[PEERDEPTH*2+2];
		peer_list = new ArrayList<Peer>();
		table = new HashMap<String, String>();
		pending_request = new HashMap<String, ArrayList<String>>();
		pending_peer_request = new HashMap<Integer, PeerFan>();
		// Add to network.
		net.addNode(this, x, y, rate);
		// Request peer list.
		send(connect, makePeerRequest(address, pos, 0, 1, requestID));
		requestID++;
	}

	// Returns true if the node's peer_list has been initialized.
	public boolean ready(){
		return peer_list.size()!=0;
	}

	// This is the core method of the network. These nodes are purely event driven, so it just process received messages.
	public void run() {
		while(!stopped){
			// Constantly process received messages.
			while(message_queue.peek() != null){
				Message m = message_queue.poll();
				processMessage(m);
			} 
			try{Thread.sleep(2); } catch(InterruptedException e){}
		}
	}

	//Processes a message by passing to the relevant processing function by type.
	public void processMessage(Message m){
		// The first byte in the message says the type of message
		byte messagetype = m.message[0];
		if(messagetype == REQUEST){
			System.out.println( address +" got request from " + m.from);
			processRequest(m);
		}else if(messagetype == RESPONSE){
			System.out.println( address +" got response from " + m.from);
			processResponse(m);
		}else if(messagetype == STOREREQUEST){
			System.out.println( address +" got store request from " + m.from);
			processStoreRequest(m);
		}else if(messagetype == PEERREQUEST){
			//System.out.println( address +" got peer request from " + m.from);
			processPeerRequest(m);
		}else if(messagetype == PEERRESPONSE){
			System.out.println( address +" got peer response from " + m.from);
			processPeerResponse(m);
		}
	}

	// Processes a request to fetch an item from the table.
	private void processRequest(Message m) {
		String from = m.from;
		ByteStream stream = new ByteStream(m.message);
		stream.readByte();//Discard the type since we already checked it.
		String key = stream.readString();
		float keypos = getPosition(key);
		
		//Find the nearest node to th key among known nodes.
		float mindist = (float)Math.abs(position - keypos);
		String minid = address;
		for(int k=0; k<peer_list.size(); k++){
			Peer p = peer_list.get(k);
			float dist = (float)Math.abs(p.position - keypos);
			if(dist < mindist){
				mindist = dist;
				minid = p.address;
			}
		}

		// If this node is the nearest amongst its peers then it owns the object.
		if(address.equals(minid)){
			// Send the response with the value immediately.
			String value = table.get(key);
			byte[] response = makeResponse(key, value); // If null value, sends empty string.
			send(from, response);
		} else if(!pending_request.containsKey(key)) {
			// If not the owner of the object and we haven't already requested it, pass request to nearest keyed peer.
			send(minid, m.message);
			// Remember who sent it to us, so we know what to do with the response.
			ArrayList<String> requesters = pending_request.get(key);
			if(requesters == null){
				requesters = new ArrayList<String> ();
				requesters.add(from);
				pending_request.put(key, requesters);
			} else {
				requesters.add(from);
			}
		}
	}

	// Processes a response to a request to fetch an item from the table.
	private void processResponse(Message m) {
		ByteStream stream = new ByteStream(m.message);
		stream.readByte();//Discard the type since we already checked it.
		String key = stream.readString();
		// Pass the message to everyone who has requested it.
		ArrayList<String> requesters = pending_request.remove(key);
		if(requesters == null){
			System.err.println(address +" - Got a response it didn't ask for!");
		} else {
			for(int k=0;k<requesters.size();k++){
				send(requesters.get(k), m.message);
			}
		}
	}

	// Processes a request to store an item in the table
	private void processStoreRequest(Message m) {
		ByteStream stream = new ByteStream(m.message);
		stream.readByte();//Discard the type since we already checked it.
		String key = stream.readString();
		String value = stream.readString();
		float keypos = getPosition(key);
		//Find the nearest node among known nodes.
		float mindist = (float)Math.abs(position - keypos);
		String minid = address;
		for(int k=0; k<peer_list.size(); k++){
			Peer p = peer_list.get(k);
			float dist = (float)Math.abs(p.position - keypos);
			if(dist < mindist){
				mindist = dist;
				minid = p.address;
			}

		}

		// If this node owns the object.
		if(address.equals(minid)){
			// Save the value into its table.
			table.put(key, value);
		} else {
			// If not the owner of object pass request to nearest keyed peer.
			send(minid, m.message);
		}
	}

	// Processes a request to fetch peers for a new node.
	// Peers are always in order from lowest to highest.
	// This nodes will also consider adding the new node to its peer list.
	private void processPeerRequest(Message m) {
		String from = m.from;
		ByteStream stream = new ByteStream(m.message);
		stream.readByte();//Discard the type since we already checked it.
		String cid = stream.readString(); // ID of node requesting peers.
		float cpos  = stream.readFloat(); // Location of node requesting peers.
		// Range over which this node is responsible.
		float min = stream.readFloat();
		float max = stream.readFloat();
		int responseID = stream.readInt();
		System.out.println( address +" got peer request from " + m.from + "(" + min +"," + max+")");
		int requests_sent = 0 ;
		// Distribute the region over my peers.
		for(int k=0;k<peer_list.size();k++){
			float kmin = 0, kmax=1;
			float kpos = peer_list.get(k).position;
			// Get region this peer covers in the space
			if( k > 0 ){
				kmin = (peer_list.get(k-1).position + kpos)*.5f;
			}
			if(k < peer_list.size()-1){
				kmax = (peer_list.get(k+1).position + kpos)*.5f;
			}
			// Make sure it doesn't overlap the owned space of this node.
			if( kpos < position){
				kmax = Math.min(kmax, (position+kpos)*0.5f);
			} else {
				kmin = Math.max(kmin, (position+kpos)*0.5f);
			}
			// Intersect this peer's controlled region with the requested region.
			kmax = Math.min(kmax, max);
			kmin = Math.max(kmin, min);

			// If there's an overlap then we need to make a request to that peer.
			if(kmax > kmin ){
				System.out.println("   " + peer_list.get(k).address + " (" + kmin +","+kmax+")");
				byte krequest[] = makePeerRequest(cid, cpos, kmin, kmax, requestID);
				send(peer_list.get(k).address, krequest);
				requests_sent++;
			} else {
				System.out.println("   " + peer_list.get(k).address );
			}
		}
		
		// If we didn't distribute the request to any other nodes.
		if(requests_sent == 0) {
			// Respond with a peer list of just this node.
			send(from, makePeerResponse(cpos, PeerNominate(cpos, address, position), responseID));
		} else {
			// If we did fan out then we need to create a record of it and wait for our responses before we can respond.
			pending_peer_request.put(requestID, new PeerFan(from, cpos, requests_sent, responseID));
			requestID++;
		}
		// Potentially merge the new node into this node's peer list if it's a better fit.
		peer = mergePeers(position, peer, PeerNominate(position, cid, cpos));
		updatePeerList();
	}

	// Processes a response to a request to fetch peers for a new node.
	private void processPeerResponse(Message m) {
		ByteStream stream = new ByteStream(m.message);
		stream.readByte();// Discard the type since we already checked it.
		float cpos = stream.readFloat();
		int requestID = stream.readInt();
		// Read the returned peer list in the same way it's written.
		Peer p[] = new Peer[stream.readShort()]; 
		for(int k=0;k<p.length;k++){
			String pid = stream.readString();
			if(!pid.equals("")){
				float ppos = stream.readFloat();
				p[k] = new Peer(pid,ppos);
			}
		}

		PeerFan pf = pending_peer_request.get(requestID);
		if(pf == null){
			if(cpos == position){ // If this was this node's peer request.
				peer = p; // Use the returned peer list.
				updatePeerList();
			} else {
				System.err.println(address +" - Got a peer response it didn't ask for!");
			}
		} else { // If we have a pending peer request entry.
			boolean done = pf.handleResponse(p); // Merge the peer response into our response.
			if(done){ // If that was the last response we were waiting for.
				// Send our response.
				send(pf.from, makePeerResponse(cpos, pf.best_peers, pf.responseID));
				pending_peer_request.remove(cpos);

			}
		}
	}

	// A request to fetch an item from the table.
	public static byte[] makeRequest(String key){
		ByteStream stream = new ByteStream();
		stream.writeByte(REQUEST);
		stream.writeString(key);
		return stream.getBytes();
	}

	// A response to a request, returning an item from the table.
	public static byte[] makeResponse(String key, String value){
		ByteStream stream = new ByteStream();
		stream.writeByte(RESPONSE);
		stream.writeString(key);
		stream.writeString(value);
		return stream.getBytes();
	}

	// A request to store an item in the table.
	public static byte[] makeStoreRequest(String key, String value){
		ByteStream stream = new ByteStream();
		stream.writeByte(STOREREQUEST);
		stream.writeString(key);
		stream.writeString(value);
		return stream.getBytes();
	}

	// A request to fetch peers for the given node in the given range. requestNumber is a unique identifier for the request.
	public static byte[] makePeerRequest(String address, float position, float min, float max, int requestNumber){
		ByteStream stream = new ByteStream();
		stream.writeByte(PEERREQUEST);
		stream.writeString(address);
		stream.writeFloat(position);
		stream.writeFloat(min);
		stream.writeFloat(max);
		stream.writeInt(requestNumber);
		return stream.getBytes();
	}

	// A response of peers fetched for the given target. requestNumber is a unique identifier for the request.
	public static byte[] makePeerResponse(float target, Peer peer[], int requestNumber){
		ByteStream stream = new ByteStream();
		stream.writeByte(PEERRESPONSE);
		stream.writeFloat(target);
		stream.writeInt(requestNumber);
		stream.writeShort((short)peer.length); 
		for(int k=0;k<peer.length;k++){
			if(peer[k] == null){
				stream.writeString("");
			} else {
				stream.writeString(peer[k].address);
				stream.writeFloat(peer[k].position);
			}
		}
		return stream.getBytes();
	}

	//Syncs the peer_list to the peer array. Called every time peer is modified.
	public void updatePeerList(){
		peer_list = new ArrayList<Peer>();
		System.out.println(address +" updating peers:");
		for(int k=0; k<peer.length; k++){
			if( peer[k]!=null && ( k == 0 || !peer[k].equals(peer[k-1]))){ // Peer is nonnull non duplicate.
				peer_list.add(peer[k]);
				System.out.println("   " + peer[k].address);
			}
		}
	}

	// Keeps track of pending fanned out peer requests.
	private class PeerFan{
		String from; // Where we go the request from, not who made the root request.
		float cpos; // Position of the root requester.
		int requests; // Amount fo requests we made.
		int responses; // Amount fo responses we've gotten.
		Peer[] best_peers; // Current set of best peers for this request.
		int responseID; // A unique identifier to let the receiver know what we're responding to.

		public PeerFan(String from, float cpos, int fanout, int responseID){
			// Initialize with self as a peer
			best_peers = PeerNominate(cpos, address, position);
			this.from = from;
			this.requests = fanout;
			this.cpos = cpos;
			this.responseID = responseID;
			responses = 0 ;
		}

		// Merges peers into the best peer list.
		// Returns true when all responses have been received.
		public boolean handleResponse(Peer p[]){
			best_peers = mergePeers(cpos,best_peers,p);
			responses++;
			return responses >= requests ;
		}
	}

	// Returns the position of a message from a String key.
	public static float getPosition(String key){
		return (new Random(key.hashCode())).nextFloat();
	}

	// return 2 + depth*2 target values around center inthe range 0, 1.
	// The middle 2 values will both be center, but it's critical when generating peers to make sure they are < and > center.
	// The remaining values will be progressively closer to the center by powers of 2.
	// If depth is on the order of O(log(nodes)) then this makes it highly likely reads and writes will be on the order of O(log(nodes)).
	// This technique is a heuristic method I made up for this demo. Pretty sure it works though.
	public static float[] getPeerTargets(float center, int depth){
		float target[] = new float[2+depth*2];
		target[depth] = center;
		target[depth+1] = center;
		target[0] = center*0.5f;;
		target[target.length-1] = (1f+center)*.5f;
		for(int k=1; k<depth;k++){
			target[k] = (target[k-1] + center)*.5f;
			target[target.length-1-k] = (target[target.length-k] + center)*.5f;
		}
		return target;
	}

	// Merges a set of peers for a node centered at center. Assumes peers are in order (assuming center in the center).
	// Use PeerNominate to generate a Peer array from a single node.
	public static Peer[] mergePeers(float center, Peer[] A, Peer[] B){
		float target[] = getPeerTargets(center, (A.length-2)/2);
		Peer C[] = new Peer[A.length];
		for(int k=0;k<target.length;k++){
			// If one is null then the non-null one wins. Otherwise the closest to the target wins
			if(A[k] == null){
				C[k] = B[k];
			} else if(B[k] == null || Math.abs(A[k].position-target[k]) < Math.abs(B[k].position-target[k])){
				C[k] = A[k];
			} else {
				C[k] = B[k];
			}
		}
		return C;
	}

	// Create a potential peer list for a node at center containing the given peer in eligible slots.
	public static Peer[] PeerNominate(float center, String peerID, float peerposition){
		Peer C[] = new Peer[2+2*PEERDEPTH];
		Peer p = new Peer(peerID, peerposition);
		if(peerposition < center){ // If less then then put inall less than slots.
			for(int k=0;k<C.length/2;k++){
				C[k] = p;
			}
		} else if(peerposition > center){// If greater than then put in all greater than slots.
			for(int k=C.length/2;k<C.length;k++){
				C[k] = p;
			}
		}
		return C;
	}
	
	// Unit test for generating peer target positions.
	public static void main(String args[]){
		float target[] = getPeerTargets(.5f, 2);
		for(int k=0;k<target.length;k++){
			System.out.println(target[k]);
		}
	}
}

// Peers for the peer god. Addresses for the address throne.
class Peer{
	float position;
	String address;

	public Peer(String address){
		this.address = address;
	}
	
	public Peer(String address, float position){
		this.address = address;
		this.position = position;
	}

	public boolean equals(Object o){
		if(o == null){
			return false;
		} else {
			return address.equals(((Peer)o).address);
		}
	}
}