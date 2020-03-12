import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;



/*
 * Use ILP-FS to solve the problem
 * 
 * */
public class AlgILPFS extends Solver{

	IloCplex solver = null;

	int numEdge = -1;// for storing the number of directed edges
	int numNode=-1;

	public void compute_optimal() {
		start_time=System.nanoTime();
		try {
			solver=new IloCplex();
		} catch (IloException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		start_time=System.nanoTime();
		
	
		try {
			optlife=ILPsolve();
			solver.end();//release memory
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

	public double lp_solve() throws IloException, FileNotFoundException {
		//check the bound obtained from relaxed ILP
		solver=new IloCplex();
		numberEdge();
		
		IloNumVar[] x=solver.numVarArray(numEdge,0,1);//x_ij in the model is x[e.num]
		IloNumVar[] y=solver.numVarArray(numEdge, 0, graph.size());//y_ij is y[e.num]
		IloNumVar t=solver.numVar(0,Double.MAX_VALUE);//t
		solver.addMinimize(solver.prod(1,t));
		solver.setOut(new FileOutputStream("cplexlog.txt"));
		
		/*(1) sum_j x_ij=1 for all i\ne 0
		 *(2) sum_j y_ij -sum_j y_ji=1 for all i\ne 0
		 *(6) y_ij-n*x_ij<=0 for all ij
		 * (7) sum_j y_ij -(e_i*t)/(Tx+Rx)<=Rx/(Rx+Tx)		
		*/
		
		for(int i=1;i<graph.size();i++) {
			DNode node=graph.get(i);
			IloLinearNumExpr cons1=solver.linearNumExpr(),
					cons2=solver.linearNumExpr(),
					cons6=null,
					cons7=solver.linearNumExpr();
			DEdge e=node.edge;
			while(e!=null) {
				cons1.addTerm(1,x[e.num]);
				
				cons2.addTerm(1,y[e.num]);
				if(e.to.id!=0) {
					//not the sink, subtract y_ji					
					cons2.addTerm(-1,y[e.pair.num]);
				}
				
				cons6=solver.linearNumExpr();
				cons6.addTerm(1,y[e.num]);
				cons6.addTerm(-graph.size(), x[e.num]);
				solver.addLe(cons6, 0);
				
				cons7.addTerm(1,y[e.num]);				
				e=e.next;
			}
			cons7.addTerm(-node.E/(DNode.Tx+DNode.Rx), t);
			solver.addEq(cons1,1);
			solver.addEq(cons2, 1);
			solver.addLe(cons7, DNode.Rx/(DNode.Rx+DNode.Tx));			
		}
		
		solver.setParam(IloCplex.Param.Threads,1);	
		solver.solve();	
		return 1/solver.getObjValue();
	}


	private void numberEdge() {
		numEdge = 0;// the # of labeled edges
		for (DNode d : graph) {
			if (d.id == 0)
				continue;
			d.numEdge = 0;
			DEdge e = d.edge;
			while (e != null) {
				e.num = numEdge;
				numEdge++;
				d.numEdge++;
				e = e.next;
			}
		}
	}

	/* x_ij is x(e.num)
	 * y_ij is x(e.num+numEdge)
	 * t is x(2*numEdge+1) 
	 */

		
	public double ILPsolve() throws IloException, FileNotFoundException {
		//delete edges between neighbors of the sink
		this.status=0;
				
		numberEdge();
		
		IloIntVar[] x=solver.boolVarArray(numEdge);//x_ij in the model is x[e.num]
		IloNumVar[] y=solver.numVarArray(numEdge, 0, graph.size());//y_ij is y[e.num]
		IloNumVar t=solver.numVar(0,Double.MAX_VALUE);//t
		solver.addMinimize(solver.prod(1,t));
		solver.setOut(new FileOutputStream("cplexlog.txt"));
		
		/*(1) sum_j x_ij=1 for all i\ne 0
		 *(2) sum_j y_ij -sum_j y_ji=1 for all i\ne 0
		 *(6) y_ij-n*x_ij<=0 for all ij
		 * (7) sum_j y_ij -(e_i*t)/(Tx+Rx)<=Rx/(Rx+Tx)		
		*/
		
		for(int i=1;i<graph.size();i++) {
			DNode node=graph.get(i);
			IloLinearNumExpr cons1=solver.linearNumExpr(),
					cons2=solver.linearNumExpr(),
					cons6=null,
					cons7=solver.linearNumExpr();
			DEdge e=node.edge;
			while(e!=null) {
				cons1.addTerm(1,x[e.num]);
				
				cons2.addTerm(1,y[e.num]);
				if(e.to.id!=0) {
					//not the sink, subtract y_ji					
					cons2.addTerm(-1,y[e.pair.num]);
				}
				
				cons6=solver.linearNumExpr();
				cons6.addTerm(1,y[e.num]);
				cons6.addTerm(-graph.size(), x[e.num]);
				solver.addLe(cons6, 0);
				
				cons7.addTerm(1,y[e.num]);				
				e=e.next;
			}
			cons7.addTerm(-node.E/(DNode.Tx+DNode.Rx), t);
			solver.addEq(cons1,1);
			solver.addEq(cons2, 1);
			solver.addLe(cons7, DNode.Rx/(DNode.Rx+DNode.Tx));			
		}
		
		solver.setParam(IloCplex.Param.Threads,1);
		int rest_time=(int)(time_limit - (long) ((System.nanoTime()-start_time) * 1.0 / 1000000000));
		solver.setParam(IloCplex.Param.TimeLimit,rest_time);//time limit in seconds
		solver.setParam(IloCplex.Param.DetTimeLimit, 1000*rest_time);//cplex has a bug that ignores time limit in some cases
		//http://www-01.ibm.com/support/docview.wss?uid=swg1RS03137
		//tries to set a second time limit to fix this issue. In this machine, one second contains roughly 700-750 ticks. Use
		//1000 as an upper bound
		
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
		solver.setParam(IloCplex.Param.MIP.Strategy.File,2);//store node file on disk in case memory is out
		
		
		solver.solve();
		CplexStatus status=solver.getCplexStatus();
		this.optlife=1/solver.getObjValue();
		if(status==CplexStatus.Optimal) {
			this.status=2;
		}else if(status==CplexStatus.AbortTimeLim||status==CplexStatus.AbortDetTimeLim) {
			this.status=1;
		}else {
			System.out.println("weird status "+status);
		}
			
		return this.optlife;	

	}
}

