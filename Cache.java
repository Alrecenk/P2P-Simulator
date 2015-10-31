/* This is an efficient implementation of a fixed size cache using Java generics.
 * It uses a least recently used eviction policy implemented via a combination hashtable/linked list data structure.
 * Written by Alrecenk October 2015 for no particular reason. Feel free to use for whatever.
 */

import java.util.HashMap;
import java.util.Random;

public class Cache<KeyType, RecordType>{

	// Example usage of generic cache.
	public static void main(String args[]){
		//Initialize integer to String cache with maximum size of 500.
		Cache<Integer, String> cache = new Cache<Integer, String>(500);
		int hits = 0, misses = 0;
		for(int k=0;k<100000;k++){
			// Caches perform well in uneven key distributions.
			float power = 1 ;
			for(int j=0;j<5;j++){
				power*=Math.random();
			}
			int key = (int)(power*10000); // Keys up to 9999 but more common in lower numbers.
			// Try to fetch key from cache.
			String value = cache.get(key);
			if(value == null){
				// If it wasn't in the cache then do the full operation and cache it.
				value = expensiveDeterministicOperation(key);
				cache.put(key, value);
				misses++;
			}else{
				hits++;
			}
		}
		
		System.out.println(cache); // Print out the keys in the cache
		System.out.println("Hits:" + hits +"   Misses:" + misses);
	}
	// An expensive but deterministic operation such as retrieving a file from a remote server.
	public static String expensiveDeterministicOperation(Integer i){
		Random r = new Random(i);
		String s = "";
		for(int k=0;k<50;k++){
			s+= (char)(r.nextInt(26) + (int)'a');
		}
		return s ;
	}
//------------Generic Cache Implementation Begins Here ---------
	// Hash table to fetch records for keys. 
	// CacheValue includes record and pointer to queue position.
	HashMap<KeyType,CacheValue> table; 
	// Doubly linked queue to keep track of least recently used item.
	QueueNode head, tail; 

	// Allowed capacity of cache and how much is already full.
	int capacity, filled = 0 ;

	public Cache(int size) {  
		capacity = size;
		table = new HashMap<KeyType,CacheValue>(2*size); // Double table size reduces hash collisions.
	}

	//Nodes for queue keeping track of item access order.
	private class QueueNode{
		QueueNode next ;
		QueueNode previous ;
		KeyType key;

		public QueueNode(KeyType k){
			key = k ;
		}

		void removeFromQueue(){
			if(this==head)head = next;
			if(this==tail)tail = previous;
			if(previous!=null)previous.next = next;
			if(next!=null)next.previous = previous;
			next = null;
			previous = null;
		}

		void removeFromTable(){
			table.remove(key);
		}
	}
	// Groups a queue node and a record, so they can be fetched from the hash table together.
	private class CacheValue{
		public CacheValue(RecordType r){
			record = r ;
		}
		QueueNode node;
		RecordType record;
	}

	// Fetch a Record from the cache or null if it is not present.
	// Keeps track of accesses for future evictions.
	public RecordType get(KeyType k){
		CacheValue c = table.get(k);
		if(c == null)return null;
		// If found move to head of queue.
		QueueNode n = c.node;
		n.removeFromQueue();
		n.next = head;
		if(head!=null)head.previous = n;
		head = n;
		return c.record;
	}

	// Puts a record in the cache. Replaces record with matching key if found.
	// If cache is full evicts least recently accessed item.
	public void put(KeyType k, RecordType r){
		//If item is in the cache just update its value.
		CacheValue d = table.get(k);
		if(d!=null){ 
			d.record = r;
		}else{
			// If not in the cache then add it.
			CacheValue c = new CacheValue(r);
			QueueNode n = new QueueNode(k);
			c.node = n;
			table.put(k,c);
			// Put the item at the head of the queue.
			n.next = head;
			if(head!=null)head.previous = n;
			head = n;
			// First item in queue is also the tail
			if(tail==null)tail = head; 
			filled++;
			//If the cache is full remove the item at the tail of the queue.
			if(filled > capacity){
				QueueNode t = tail.previous;
				tail.removeFromTable();
				tail.removeFromQueue();
				tail = t;
				filled--;
			}
		}
	}
	// Prints the keys for the items currently in the cache.
	public String toString(){
		QueueNode node = head;
		String s = "Keys:" + node.key;
		while(node != tail){
			node = node.next;
			if(node==null){
				break;
			}
			s+=", " + node.key;
		}
		return s ;
	}
}