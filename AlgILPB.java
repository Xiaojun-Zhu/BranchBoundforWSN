import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;



public class AlgILPB extends Solver {
	//ArrayList<DNode> graph = null;
	IloCplex solver = null;

	int numEdge = -1;// for storing the number of directed edges

	ArrayList<DEdge> debugEdge = new ArrayList<DEdge>();

	private void numberEdge() {
		numEdge = 0;// the # of labeled edges
		for (DNode d : graph) {
			if (d.id == 0)
				continue;
			d.numEdge = 0;
			DEdge e = d.edge;
			while (e != null) {
				debugEdge.add(e);
				e.num = numEdge;
				numEdge++;			
				d.numEdge++;
				e = e.next;
			}
		}
	}

	IloIntVar[] x;
	IloNumVar[] y;

	public void formulateILP(double l) throws IloException, FileNotFoundException{

		solver=new IloCplex();
		solver.setOut(new FileOutputStream("cplexlog.txt"));
		solver.setParam(IloCplex.Param.Threads,1);		
		solver.setParam(IloCplex.Param.MIP.Tolerances.MIPGap,0);//set the gap tolerance, otherwise numerical instability problem arises.
		solver.setParam(IloCplex.Param.MIP.Tolerances.AbsMIPGap,0);		
		solver.setParam(IloCplex.Param.Preprocessing.Presolve,false);
		solver.setParam(IloCplex.Param.Preprocessing.Aggregator, 0);		
		solver.setParam(IloCplex.Param.Emphasis.MIP, IloCplex.MIPEmphasis.Optimality);
		solver.setParam(IloCplex.Param.MIP.Limits.EachCutLimit,0);
		solver.setParam(IloCplex.Param.MIP.Cuts.Gomory, -1);
		solver.setParam(IloCplex.Param.MIP.Cuts.LiftProj, -1);
		solver.setParam(IloCplex.Param.MIP.Cuts.Cliques, -1);
		solver.setParam(IloCplex.Param.MIP.Strategy.Search,1);//traditional bb strategy

		x=solver.boolVarArray(numEdge);
		y=solver.numVarArray(numEdge, 0, graph.size());

		/*
		 * (1) sum_j x_ij=1
		 * (2)sum_j y_ij-sum_j y_ji=1
		 * (3)x_ij<=y_ij
		 * (4) y_ij<= floor (e_i/l+Rx)/(Tx+Rx) * x_ij
		 * 
		 * */


		for (int i = 1; i < graph.size(); i++) {
			DNode node = graph.get(i);
			IloLinearNumExpr cons1=solver.linearNumExpr(),
					cons2=solver.linearNumExpr();

			
			double dcap = (node.E / l +DNode.Rx) / (DNode.Tx + DNode.Rx);
			int cap = (int) Math.floor(dcap);
			if (dcap - cap > 0.99999) {// numerical instability problem: 1.9999998
				cap++;
			}

			DEdge e = node.edge;
			while (e != null) {
				cons1.addTerm(1,x[e.num]);
				cons2.addTerm(1,y[e.num]);
				if (e.to.id != 0) {// no directed edge from the sink; finding
					cons2.addTerm(-1,y[e.pair.num]);
				}
				solver.addLe(x[e.num],y[e.num]);//(3)
				solver.addLe(y[e.num], solver.prod(dcap, x[e.num]));	//(4)
				e = e.next;
			}
			solver.addEq(cons1,1);//(1)
			solver.addEq(cons2,1);//(2)
		}
	}



	public void printCap(double l) {
		for (DNode d : graph) {
			if (d.id == 0)
				continue;
			System.out.println(" " + d.id + " " + Math.floor((d.E / l - DNode.Tx) / (DNode.Tx + DNode.Rx) + 1));
		}
	}

	public void printVariables() {
		for (DNode n : graph) {
			if (n.id == 0)
				continue;
			DEdge e = n.edge;
			while (e != null) {
				System.out.println(" " + e.from.id + " " + e.to.id + " " + e.num + " " + (e.num + numEdge));
				e = e.next;
			}
		}
	}


	public void compute_optimal() {
		start_time=System.nanoTime();
		try {
			optlife=binarySearch();
		} catch (FileNotFoundException | IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public double binarySearch() throws IloException, FileNotFoundException {
		// get the set of possible lifetimes

		numberEdge();


		int total = (graph.size() - 1) * (graph.size() - 1);
		double[] lifes = new double[total];
		int index = 0;
		for (int i = 1; i < graph.size(); i++) {
			DNode node = graph.get(i);
			for (int j = 0; j < graph.size() - 1; j++) {
				lifes[index] = node.E / (j * (DNode.Tx + DNode.Rx) + DNode.Tx);
				index++;
			}
		}
		Arrays.sort(lifes);

		int i = 0, j = total - 1, mid = (i + j) / 2;// L[i] is always feasible, L[j] is
		// always infeasible
		
		this.status=2;
		while (i < j - 1) {
			double l = lifes[mid];
			this.formulateILP(l);

			int rest_time=(int)(time_limit - (long) ((System.nanoTime()-start_time) * 1.0 / 1000000000));
			if(rest_time<=0) {					
				this.status=1;//suboptimal
				break;
			}
			solver.setParam(IloCplex.Param.TimeLimit,rest_time);//time limit in seconds			

			boolean feasible = solver.solve();
			CplexStatus cplex_status=solver.getCplexStatus();

			if(cplex_status==CplexStatus.AbortTimeLim) {					
				this.status=1;//suboptimal
				break;
			}

			if (feasible) {
				// L[mid] is feasible
				i = mid;
			} else{
				// L[mid] is infeasible
				j = mid;
			} 
			mid = (i + j) / 2;

			//reset solver
			solver.endModel();
		}
		solver.end();
		return lifes[i];

	}


}
