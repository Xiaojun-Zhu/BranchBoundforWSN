
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;



/*
 * Use ILP-FS-multi-commodity flow to solve the problem
 * 
 * */
public class AlgILPFSMFlow extends Solver{

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
		//bound obtained by relaxed ILP
		solver=new IloCplex();
		visitEdge();
		
		IloNumVar[] x=solver.numVarArray(numEdge,0,1);//x_ij in the model is x[e.num]
		IloNumVar[][] f=new IloNumVar[numEdge][];
		for(int i=0;i<numEdge;i++) {
			f[i]=solver.numVarArray(graph.size(), 0, 1);//f_ijk note that flow from k along edge ij is f[ij][k]
		}
				
		IloNumVar t=solver.numVar(0,Double.MAX_VALUE);//t
		solver.addMinimize(solver.prod(1,t));
		solver.setOut(new FileOutputStream("cplexlog.txt"));
		
		/*(1) sum_j x_ij=1 for all i\ne 0
		 *(2) for any k, and i\ne 0,  sum_j f_ijk=sum_j f_jik+[i=k]
		 *(3)for any k\ne 0,  sum_j f_j0k=1
		 *(4) f_ijk<=x_ij
		 *(5)for any i\ne 0,  sum_jk f_ijk- e_i*t/(Rx+Tx)<=Rx/(Rx+Tx)
		*/
		
		//constraint (1)
		for(int i=1;i<graph.size();i++) {
			IloLinearNumExpr ex=solver.linearNumExpr();
			DEdge e=graph.get(i).edge;
			while(e!=null) {
				ex.addTerm(1,x[e.num]);
				e=e.next;
			}
			solver.addEq(ex,1);
		}		
		//constraint (2)
		for(int k=1;k<graph.size();k++) {
			for(int i=1;i<graph.size();i++) {
				IloLinearNumExpr ex=solver.linearNumExpr();
				DEdge e=graph.get(i).edge;
				while(e!=null) {
					ex.addTerm(1,f[e.num][k]);
					e=e.next;
				}
				for(DEdge d:graph.get(i).inedges) {
					ex.addTerm(-1,f[d.num][k]);
				}
				solver.addEq(ex,i==k?1:0);
			}
		}		
		
		//constraint (3)
		for(int k=1;k<graph.size();k++) {
			IloLinearNumExpr ex=solver.linearNumExpr();
			for(DEdge e:graph.get(0).inedges) {
				ex.addTerm(1,f[e.num][k]);
			}
			solver.addEq(ex,1);
		}
		
		//constraint (4)
		for(int i=1;i<graph.size();i++) {
			DEdge e=graph.get(i).edge;
			while(e!=null) {
				for(int k=1;k<graph.size();k++)
					solver.addLe(f[e.num][k], x[e.num]);//
				e=e.next;
			}
		}
		
		//constraint (5)
		for(int i=1;i<graph.size();i++) {
			DEdge e=graph.get(i).edge;
			IloLinearNumExpr ex=solver.linearNumExpr();
			while(e!=null) {
				for(int k=1;k<graph.size();k++) {
					ex.addTerm(1,f[e.num][k]);
				}
				e=e.next;
			}
			ex.addTerm(-1*graph.get(i).E/(DNode.Rx+DNode.Tx), t);
			solver.addLe(ex,DNode.Rx/(DNode.Rx+DNode.Tx));
		}	
		
		solver.setParam(IloCplex.Param.Threads,1);		
		solver.solve();
		return 1/solver.getObjValue();		
	}


	private void visitEdge() {
		numEdge = 0;// the # of labeled edges
		for(DNode d:graph) {
			if(d.inedges==null) {
				d.inedges=new LinkedList<DEdge>();
			}else {
				d.inedges.clear();
			}
			
		}
		for (DNode d : graph) {
			if (d.id == 0)
				continue;
			d.numEdge = 0;
			DEdge e = d.edge;
			while (e != null) {
				e.num = numEdge;
				numEdge++;
				d.numEdge++;
				e.to.inedges.add(e);
				e = e.next;
			}
		}
	}


		
	public double ILPsolve() throws IloException, FileNotFoundException {
		//delete edges between neighbors of the sink
		this.status=0;
				
		visitEdge();
		
		IloIntVar[] x=solver.boolVarArray(numEdge);//x_ij in the model is x[e.num]
		IloNumVar[][] f=new IloNumVar[numEdge][];
		for(int i=0;i<numEdge;i++) {
			f[i]=solver.numVarArray(graph.size(), 0, 1);//f_ijk note that flow from k along edge ij is f[ij][k]
		}
				
		IloNumVar t=solver.numVar(0,Double.MAX_VALUE);//t
		solver.addMinimize(solver.prod(1,t));
		solver.setOut(new FileOutputStream("cplexlog.txt"));
		
		/*(1) sum_j x_ij=1 for all i\ne 0
		 *(2) for any k, and i\ne 0,  sum_j f_ijk=sum_j f_jik+[i=k]
		 *(3)for any k\ne 0,  sum_j f_j0k=1
		 *(4) f_ijk<=x_ij
		 *(5)for any i\ne 0,  sum_jk f_ijk- e_i*t/(Rx+Tx)<=Rx/(Rx+Tx)
		*/
		
		//constraint (1)
		for(int i=1;i<graph.size();i++) {
			IloLinearNumExpr ex=solver.linearNumExpr();
			DEdge e=graph.get(i).edge;
			while(e!=null) {
				ex.addTerm(1,x[e.num]);
				e=e.next;
			}
			solver.addEq(ex,1);
		}		
		//constraint (2)
		for(int k=1;k<graph.size();k++) {
			for(int i=1;i<graph.size();i++) {
				IloLinearNumExpr ex=solver.linearNumExpr();
				DEdge e=graph.get(i).edge;
				while(e!=null) {
					ex.addTerm(1,f[e.num][k]);
					e=e.next;
				}
				for(DEdge d:graph.get(i).inedges) {
					ex.addTerm(-1,f[d.num][k]);
				}
				solver.addEq(ex,i==k?1:0);
			}
		}		
		
		//constraint (3)
		for(int k=1;k<graph.size();k++) {
			IloLinearNumExpr ex=solver.linearNumExpr();
			for(DEdge e:graph.get(0).inedges) {
				ex.addTerm(1,f[e.num][k]);
			}
			solver.addEq(ex,1);
		}
		
		//constraint (4)
		for(int i=1;i<graph.size();i++) {
			DEdge e=graph.get(i).edge;
			while(e!=null) {
				for(int k=1;k<graph.size();k++)
					solver.addLe(f[e.num][k], x[e.num]);//
				e=e.next;
			}
		}
		
		//constraint (5)
		for(int i=1;i<graph.size();i++) {
			DEdge e=graph.get(i).edge;
			IloLinearNumExpr ex=solver.linearNumExpr();
			while(e!=null) {
				for(int k=1;k<graph.size();k++) {
					ex.addTerm(1,f[e.num][k]);
				}
				e=e.next;
			}
			ex.addTerm(-1*graph.get(i).E/(DNode.Rx+DNode.Tx), t);
			solver.addLe(ex,DNode.Rx/(DNode.Rx+DNode.Tx));
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
		solver.setParam(IloCplex.Param.MIP.Strategy.Search,1);//traditional bc strategy
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

