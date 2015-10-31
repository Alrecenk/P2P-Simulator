/* This is a JFrame test of the Network simulator using dumb nodes.
 * It shows how to initialize, edit, and run a network.
 * Nodes spawn and die at random and randomly select a list of targets to message(round robin).
 * Nodes will also add nodes they receive messages from to their target list.
 * When a node or link's capacity is reached it will turn red and being to slow down.
 */

import java.awt.* ;
import java.awt.event.* ;
import javax.swing.* ;
import java.awt.image.BufferedImage ;
import java.util.ArrayList;
import java.awt.image.BufferStrategy ;

public class GUINetworkTest extends JFrame
implements ActionListener,MouseListener
{
	private static final long serialVersionUID = 1L;
	private Container pane ;
	BufferedImage display;
	static int width=1024,height=768;
	BufferStrategy strategy ;

	Network net;
	
	// Initialize the JFrame.
	public static void main(String[] args){
		GUINetworkTest window = new GUINetworkTest();
		window.init() ;
		window.addWindowListener(new WindowAdapter()
		{ public void windowClosing(WindowEvent e) { System.exit(0); }});

		window.setSize(width, height);
		window.setVisible(true);
	}
	
	// Initialize the network and some random nodes.
	public void init(){
		pane = getContentPane();
		pane.addMouseListener(this);
		pane.requestFocus();
		Timer clock = new Timer(10, this); 
		clock.start();

		net = new Network(30, 0.01f);
		int amount=10;
		// Start with some random nodes.
		for(int k=0;k<amount;k++){
			ArrayList<String> target = new ArrayList<String>();
			if(k > 3){
				for(int j=0;j<3;j++){
					target.add(net.RandomNode());
				}
			}
			Node node = new TestNode(""+k, target, (float)(.1+Math.random()), 10);
			net.addNode(node, 
					(float)(300 + Math.sin(k * 2 * Math.PI / amount)*200), 
					(float)(300 + Math.cos(k * 2 * Math.PI / amount)*200),
					20);
		}
	}
	
	// Paint method is override to perform double buffering.
	public void paint(Graphics g){
		if(display==null){
			createBufferStrategy(2);
			strategy = getBufferStrategy();
			display = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}

		Graphics g2 = strategy.getDrawGraphics();
		paint2(g2);
		strategy.show();
	}
	
	// App specific drawing goes here to benefit from double buffering.
	public void paint2(Graphics g){
		g.setColor(Color.white);
		g.fillRect(0,0, width, height);
		net.draw(g,2f);
	}
	
	// The timer.
	public void actionPerformed(ActionEvent e ){
		// Kill nodes at random.
		if(Math.random() < 0.001){
			net.stop(net.RandomNode());
		}
		// Add nodes at random.
		if(Math.random() < 0.002){
			ArrayList<String> target = new ArrayList<String>();
			for(int j=0;j<3;j++){
				target.add(net.RandomNode());
			}
			Node node = new TestNode(""+(int)(Math.random()*10000), target, (float)(.1+Math.random()), 10);
			net.addNode(node, (float)(100 + Math.random()*800), (float)(100 + Math.random()*500),20);
			Thread t = new Thread(node);
			t.start();
		}
		repaint();
	}

	public void mousePressed(MouseEvent e){
		pane.requestFocus();
	}
	public void mouseClicked(MouseEvent e){
	}

	public void mouseReleased(MouseEvent e){
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}