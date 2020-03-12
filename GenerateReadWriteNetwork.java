import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class GenerateReadWriteNetwork {
	static final double radius=20;//communication radius 20m
	static final double side_length=100;//100*100 square
	static final double max_energy=10;//maximum possible energy

	private static ArrayList<DNode> generate_a_sensor_network(int n){  //n=total number of nodes (including the sink)
		double l=side_length;
		ArrayList<DNode> gn=new ArrayList<DNode>();
		DNode sink=new DNode();
		sink.id=0;
		sink.x=l/2;
		sink.y=l/2;
		sink.E=0;
		gn.add(sink);
		for(int i=1;i<n;i++){
			DNode g=new DNode();
			g.id=i;
			g.x=Math.random()*l;
			g.y=Math.random()*l;		
			g.E=1+Math.random()*9;
			g.visited=DNode.unvisited;
			gn.add(g);
		}

		//System.out.println("here"+gn.size());
		for(int i=0;i<n;i++){
			DNode u=gn.get(i);
			for(int j=i+1;j<n;j++){
				DNode v=gn.get(j);
				double dist=Math.sqrt((u.x-v.x)*(u.x-v.x)+(u.y-v.y)*(u.y-v.y));
				if(dist<=radius){
					DEdge e=new DEdge();e.from=u;e.to=v;e.inSPT=false;e.previous=null;e.next=null;DNode.insertEdge(e);
					DEdge e2=new DEdge();e2.from=e.to;e2.to=e.from;e2.inSPT=false;e2.previous=null;e2.next=null;
					e2.pair=e;e.pair=e2;DNode.insertEdge(e2);		
				}
			}
		}

		if(is_connected(sink,gn)){
			return gn;
		}else{
			return null;
		}	
	}

	private static boolean is_connected(DNode s,ArrayList<DNode>ns){
		DFS(s);
		boolean b=true;
		for(DNode d:ns){
			if(d.visited==DNode.unvisited){
				if(b)b=false;
			}else{
				d.visited=DNode.unvisited;
			}
		}
		return b;
	}
	public static void DFS(DNode v){
		v.visited=!DNode.unvisited;
		DEdge e=v.edge;
		while(e!=null){
			if(e.to.visited==DNode.unvisited){
				DFS(e.to);
			}
			e=e.next;
		}
	}

	private static ArrayList<DNode> generate_connected_sensor_network(int n){	
		ArrayList<DNode> nodes=null;
		while((nodes=generate_a_sensor_network(n))==null);	
		return nodes;
	}

	public static void store_samples(String fileName,int[] numsOfnodes,int numPer) {
		//generate numPer*numOfnodes.length networks
		FileOutputStream f ;PrintStream ps;
		try {
			f=new FileOutputStream(fileName, true);
			ps=new PrintStream(f);
			int label=0;
			for(int i=0;i<numsOfnodes.length;i++){
				int numNodes=numsOfnodes[i];
				for(int j=0;j<numPer;j++) {
					ArrayList<DNode> nodes=generate_connected_sensor_network(numNodes);	
					int dedge_count = 0;//number of directed edges
					for(DNode d:nodes){				
						DEdge e=d.edge;					
						while(e!=null){
							dedge_count++;					
							e=e.next;
						}
					}
					ps.println(label+" "+nodes.size()+" "+dedge_count+" "+DNode.Tx+" "+DNode.Rx);//first line; label numofnodes numofdirectededges Tx  Rx				

					//the second line, the energy of all nodes
					for(DNode g:nodes)
						ps.print(""+g.E+" ");
					ps.println();
					//the third line, edge| assuming undirected graph					
					for(DNode d:nodes){				
						DEdge e=d.edge;					
						while(e!=null){						
							if(e.to.id>e.from.id){
								ps.print(""+e.from.id+" "+e.to.id+" ");								
							}
							e=e.next;
						}
					}
					System.out.println(""+label+": "+nodes.size()+" "+dedge_count);
					label++;
					ps.println();
					//the fourth line, location; 
					for(DNode d:nodes){
						ps.print(""+d.x+" "+d.y+" ");
					}
					ps.println();				
					ps.flush();							
				}		

			}		
			ps.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}

	ArrayList<DNode> cgraph=null;
	int cID=-1,n=-1,numDedge=-1;

	BufferedReader in;


	public boolean prepareRead(String filename) {
		try {
			in= new BufferedReader(new FileReader(filename));
			cgraph=null;
			cID=-1;
			numDedge=-1;
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public static ArrayList<DNode> make_a_copy(ArrayList<DNode> inputnodes){
		//for undirected graph
		ArrayList<DNode> newnodes = new ArrayList<DNode>(inputnodes.size());
		for (DNode n : inputnodes) {
			DNode nn = new DNode();
			nn.x = n.x;
			nn.y = n.y;
			nn.E = n.E;
			nn.id = n.id;
			nn.visited = DNode.unvisited;
			newnodes.add(nn);
		}
		for (DNode n : inputnodes) {
			DNode u = newnodes.get(n.id);
			DEdge ne = n.edge;
			while (ne != null) {
				if (ne.to.id > ne.from.id) {
					DNode v = newnodes.get(ne.to.id);
					DEdge e = new DEdge();
					e.from = u;
					e.to = v;
					e.inSPT = false;
					e.previous = null;
					e.next = null;
					DEdge e2 = new DEdge();
					e2.from = e.to;
					e2.to = e.from;
					e2.inSPT = false;
					e2.previous = null;
					e2.next = null;
					e2.pair = e;
					e.pair = e2;
					DNode.insertEdge(e);
					DNode.insertEdge(e2);
				}
				ne = ne.next;
			}
		}
		return newnodes;
	}
	
	public ArrayList<DNode> make_a_copy(){
		return make_a_copy(cgraph);
	}

	public ArrayList<DNode> nextNetwork(){
		String s;
		try {
			s = in.readLine();

			if(s==null)
				return null;
			String[] t = s.split("\\s+");
			cID=Integer.parseInt(t[0]);
			n=Integer.parseInt(t[1]);
			numDedge=Integer.parseInt(t[2]);
			DNode.Tx=Double.parseDouble(t[3]);
			DNode.Rx=Double.parseDouble(t[4]);

			String energy, edge, pos;

			energy = in.readLine();
			edge = in.readLine();
			pos = in.readLine();


			t = energy.split("\\s+");
			ArrayList<DNode> gr = new ArrayList<DNode>(t.length);
			for (int i = 0; i < t.length; i++) {
				DNode g = new DNode();
				g.E = Double.parseDouble(t[i]);
				g.id = i;
				gr.add(g);
			}
			t = edge.split("\\s+");
			for (int i = 0; i < t.length; i += 2) {
				DNode u = gr.get(Integer.parseInt(t[i])), v = gr.get(Integer.parseInt(t[i + 1]));
				DEdge e = new DEdge();
				e.from = u;
				e.to = v;
				e.inSPT = false;
				e.previous = null;
				e.next = null;
				DNode.insertEdge(e);
				DEdge e2 = new DEdge();
				e2.from = e.to;
				e2.to = e.from;
				e2.inSPT = false;
				e2.previous = null;
				e2.next = null;
				e2.pair = e;
				e.pair = e2;
				DNode.insertEdge(e2);
			}

			//set positions  
			t = pos.split("\\s+");
			for(int i=0;i<t.length;i+=2){
				//	System.out.println(t[i]);
				gr.get(i/2).x=Double.parseDouble(t[i]);
				gr.get(i/2).y=Double.parseDouble(t[i+1]);
			}
			return gr;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}	
	}



	public static void printTopoogy(ArrayList<DNode> graph) {
		for(DNode node:graph) {
			System.out.println(node.x+" "+node.y+" ");
		}
		
		for(DNode node:graph) {
			DEdge e=node.edge;
			while(e!=null) {
				System.out.println(e.from.id+" "+e.to.id);
				e=e.next;
			}
		}
	}
	public static void printTopoogy(ArrayList<DNode> graph,PrintStream ps) {
		for(DNode node:graph) {
			ps.println(node.x+" "+node.y+" "+(node.mark?node.x:node.E));
		}
		
		for(DNode node:graph) {
			DEdge e=node.edge;
			while(e!=null) {
				ps.println(e.from.id+" "+e.to.id);
				e=e.next;
			}
		}
	}
	
	public static void printTopoogy(ArrayList<DNode> graph,double[]flow,PrintStream ps) {
		for(DNode node:graph) {
			ps.println(node.x+" "+node.y);
		}
		
		for(DNode node:graph) {
			DEdge e=node.edge;
			while(e!=null) {
				if(flow[e.num-1]>0.0001)
				ps.println(e.from.id+" "+e.to.id+" "+flow[e.num-1]);
				e=e.next;
			}
		}
	}
	

	public static void main(String[] args) {
		int [] nums= {30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};		
		String filename="networks50nodes100networks.txt";
		int numPer=20;
		store_samples(filename,nums,numPer);

	}




}
