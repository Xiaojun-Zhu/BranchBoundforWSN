
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;


import ilog.concert.IloException;



public class EvalScript {
	public static final long time_limit_in_seconds=10*60;//10 minutes

	public static ArrayList<DNode> make_a_copy(ArrayList<DNode> cgraph){
		//this is for undirected graph
		ArrayList<DNode> newnodes = new ArrayList<DNode>(cgraph.size());
		for (DNode n : cgraph) {
			DNode nn = new DNode();
			nn.x = n.x;
			nn.y = n.y;
			nn.E = n.E;
			nn.id = n.id;
			nn.visited = DNode.unvisited;
			newnodes.add(nn);
		}
		for (DNode n : cgraph) {
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



	public static class MyThread extends Thread{
		Solver solver;
		public MyThread(Solver s) {
			solver=s;
		}
		public void run() {
			solver.compute_optimal();
		}
	}

	public static void run_one_method_one_network(Solver solver,int cID,PrintStream ps) {
		MyThread th=new MyThread(solver);				
		long startTime = System.nanoTime();
		th.start();

		try {
			th.join(time_limit_in_seconds * 1000+120*1000);//wait for 120 more seconds for ILP solver to stop
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}// wait for a fixed time, 10 minutes in our experiments

		if (th.isAlive()) {// time out
			while(th.isAlive())	th.stop();			
		}
		// finished within time ; write to file the actual time
		long e4 = System.nanoTime() - startTime;

		String str=cID+" "+solver.get_optimal()+" "+(e4 * 1.0 / 1000000000)+" "+solver.get_status();
		System.out.println(str);
		ps.println(str+"\r\n");
		ps.flush();



	}



	public static void evaluate_fs() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("EvalFS10mNewSetting.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			Solver solver=new AlgILPFS();
			solver.set_time_limit(time_limit_in_seconds+10);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
			
				if(grwn.cID>239)continue;
				solver.set_graph(cgraph);
				run_one_method_one_network(solver,grwn.cID,ps);		
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void evaluate_BB() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("EvalBB10m.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			Solver solver=new AlgBB();
			solver.set_time_limit(time_limit_in_seconds);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
			
				if(grwn.cID>239)continue;
				solver.set_graph(cgraph);
				run_one_method_one_network(solver,grwn.cID,ps);		
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void evaluate_Combine() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("EvalComb10m.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			Solver solver=new AlgCombineFSBB();
			solver.set_time_limit(time_limit_in_seconds);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
			
				
				solver.set_graph(cgraph);
				run_one_method_one_network(solver,grwn.cID,ps);		
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void evaluate_ILPB() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("EvalILPB10m.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			Solver solver=new AlgILPB();
			solver.set_time_limit(time_limit_in_seconds);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
		
				if(grwn.cID>239)continue;
				solver.set_graph(cgraph);
				run_one_method_one_network(solver,grwn.cID,ps);		
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void evaluate_ILPBD() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("EvalILPBD10m.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			Solver solver=new AlgILPBD();
			solver.set_time_limit(time_limit_in_seconds);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
		
				if(grwn.cID>217)continue;
				solver.set_graph(cgraph);
				run_one_method_one_network(solver,grwn.cID,ps);		
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void evaluate_ILPFSMFlow() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("EvalILPFSMFlow10m.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			Solver solver=new AlgILPFSMFlow();
			solver.set_time_limit(time_limit_in_seconds);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
	
				if(grwn.cID>239)continue;
				solver.set_graph(cgraph);
				run_one_method_one_network(solver,grwn.cID,ps);		
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void compare_bounds_of_mcf() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("CompareBounds.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			AlgILPFS fs=new AlgILPFS();
			AlgILPFSMFlow mcf=new AlgILPFSMFlow();	

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	

				String s;
				fs.set_graph(cgraph);
				mcf.set_graph(cgraph);
				s=grwn.cID+" "+fs.lp_solve()+" "+mcf.lp_solve();
				System.out.println(s);
				ps.println(s);
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void study_edge_removal_constraints_onFs() {
		FileOutputStream f;
		PrintStream ps;

		try {
			f = new FileOutputStream("CompareEdgeRemovalConstraints.txt", true);
			ps = new PrintStream(f);		

			GenerateReadWriteNetwork grwn=new GenerateReadWriteNetwork();
			grwn.prepareRead("networks30to100.txt");
			AlgILPFSEdgeRandCons onlye=new AlgILPFSEdgeRandCons();
			onlye.type=1;
			onlye.set_time_limit(time_limit_in_seconds);
			
			AlgILPFSEdgeRandCons onlyc=new AlgILPFSEdgeRandCons();
			onlyc.type=2;
			onlyc.set_time_limit(time_limit_in_seconds);
			AlgILPFSEdgeRandCons both=new AlgILPFSEdgeRandCons();
			both.type=3;
			both.set_time_limit(time_limit_in_seconds);

			ArrayList<DNode> cgraph=null;
			while ( (cgraph=grwn.nextNetwork())!= null) {	
		
				if(grwn.cID>239)continue;
				
				onlye.set_graph(cgraph);
				run_one_method_one_network(onlye,grwn.cID,ps);
				
				onlyc.set_graph(cgraph);
				run_one_method_one_network(onlyc,grwn.cID,ps);
				
				both.set_graph(cgraph);
				run_one_method_one_network(both,grwn.cID,ps);				
				
			}
			ps.close();	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
			throws NumberFormatException, IOException,  InterruptedException {
		
	//	System.out.println("hello world");
		evaluate_Combine();
//	    evaluate_BB();
//		evaluate_fs();
//		evaluate_ILPB();
//		evaluate_ILPBD();

	}
}
