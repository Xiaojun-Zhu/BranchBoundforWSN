
public class AlgCombineFSBB extends Solver{

	double time_ratio=0.1;
	
	@Override
	public void compute_optimal() {
		// TODO Auto-generated method stub
		AlgILPFS first_solver=new AlgILPFS();
		first_solver.set_graph(graph);
		first_solver.time_limit=(long)(time_limit*time_ratio);
		first_solver.compute_optimal();
		if(first_solver.status==2) {
			//optimal found
			status=2;
			this.optlife=first_solver.get_optimal();
			return;
		}
		
		//now use the second solver
		AlgBB second_solver=new AlgBB();
		second_solver.set_graph(graph);
		second_solver.time_limit=time_limit-first_solver.time_limit;
		if(first_solver.status==1) {
			second_solver.initial=first_solver.get_optimal();
		}
		
		second_solver.compute_optimal();
		
		if(second_solver.status==2) {
			status=2;
			this.optlife=second_solver.get_optimal();
			return;
		}
		
		status=second_solver.status;
		this.optlife=second_solver.get_optimal();		
		
	}

}
