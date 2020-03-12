import java.util.LinkedList;


public class DNode {

//common variables, used in both naive enumeration and our new algorithm 枚举和新算法
int id=-1;//node id
double x,y;//node location
double E;//initial energy 初始能量
DEdge edge;// incident edges
int tID;//for finding bridges in naive enumeration; and find blocks in  our new algorithm

int des=0;// computing lifetime in naive enumeration alg; and store the number of known descendants in the new algorithm

public static double Rx=3.33*Math.pow(10, -4),Tx=6.66*Math.pow(10, -4);//system parameters, energy consumption for transmitting and receiving one message




//for use in naive enumeration alg
static boolean unvisited=false;//for speeding up naive enumeration algorithm
boolean visited=false;//for use in naive enumeration algorithm
int group;//for finding cycles




//for use in our new algorithm
DNode parent=null;
boolean mark=false;//for various purposes 
int low=0;//for decomposing graph into blocks
boolean inspt=false;// whether in partial spanning tree

//for use in ILP
int numEdge=0;


//for use  to record incomming edges
LinkedList<DEdge> inedges=null;

public static void deleteEdge(DEdge e){
	if(e.previous==null){//the first edge for a node
		e.from.edge=e.next;
		if(e.next!=null) e.next.previous=null;
	}else{
		e.previous.next=e.next;
		if(e.next!=null){
			e.next.previous=e.previous;
		}
	}
}

public static void insertEdge(DEdge e){
	e.previous=null;
	if(e.from.edge==null){
		//no edge
		e.from.edge=e;
		e.next=null;//this bug was found at 2013-2-9 10:08
	}else{
	    e.from.edge.previous=e;
	    e.next=e.from.edge;
	    e.from.edge=e;
	}
}

public static double compute_lifetime(DNode v, DNode parent){
	//only used in the naive enumeration algorithm
	double l=0;
	v.des=0;
	DEdge e=v.edge;
	while(e!=null){
		if(e.to!=parent&&e.inSPT==true){
			double t=compute_lifetime(e.to,v);
			if(l<=0||t<l){l=t;}
			v.des+=e.to.des+1;
		}
		e=e.next;
	}
	double lx=v.E/(v.des*DNode.Rx+(v.des+1)*DNode.Tx);
	if(v.id!=0){
		if(l==0||lx<l){
			//no children
			l=lx;
		}
	}	
	return l;	
}


}
