import java.util.ArrayList;

public abstract class Solver {

	long time_limit;//in seconds, time limit on the running time
	long start_time;//System.nanotime();
	int status=0;//1 suboptimal, 2 optimal
	ArrayList<DNode> graph;
	double optlife=0;
	void set_graph(ArrayList<DNode> nodes) {
		graph=GenerateReadWriteNetwork.make_a_copy(nodes);
	}
	abstract public void compute_optimal();
	public double get_optimal() {
		return optlife;
	}
	public void set_time_limit(long seconds) {
		time_limit=seconds;
	}
	public double get_status() {
		return status;
	}
}
