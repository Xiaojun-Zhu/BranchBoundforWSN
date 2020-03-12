import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import ilog.cplex.IloCplex.UnknownObjectException;



public class AlgBB extends Solver{
	/*
	 * preconditions:  do not use the 'pair' field of the node. it may be invalid
	 * 
	 * */

	public static final double MIN_VALUE=0.0000001;
	PrintStream ps;

	boolean debug=false;
	enum COLOR{
		WHITE,//not visited
		GRAY,//visited but not finished
		BLACK//finished
	};

	IloCplex solver;

	public static ArrayList<DNode> copy_graph_without_pair_field(ArrayList<DNode> nodes) {
		ArrayList<DNode> newnodes = new ArrayList<DNode>(nodes.size());
		for (DNode n : nodes) {
			DNode nn = new DNode();
			nn.x = n.x;
			nn.y = n.y;
			nn.E = n.E;
			nn.id = n.id;
			nn.visited = DNode.unvisited;
			newnodes.add(nn);
		}
		for (DNode n : nodes) {
			DNode u = newnodes.get(n.id);
			DEdge ne = n.edge;
			while (ne != null) {
				DNode v = newnodes.get(ne.to.id);
				DEdge e = new DEdge();
				e.from = u;
				e.to = v;
				e.inSPT = false;
				e.previous = null;
				e.next = null;		
				e.pair=null;
				DNode.insertEdge(e);
				ne = ne.next;
			}
		}
		return newnodes;
	}


	//to use this function, make sure the inedge field is maintained correctly
	//list does not contain the sink. implemented by xzhu 2018-12-20
	// critical should be of the same size as nodes, list should be of size nodes.size()-1
	public  static int[]  findCriticalNode(ArrayList<DNode> nodes,int[] list,boolean[] critical) {


		COLOR [] color=new COLOR[nodes.size()];
		Arrays.fill(color, COLOR.WHITE);
		LinkedList<DNode> Q=new LinkedList<DNode>();

		if(critical==null) {
			System.out.println("critical cannot be null @findCriticalNode");
		}

		Arrays.fill(critical,false);

		int[] list_prime=new int[list.length];
		int cindex=0;
		color[0]=COLOR.BLACK;
		int in=0;
		LinkedList<DNode> U=new LinkedList<DNode>();
		while(in<list.length) {
			U.clear();Q.clear();
			int j=in;
			while(j<list.length&&Math.abs(nodes.get(list[in]).E-nodes.get(list[j]).E)<MIN_VALUE) {
				U.add(nodes.get(list[j]));
				j++;
			}
			in=j;
			double e_U=U.get(0).E;			
			for(DNode u:U) {
				color[u.id]=COLOR.GRAY;
				DEdge e=u.edge;
				while(e!=null) {
					if(color[e.to.id]==COLOR.BLACK) {
						break;
					}
					e=e.next;
				}				
				if(e!=null) {
					//adjacent to a black node
					critical[u.id]=true;
					Q.add(u);
					list_prime[cindex++]=u.id;					
				}
			}

			for(DNode u:Q) color[u.id]=COLOR.BLACK;

			while(!Q.isEmpty()) {
				DNode k=Q.remove();
				for(DEdge lk:k.inedges) {
					DNode l=lk.from;
					if(color[l.id]==COLOR.GRAY) {
						color[l.id]=COLOR.BLACK;
						l.E=e_U;
						list_prime[cindex++]=l.id;
						Q.add(l);
					}
				}
			}
		}

		if(cindex!=list_prime.length) {
			//the graph is disconnected, happens when redundant edges are not removed
			return null;
			
		}

		return list_prime;		
	}


	// borrow the visited field of an edge to indicate whether it is in the graph, true: yes  false: no
	// set the inedge field correctly at termination
	public static void deleteRedundantEdges(ArrayList<DNode> nodes) {		
		LinkedList<DEdge> R=new LinkedList<DEdge>();
		//delete edges from the sink

		while(nodes.get(0).edge!=null) {
			DNode.deleteEdge(nodes.get(0).edge);
		}
		//	GenerateReadWriteNetwork.printTopoogy(nodes);

		for(DNode i:nodes) {//indedge field
			i.mark=false;
			if(i.inedges==null) {
				i.inedges=new LinkedList<DEdge>();
			}else {
				i.inedges.clear();
			}
		}		
		for(DNode i:nodes) {//initialize inedge field
			DEdge e=i.edge;
			while(e!=null) {
				e.to.inedges.add(e);
				e.visited=true;//in the graph
				e=e.next;
			}
		}		

		ArrayList<DEdge> B=new ArrayList<DEdge>();
		for(DNode i:nodes) {
			if(i.id==0) continue;//skip the sink

			//B contains all edges entering the out-neighbor of i
			B.clear();
			DEdge e=i.edge;
			while(e!=null) {
				B.addAll(e.to.inedges);
				e=e.next;
			}			
			for(DEdge de:B) {
				de.visited=false;//not in the graph
			}

			//traverse the graph from the sink on the reverse direction of all edges
			LinkedList<DNode> Q=new LinkedList<DNode>();
			DNode sink=nodes.get(0);
			sink.mark=true;
			Q.add(sink);
			while(!Q.isEmpty()) {
				DNode v=Q.remove();
				for(DEdge de:v.inedges) {
					if(de.visited) {//consider only edges in the graph
						if(de.from.mark==false) {
							de.from.mark=true;
							Q.add(de.from);
						}						
					}
				}
			}

			e=i.edge;
			while(e!=null) {
				if(e.to.mark==false) {
					//e is redundant
					R.add(e);
				}
				e=e.next;				
			}
			//now reset the mark field
			for(DNode dn:nodes) {
				dn.mark=false;
			}
			//insert back all edges
			for(DEdge de:B) {
				de.visited=true;
			}

		}		

		//System.out.println(" ");
		//delete redundant edges
		for(DEdge de:R) {
			de.to.inedges.remove(de);//maintain the inedge field
			DNode.deleteEdge(de);
			//			System.out.println(de.from.id+" "+de.to.id);
		}
		//	System.out.println("num of deleted edges: "+R.size());

	}


	public long probID=0;
	class ProbInstance{
		long ID;//for display
		long parentID;//for display
		int type;//D1=1,D2=2,D3=3
		ArrayList<DNode> graph;
		int[] list;
		ProbInstance(ArrayList<DNode> g,int probtype, long pID){//not the first problem instance
			graph=g;
			type=probtype;
			parentID=pID;
			ID=probID++;

		}
		ProbInstance(ArrayList<DNode> g,int[] l){//the first problem instance
			graph=g;
			type=0;
			parentID=0;//its parent is itself
			list=l;
			probID=0;//initialize the global counter here
			ID=probID++;

		}
	}

	class InvLifeVal{
		int indexInQ;
		double value;		
		InvLifeVal(double b){

			value=b;
		}
	};

	

	public static int[] IDlistDescendingEnergy(ArrayList<DNode> nodes) {
		//sort nodes in descending order of energy
		ArrayList<DNode> nodeslist=new ArrayList<DNode>();
		nodeslist.addAll(nodes.subList(1,nodes.size()));//subList from 1(inclusive) to the last one (exlusive)

		nodeslist.sort(new Comparator<DNode>() {//in descending order of energy
			@Override
			public int compare(DNode arg0, DNode arg1) {
				// TODO Auto-generated method stub
				if(arg0.E>arg1.E)return -1;
				else if(arg0.E<arg1.E) return 1;
				else return 0;
			}

		});	

		int[] idlist=new int[nodeslist.size()];
		for(int i=0;i<nodeslist.size();i++) {
			idlist[i]=nodeslist.get(i).id;					
		}
		return idlist;

	}

	


	public double  BranchBound() throws UnknownObjectException, FileNotFoundException, IloException {
		//use the this.graph field to get the input

		double cur=0;//initial lifetime

		graph.get(0).E=GenerateReadWriteNetwork.max_energy;

		int[] idlist=IDlistDescendingEnergy(graph);
		double[] energylist=new double[idlist.length];
		for(int i=0;i<idlist.length;i++) {		
			energylist[i]=graph.get(idlist[i]).E;
		}

		/*this part is for avoiding sorting of Q' every time*/
		//construct M
		ArrayList<ArrayList<InvLifeVal>> M=new ArrayList<ArrayList<InvLifeVal>>();
		ArrayList<InvLifeVal> Q_prime=new ArrayList<InvLifeVal>();
		for(int i=0;i<idlist.length;i++) {
			ArrayList<InvLifeVal> Mi=new ArrayList<InvLifeVal>();
			for(int j=1;j<=idlist.length;j++) {//j can be equal to nodeslist.size()
				Mi.add(new InvLifeVal((j*(DNode.Rx+DNode.Tx)-DNode.Rx)/energylist[i]));
			}
			M.add(Mi);
			Q_prime.addAll(Mi);
		}

		Q_prime.sort(new Comparator<InvLifeVal>() {
			@Override
			public int compare(InvLifeVal arg0, InvLifeVal arg1) {
				// TODO Auto-generated method stub
				if(arg0.value>arg1.value)
					return 1;
				else if(arg0.value<arg1.value)
					return -1;
				else
					return 0;				
			}			
		});
		//now establish the inverse index
		for(int i=0;i<Q_prime.size();i++) {
			Q_prime.get(i).indexInQ=i;
		}
		boolean[] markindex=new boolean[Q_prime.size()];//this array is for marking the index, false:unmarked, true:marked
		/*part end*/


		LinkedList<ProbInstance> S=new LinkedList<ProbInstance>();
		//Stack<ProbInstance> S=new Stack<ProbInstance>();
		//initialize stack
		S.push(new ProbInstance(graph,idlist));
		//S.addFirst(new ProbInstance(graph,idlist));

		while(!S.isEmpty()) {

			if(time_limit < ((System.nanoTime()-start_time) * 1.0 / 1000000000)){
				//timeout
				break;
			}

			ProbInstance prob=S.pop();
			//ProbInstance prob=S.removeLast();
			deleteRedundantEdges(prob.graph);

			

			boolean[] critical=new boolean[prob.graph.size()];
			int[] list_prime=findCriticalNode(prob.graph,prob.list,critical);

			



			/*prepare the sorted Q'*/
			ArrayList<Double> q=new ArrayList<Double>();
			//mark indices
			Arrays.fill(markindex,false);
			int i=0;
			for(int dn:list_prime) {

				while(Math.abs(energylist[i]-prob.graph.get(dn).E)>0.00001) {
					i++;
				}

				for(InvLifeVal ilv:M.get(i)) {
					if(ilv.value<=1/cur+MIN_VALUE)//further optimization
						markindex[ilv.indexInQ]=true;
				}				
			}

			for(i=0;i<Q_prime.size();i++) {
				if(markindex[i]) { 
					q.add(Q_prime.get(i).value);
				}

			}			


			//				System.out.print("from q: ");
			//				for(double li:q) {
			//					System.out.print(" "+li);
			//				}
			//				System.out.println();
			//				
			//				System.out.print(" from localQ: ");
			//				for(double li:T) {
			//					System.out.print(" "+li);
			//				}
			//				System.out.println();		



			//prepare end
			/*//check wheter q is sorted in ascending order
			for(i=0;i<q.size()-1;i++) {
				if(q.get(i)>q.get(i+1)) {
					System.out.println("wrong");
				}
			}*/

			double[] x=bound_by_RFSI(prob,q,critical);//
			double l_prime=x[x.length-1];//the last one is the lifetime upper bound
			double l_T=0;

			//debug
			//	if(S.size()>=13) {
			//		GenerateReadWriteNetwork.printTopoogy(prob.graph);
			//	}

			if(l_prime>cur+0.000001) {//0.000001 is for numerical instability problem

				ArrayList<ProbInstance> newprobs=new ArrayList<ProbInstance>();

				l_T=const_and_branch(prob,x,newprobs);//obtain y from solver, newprobs is for getting the two new subproblems



				if(l_T>cur+0.000001) {
					cur=l_T;
				}
				if(l_T<=l_prime-0.000001) {// l_T is smaller than l_prime. Not finished, branch
					newprobs.get(0).list=list_prime;
					newprobs.get(1).list=list_prime;

					if(newprobs.size()==3) {
						newprobs.get(2).list=list_prime;
						S.push(newprobs.get(2));
					}
					S.push(newprobs.get(1));
					S.push(newprobs.get(0));


				}
			}

			if(debug) {
				ps.println("@("+prob.ID+","+prob.type+","+prob.parentID+"), upper bound "+l_prime+", feasible "+l_T+", cur="+cur+", stack size "+S.size());
			}


		}
		if(S.isEmpty()) {
			this.status=2;
		}else {
			this.status=1;
		}

		return cur;
	}




	private double improve_lifetime(ArrayList<DNode> graph2) {
		//precondition: already have a tree
		int[] childNum=new int[graph2.size()];
		int[] desNum=new int[graph2.size()];
		ArrayList<ArrayList<DNode>> childs=new ArrayList<ArrayList<DNode>>(graph2.size());
		for(int i=0;i<graph2.size();i++) {
			childs.add(new ArrayList<DNode>());			
		}
		for(int i=1;i<graph2.size();i++) {
			DNode n=graph2.get(i);
			int toID=n.parent.id;
			childs.get(toID).add(n);
			//			childs.set(i,new ArrayList<DNode>());			
		}



		boolean[] isbottleneck=new boolean[graph2.size()];

		double life=-1;
		boolean isimproved=true;
		ps.println("life improvement ");
		while(isimproved) {
			isimproved=false;
			Arrays.fill(isbottleneck,false);
			life=compute_lifetime(graph2,childNum,desNum);
			ps.print(" "+life+" ");
			for(int i=1;i<graph2.size();i++) {//mark botteneck node
				DNode t=graph2.get(i);
				double tlife=t.E/((DNode.Rx+DNode.Tx)*desNum[t.id]+DNode.Tx);
				if(tlife<=life+MIN_VALUE) {
					//bottleneck
					isbottleneck[i]=true;

					LinkedList<Integer> q=new LinkedList<Integer>();
					q.addFirst(i);
					while(!q.isEmpty()) {
						int cnode=q.removeLast();
						for(DNode c:childs.get(cnode)) {
							isbottleneck[c.id]=true;
							q.addFirst(c.id);
						}
					}					
				}
			}
			//try to switch the parent of a node whose parent is bottleneck

			for(int i=1;i<graph2.size();i++) {
				DNode cnode=graph2.get(i);
				if(isbottleneck[cnode.parent.id]) {
					//try to switch the parent of cnode
					DEdge e=cnode.edge;
					int offset=desNum[i]+1;
					while(e!=null) {
						if(!isbottleneck[e.to.id]) {
							//try to switch, not actually switch
							boolean hasimproved=true;
							DNode pnode=e.to;
							while(pnode.parent!=null) {
								//add cnode.des+1 to see the lifetime
								double cl=pnode.E/((DNode.Rx+DNode.Tx)*(desNum[pnode.id]+offset)+DNode.Tx);
								if(cl<life) {
									hasimproved=false;
									break;
								}else {
									pnode=pnode.parent;
								}								
							}
							if(hasimproved) {
								//update childs

								childs.get(cnode.parent.id).remove(cnode);
								childs.get(e.to.id).add(cnode);
								cnode.parent=e.to;
								ps.print(" improved to ");
								isimproved=true;
								break;								
							}							
						}
						e=e.next;
					}	

				}
				if(isimproved)break;

			}	

		}
		ps.println(" improve end ");	
		return life;
	}

	private double compute_lifetime(ArrayList<DNode>graph2,int[] childNum,int[] desNum) {
		//according to the parent field
		if(childNum==null)childNum=new int[graph2.size()];
		if(desNum==null) desNum=new int[graph2.size()];
		Arrays.fill(childNum,0);
		Arrays.fill(desNum, 0);
		Queue<DNode> queue=new LinkedList<DNode>();
		//compute lifetime of the data gathering tree
		for(DNode n:graph2) {
			if(n.parent!=null) {
				childNum[n.parent.id]++;
			}
		}
		for(int ind=0;ind<childNum.length;ind++) {
			if(childNum[ind]==0) {
				//leaf nodes
				queue.add(graph2.get(ind));
			}
		}
		double life=-1;
		while(!queue.isEmpty()) {
			DNode t=queue.poll();
			if(t.parent==null) {
				//t is the sink
				break;
			}
			double tlife=t.E/((DNode.Rx+DNode.Tx)*desNum[t.id]+DNode.Tx);
			if(life<=-1||tlife<=life) {
				life=tlife;
			}
			childNum[t.parent.id]--;
			desNum[t.parent.id]+=desNum[t.id]+1;
			//			t.parent.des+=t.des+1;
			if(childNum[t.parent.id]==0) {
				queue.offer(t.parent);
			}			
		}
		return life;		
	}



	private double const_and_branch(ProbInstance prob, double[] x,ArrayList<ProbInstance> newprobs) {
		// TODO Auto-generated method stub
		//note, the flow on an edge with num n is x[n-1]

		ArrayList<DNode> graph2=prob.graph;
		//	if(debug) {
		//		ps.println("original bound is "+x[x.length-1]);
		//	}
		/*remove acyclic flow begin*/
		ArrayList<DEdge> deletedEdges=new ArrayList<DEdge>();

		for(DNode n:graph2) {
			DEdge e=n.edge;
			while(e!=null) {
				if(x[e.num]<=0.000001) {
					deletedEdges.add(e);
				}
				e=e.next;
			}
			n.inedges.clear();
		}

		for(DEdge e:deletedEdges) {
			DNode.deleteEdge(e);
		}

		for(DNode n:graph2) {//establish the in-edge field
			DEdge e=n.edge;
			while(e!=null) {
				e.to.inedges.add(e);
				e=e.next;
			}
		}

		//now we get the induced graph of positive flow		
		COLOR[] color=new COLOR[graph2.size()];
		Arrays.fill(color, COLOR.WHITE);//initialize color

		Stack<DNode> stack=new Stack<DNode>();
		ArrayList<DEdge> P=new ArrayList<DEdge>();
		for(int i=0;i<graph2.size();i++) {
			DNode n=graph2.get(i);
			if(color[n.id]!=COLOR.WHITE) continue; //only consider white vertices

			color[n.id]=COLOR.GRAY;
			stack.push(n);

			while(!stack.isEmpty()) {
				DNode j=stack.peek();
				if(j.edge==null) {
					//dead end
					color[j.id]=COLOR.BLACK;
					for(DEdge de:j.inedges) {//no need to consider the inedges field
						deletedEdges.add(de);
						DNode.deleteEdge(de);
					}
					stack.pop();
					if(P.size()>=1)P.remove(P.size()-1);
				}else {
					DEdge jk=j.edge;
					DNode k=jk.to;
					if(color[k.id]==COLOR.WHITE) {//advance
						color[k.id]=COLOR.GRAY;
						stack.push(k);
						P.add(jk);
					}else {//find a cycle
						double min=x[jk.num];//flow from j to k
						int index=P.size()-1;
						while(index>=0) {
							DEdge ce=P.get(index);
							if(min<x[ce.num]) {
								min=x[ce.num];
							}
							if(ce.from==k) {
								break;
							}
							index--;
						}

						if(index<0)System.out.println("error P does not contain a cycle");

						DNode xn=j;						
						x[jk.num]-=min;
						if(Math.abs(x[jk.num])<=0.00001) {
							//delete this edge
							deletedEdges.add(jk);
							DNode.deleteEdge(jk);		
							jk.to.inedges.remove(jk);//in case it will be deleted again when k becomes dead end
						}

						index=P.size()-1;
						while(index>=0) {
							DEdge ce=P.get(index);
							x[ce.num]-=min;
							if(Math.abs(x[ce.num])<=0.000001) {
								deletedEdges.add(ce);
								DNode.deleteEdge(ce);		
								ce.to.inedges.remove(ce);//in case it will be deleted again when k becomes dead end
								xn=ce.from;
							}
							if(ce.from==k) {
								break;
							}
							index--;
						}

						while(stack.peek()!=xn) {
							DNode tn=stack.pop();
							color[tn.id]=COLOR.WHITE;
							P.remove(P.size()-1);							
						}
					}
				}

			}			
		}

		for(DEdge de:deletedEdges) {		
			DNode.insertEdge(de);
		}

		//remove acyclic flow end */



		int[] childNum=new int[graph2.size()];
		Arrays.fill(childNum,0);

		//detect whether we get a tree
		DNode i=null;
		int count=0;//number of out-edges
		int second_key=0;//if out number is the same, compare this one, total out
		for(DNode dn:graph2) {
			if(dn.id==0)continue;//skip sink
			DEdge e=dn.edge;
			int localoutedge=0;//record # of out-edges of node dn
			int localoutflow=0;
			DEdge treeEdge=e;//for the feasible data gathering tree
			dn.des=0;
			while(e!=null) {
				if(x[e.num]>=0.000001) {
					//positive
					localoutedge++;
					localoutflow+=(int)x[e.num];
				}
				if(x[e.num]>x[treeEdge.num]+0.000001) {
					treeEdge=e;//find the edge with most flow
				}
				e=e.next;
			}		
			treeEdge.from.parent=treeEdge.to;
			childNum[treeEdge.to.id]++;
			if((localoutedge>count)
					||( (localoutedge==count)&&(localoutflow>second_key) )) {
				count=localoutedge;
				second_key=localoutflow;
				i=dn;//find the node with the most number of out-edges in the induced graph
			}
		}
		if(count==1) {
			//the graph is a tree		
			return x[x.length-1];//lifetime is the same as that from RFSI
		}

		Queue<DNode> queue=new LinkedList<DNode>();
		//compute lifetime of the data gathering tree
		for(int ind=0;ind<childNum.length;ind++) {
			if(childNum[ind]==0) {
				//leaf nodes
				queue.add(graph2.get(ind));
			}
		}
		double life=-1;
		while(!queue.isEmpty()) {
			DNode t=queue.poll();
			if(t.parent==null) {
				//t is the sink
				break;
			}
			double tlife=t.E/((DNode.Rx+DNode.Tx)*t.des+DNode.Tx);
			if(life<=-1||tlife<=life) {
				life=tlife;
			}
			childNum[t.parent.id]--;
			t.parent.des+=t.des+1;
			if(childNum[t.parent.id]==0) {
				queue.offer(t.parent);
			}			
		} //evaluating the const function 
		//	life=improve_lifetime(graph2);
		//life is the lifetime of the data gathering tree
		//we find a vertex i
		ArrayList<DEdge> D1=new ArrayList<DEdge>(),D2=new ArrayList<DEdge>(),D3=new ArrayList<DEdge>();
		int total_out=0,max_out=0;
		DEdge max_edge=null;
		ArrayList<DEdge> posEdge=new ArrayList<DEdge>();

		DEdge e=i.edge;
		while(e!=null) {
			if(x[e.num]<=0.000001) {
				//zero
				D3.add(e);
			}else {
				total_out+=(int)x[e.num];
				posEdge.add(e);
				if(x[e.num]>max_out) {
					max_out=(int)x[e.num];
					max_edge=e;
				}
			}
			DEdge te=e;
			e=e.next;
			DNode.deleteEdge(te);
		}//delete all out-edges of i 



		int best_cum_flow=0;
		boolean [][] f=new boolean [posEdge.size()][total_out+1];

		if(debug)ps.println("max_out "+max_out+" total_out "+total_out+" max num of out edges "+count);
		if(max_out>=total_out/2.0) {
			//special case, no need to find groups: one group contains max_edge, the other does not
			D1.add(max_edge);
			posEdge.remove(max_edge);
			D2.addAll(posEdge);			
		}else {
			//now divide the edges into two groups


			for(int j=0;j<posEdge.size();j++) {
				Arrays.fill(f[j],false);
				int cflow=(int)x[posEdge.get(j).num];
				if(j==0) {
					f[j][cflow]=true;
					f[j][0]=true;
					continue;
				}
				for(int k=0;k<total_out+1;k++) {			
					f[j][k]=f[j-1][k]||(k>=cflow&&f[j-1][k-cflow]);		
				}
			}

			//find the smallest flow greater than or equal to tota_out/2

			for(int k=total_out/2;k<total_out;k++) {
				if(f[posEdge.size()-1][k]) {
					best_cum_flow=k;
					break;
				}
			}

			if(best_cum_flow==0)System.out.println("error best cum flow =0");
			//now track down the best value
			int restFlow=best_cum_flow;
			for(int j=posEdge.size()-1;j>=0;j--) {
				int cflow=(int)x[posEdge.get(j).num];
				if( (j-1>=0&&restFlow>=cflow&&f[j-1][restFlow-cflow])||(j==0&&restFlow==cflow)) {
					D1.add(posEdge.get(j));
					restFlow-=cflow;
				}else {
					//not include this edge in D1, insert it into D2
					D2.add(posEdge.get(j));
				}

			}
		}
		if(debug)ps.print("D1 sub, keep:");
		for(DEdge de:D1) {
			DNode.insertEdge(de);
			if(debug)ps.print("("+de.from.id+" ,"+de.to.id+")");
		}
		if(debug)ps.println();
		ProbInstance pi=new ProbInstance(AlgBB.copy_graph_without_pair_field(graph2),1,prob.ID);
		newprobs.add(pi);
		for(DEdge de:D1) {
			DNode.deleteEdge(de);			
		}


		if(debug)ps.print("D2 sub, keep:");
		for(DEdge de:D2) {
			DNode.insertEdge(de);
			if(debug)ps.print("("+de.from.id+" ,"+de.to.id+")");
		}		
		if(debug)ps.println();
		pi=new ProbInstance(AlgBB.copy_graph_without_pair_field(graph2),2,prob.ID);
		newprobs.add(pi);
		for(DEdge de:D2) {
			DNode.deleteEdge(de);			
		}		


		if(D3.size()!=0) {
			if(debug)ps.print("D3 sub, keep:");
			for(DEdge de:D3) {
				DNode.insertEdge(de);
				if(debug)ps.print("("+de.from.id+" ,"+de.to.id+")");
			}
			pi=new ProbInstance(AlgBB.copy_graph_without_pair_field(graph2),3,prob.ID);
			newprobs.add(pi);//no need to restore the graph2 structure, it will no longer be used
			if(debug)ps.println();
		}


		return life;
	}


	private double[] bound_by_RFSI(ProbInstance prob,ArrayList<Double>Q,boolean[] critical) throws UnknownObjectException, IloException, FileNotFoundException{
		// TODO Auto-generated method stub

		int low=0,high=Q.size()-1;

		/*prepare LP Solve
		 * */
		int label=0;
		for(DNode n:prob.graph) {
			n.numEdge=0;//out edges
			if(n.inedges==null)
				n.inedges=new LinkedList<DEdge>();
			else
				n.inedges.clear();
		}
		for(DNode n:prob.graph){
			DEdge e=n.edge;			
			while(e!=null){
				n.numEdge++;//record the number of outgoing edges
				e.num=label;
				label++;
				e.to.inedges.add(e);
				e=e.next;
			}
		}

		this.solver.endModel();;


		/* minimize z
		 * (2) sum_j y_ij-sum_j y_ji=1 for i\ne 0
		 * (3) for i in critical, sum_j y_ij  -z<= floor(e_i*t+Rx)/(Tx+Rx)
		 * (4) for i not critical, sum_j y_ij - z<=floor( (e_i'*t+Rx)/(Tx+Rx)-1 )
		 */
		IloNumVar[] y=solver.numVarArray(label, 0, graph.size());//y_ij is y[e.num]
		IloNumVar z=solver.numVar(-Double.MAX_VALUE,Double.MAX_VALUE);//t
		solver.addMinimize(solver.prod(1,z));
		solver.setOut(new FileOutputStream("cplexlog.txt"));

		solver.setParam(IloCplex.Param.Threads,1);//single threaded, for fairness
		solver.setParam(IloCplex.Param.MIP.Tolerances.MIPGap,0);//set the gap tolerance, otherwise numerical instability problem arises.
		solver.setParam(IloCplex.Param.MIP.Tolerances.AbsMIPGap,0);

		solver.setParam(IloCplex.Param.Preprocessing.Presolve,false);
		solver.setParam(IloCplex.Param.Preprocessing.Aggregator, 0);


		//the first |V|-1 constraints: sum_j y_ij-sum_j y_ji=1

		IloConstraint[] cons3s=new IloConstraint[prob.graph.size()-1];
		IloLinearNumExpr[] numexpr3s=new IloLinearNumExpr[prob.graph.size()-1];

		for(int i=1;i<prob.graph.size();i++) {
			DNode node=prob.graph.get(i);
			int numInEdge=node.inedges.size();

			IloLinearNumExpr cons2=solver.linearNumExpr();
			numexpr3s[i-1]=solver.linearNumExpr();

			DEdge e=node.edge;
			while(e!=null) {
				cons2.addTerm(1, y[e.num]);
				numexpr3s[i-1].addTerm(1, y[e.num]);				
				e=e.next;
			}

			numexpr3s[i-1].addTerm(-1,z);

			for(DEdge te:node.inedges) {
				cons2.addTerm(-1,y[te.num]);
			}
			solver.addEq(cons2,1);			
		}			

		int []rhs=new int[prob.graph.size()-1];
		while(low<high-1) {

			int mid=(int)Math.floor((low+high)/2.0);
			//set t=Q[mid]
			double candidate=Q.get(mid);
			for(int i=1;i<prob.graph.size();i++) {
				double lrh= (prob.graph.get(i).E*candidate+DNode.Rx)/(DNode.Rx+DNode.Tx);
				if(!critical[i]) lrh-=1; //RFSI+
				//		System.out.print("("+lrh+"->"+(int)(lrh+MIN_VALUE)+", "+Math.floor(lrh)+") ");	

				if(cons3s[i-1]!=null) {
				//	solver.remove(cons3s[i-1]);
				//	cons3s[i-1].
					solver.delete(cons3s[i-1]);
				}
				rhs[i-1]=(int)(lrh+MIN_VALUE);
				cons3s[i-1]=solver.addLe(numexpr3s[i-1],rhs[i-1]);			
			}
			//	System.out.println();



			
			boolean feasible=solver.solve();
			CplexStatus status=solver.getCplexStatus();		


			double z_value=solver.getValue(z);
			if(z_value<=MIN_VALUE) {
				high=mid;
			}else {
				low=mid;
			}
			//	System.out.println("("+Q.get(low)+","+Q.get(high)+") (");

			double Bmin=-1,Bmax=-1;
			for(int i=1;i<prob.graph.size();i++) {
				double e_i=prob.graph.get(i).E;
				double val=((rhs[i-1]+z_value)*(DNode.Rx+DNode.Tx)-DNode.Rx)/e_i;						

				double val2= ((rhs[i-1]+ Math.ceil(z_value-MIN_VALUE) )*(DNode.Rx+DNode.Tx)-DNode.Rx)/e_i;
				if(Bmin==-1||val<Bmin) Bmin=val;
				if(Bmax==-1||val2>Bmax) Bmax=val2;				
			}

			while(low+1<high&&Q.get(low+1)<Bmin) {
				low++;			
			}

			while(low+1<high&&Q.get(high)>Bmax) {
				high--;
			}		
			//		System.out.println(""+1/Q.get(low)+", "+1/Q.get(high)+")");
		}

		//optimal one is Q'[high]
		double candidate=Q.get(high);	

		//fix the solver		
		for(int i=1;i<prob.graph.size();i++) {
			double lrh= (prob.graph.get(i).E*candidate+DNode.Rx)/(DNode.Rx+DNode.Tx);
			if(!critical[i]) lrh-=1; //RFSI+
			if(cons3s[i-1]!=null) {
				solver.remove(cons3s[i-1]);
			}			
			cons3s[i-1]=solver.addLe(numexpr3s[i-1],(int)(lrh+MIN_VALUE));				
		}

		solver.solve();
		double []x=new double[label+1];
		for(int i=0;i<label;i++) {
			x[i]=solver.getValue(y[i]);			
		}

		double z_value=solver.getValue(z);
		if(z_value>MIN_VALUE) {
			//is infeasible
			//	if(debug) ps.println("x[label-1] "+x[label-1]);
			x[label]=0;

		}else {
			x[label]=1/candidate;
		}			

		return x;
	}

	public void compute_optimal() {
		start_time=System.nanoTime();
		try {
			solver=new IloCplex();
		} catch (IloException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if(debug)
			try {			
				ps=new PrintStream(new FileOutputStream("DebugAlgBBEE.txt"),true);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


		optlife=-1;

		try {
			optlife=this.BranchBound();
		} catch (FileNotFoundException | IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
