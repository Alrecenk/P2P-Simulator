/* This is a test of the basic distributed hash table implemented by HashNode.
 * It's also a good example of how to use the simulator in general. 
 * This test will add hashNodes until max nodes is reached, then it will add a client node you can send requests from.
 * See keyPressed for all controls.
 */

import java.awt.* ;
import java.awt.event.* ;
import javax.swing.* ;
import java.awt.image.BufferedImage ;
import java.awt.image.BufferStrategy ;

public class GUIHashNodeTest extends JFrame
implements ActionListener,MouseListener, KeyListener
{
	// JFrame variables.
	private static final long serialVersionUID = 1L;
	private Container pane ;
	BufferedImage display;
	static int width=900,height=900;
	BufferStrategy strategy ;

	// Simulated Network variables.
	Network net;
	int nodes = 0 ;
	int maxnodes = 50;
	Node lastnode; 
	ClientNode client = null;
	String typed = "";
	String lasttyped = "";
	String result = "Once the blue client node appears. Send \"key\" to fetch or \"key>value\" to store.";

	boolean pausecreation = false;

	// Main starts up the JFrame.
	public static void main(String[] args){
		GUIHashNodeTest window = new GUIHashNodeTest();
		window.init() ;
		window.addWindowListener(new WindowAdapter()
		{ public void windowClosing(WindowEvent e) { System.exit(0); }});

		window.setSize(width, height);
		window.setVisible(true);
	}

	//Initialize UI listeners and the network.
	public void init(){
		pane = getContentPane();
		pane.addMouseListener(this);
		pane.addKeyListener(this);
		pane.requestFocus();
		Timer clock = new Timer(10, this); 
		clock.start();

		// Unlimited link capacity and no packet drop chance, since this table implementation doesn't consider those problems.
		net = new Network(99999999, 0.001f, .1f, 0, 100f, 12345);
		// Go ahead and start with 2 nodes because 1 node initializes immediately.
		addNode();
		addNode();
	}

	// Adds a node randomly in a circle around (450,450) and connects it to a random node in the network.
	public void addNode(){
		float pos = (float)(Math.random());
		double s = 1 - Math.random()*.5;
		float x = (float)(450 + Math.sin(nodes*Math.PI + nodes*2*Math.PI/maxnodes)*400*s), y = (float)(450 + Math.cos(nodes*Math.PI + nodes*2*Math.PI/maxnodes)*400*s);
		String connect = net.RandomNode();
		lastnode = new HashNode("ID-"+pos, pos, connect, net, x, y, 9999999);
		nodes++;
		System.out.println("Added node:" + lastnode.address);
	}

	// Paint method is override to perform double buffering.
	public void paint(Graphics g){
		if(display==null){
			createBufferStrategy(2);
			strategy = getBufferStrategy();
			display = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		}

		Graphics g2 = strategy.getDrawGraphics();
		g2.drawImage(display,0,0,this);
		paint2(g2);
		strategy.show();
	}

	// App specific drawing goes here to benefit from double buffering.
	public void paint2(Graphics g){
		g.setColor(Color.white);
		g.fillRect(0,0, width, height);

		net.draw(g, 2f);

		g.setColor(Color.black);
		g.drawString(typed, 50, 850);
		g.drawString(result, 50, 865);
		g.drawString("left/right = change speed, up = last command, down = clear command" , 50, 880);
		g.drawString("Nodes: " + nodes +" / " + maxnodes , 50, 70);
		g.drawString("Speed: " + String.format("%4.2f", net.time_speed), 650, 70);
	}

	// The timer adds nodes and checks for client responses.
	public void actionPerformed(ActionEvent e ){

		// Create node when previous node is done initializing.
		if(((HashNode)lastnode).ready() && nodes < maxnodes && !pausecreation){
			addNode();
		}

		// Create client node when all nodes are initialized.
		if(((HashNode)lastnode).ready() && nodes == maxnodes && client == null){
			client = new ClientNode("client");
			net.addNode(client,450,450,9999999);
		}

		// Print and display responses received by the client.
		if(client!=null){
			Message m = client.receive();
			if(m!=null){
				ByteStream stream = new ByteStream(m.message);
				byte type = stream.readByte();
				if(type == HashNode.RESPONSE){
					String key = stream.readString();
					String value = stream.readString();
					result = "Response from " + m.from +" : " + key +" -> " + value;
					System.out.println("Client got a response from " + m.from +" : " + key +" - > " + value);
				} else {
					System.err.println("Client got an unknown message!");
				}
			}
		}

		repaint();
	}

	// keyboard controls.
	public void keyPressed(KeyEvent e){
		int t = e.getKeyCode() ;
		if(t == KeyEvent.VK_BACK_SPACE && typed.length()>0){
			typed = typed.substring(0,typed.length()-1); // Back space.
		} else if(t == KeyEvent.VK_ENTER && client!=null){
			int split = typed.indexOf('>');
			String server = net.RandomNode();
			while(server.equals(client.address))server = net.RandomNode();
			if(split < 0){ // Submit a look up request if no ">" in command.
				client.send(server, HashNode.makeRequest(typed));
				result = "Requested:" + typed; 
			} else { // Submit a store request if ">" in command.
				String key = typed.substring(0,split);
				String value = typed.substring(split+1,typed.length());
				result = "Requested store:" + typed;
				client.send(server, HashNode.makeStoreRequest(key, value));
			}
			lasttyped = typed;
			typed = "";
		} else if(t == KeyEvent.VK_LEFT){ // Slow down time.
			net.time_speed /= (float)Math.sqrt(2);
		} else if(t == KeyEvent.VK_RIGHT){ // Speed up time.
			net.time_speed *= (float)Math.sqrt(2);
		} else if(t == KeyEvent.VK_UP){ // Get last submitted command.
			typed = lasttyped;
		} else if(t == KeyEvent.VK_DOWN){ // Clear command.
			typed= "";
		} else if(t == KeyEvent.VK_CAPS_LOCK){ // pause creating new nodes.
			pausecreation = !pausecreation;
		} else { // Otherwise put characters into typed.
			char c = e.getKeyChar() ;
			if(c == ' '){
				typed+='_'; // Spaces in keys can be confusing.
			}
			if(Character.isAlphabetic(c) || Character.isDigit(c) || c=='>'){
				typed+=c;
			}
		}
	}

	public void keyTyped(KeyEvent e){
	}

	public void keyReleased(KeyEvent e){
	}

	public void mousePressed(MouseEvent e){
		pane.requestFocus();
	}
	public void mouseClicked(MouseEvent e){
	}

	public void mouseReleased(MouseEvent e){
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {		
	}
}