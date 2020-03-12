import java.util.*;

public class AlgEnum extends Solver{

	//ArrayList<DNode> graph = null;


	int sptcount = 0;

	public void compute_optimal() {
		sptcount = 0;
		DNode sink = graph.get(0);
		double minE = graph.get(1).E;
		for (int i = 2; i < graph.size(); i++) {
			if (graph.get(i).E < minE)
				minE = graph.get(i).E;
		}
		sink.parent = null;
		sink.E = graph.size() * (1 + DNode.Rx / DNode.Tx) * minE;
		DEdge e = sink.edge;
		while (e != null) {
			e.inSPT = true;
			e.pair.inSPT = true;
			e.to.parent = sink;
			e.to.inspt = true;
			DEdge te = e.to.edge;
			while (te != null) {
				DEdge tmp = te.next;
				if (!te.inSPT && te.to.inspt) {
					DNode.deleteEdge(te);
					DNode.deleteEdge(te.pair);
				}
				te = tmp;
			}
			e = e.next;
		}
		optlife = decomp(sink);		
	}

	double decomp(DNode s) {
		double life = -1;
		LinkedList<LinkedList<DEdge>> blocks = new LinkedList<LinkedList<DEdge>>();
		LinkedList<DNode> sinks = new LinkedList<DNode>();
		LinkedList<Integer> extra = new LinkedList<Integer>();
		decompose(s, blocks, sinks, extra);

		Iterator<LinkedList<DEdge>> b_ite = blocks.iterator();
		Iterator<DNode> s_ite = sinks.iterator();
		while (b_ite.hasNext()) {
			double l;
			LinkedList<DEdge> b = b_ite.next();
			DNode bsink = s_ite.next();
			if (b.size() == 1) {
				// a bridge, compute lifetime
				sptcount++;
				DNode a = b.get(0).to;
				l = a.E / (a.des * DNode.Rx + (a.des + 1) * DNode.Tx);
			} else {
				// insert edges in b into the graph
				for (DEdge e : b) {
					DNode.insertEdge(e);
					DNode.insertEdge(e.pair);
				}
				if (bsink == s) {
					// call try
					l = trySolve(bsink);

				} else {
					// Procedure Grow
					LinkedList<DEdge> B1 = new LinkedList<DEdge>(), B2 = new LinkedList<DEdge>();
					LinkedList<DNode> queue = new LinkedList<DNode>();
					queue.add(bsink);
					DNode bp = bsink.parent;
					boolean bin = bsink.inspt;
					bsink.parent = null;
					bsink.inspt = true;

					while (!queue.isEmpty()) {
						DNode x = queue.removeLast();
						DEdge ne = x.edge;
						while (ne != null) {
							if (ne.to.E >= bsink.E && ne.to != x.parent) {// the
																			// check
																			// of
																			// parent
																			// is
																			// necessary
																			// for
																			// preventing
																			// deadlock
								queue.addFirst(ne.to);
							}
							if (!ne.to.inspt) {
								// add ne into spt
								ne.inSPT = true;
								ne.pair.inSPT = true;
								ne.to.inspt = true;
								ne.to.parent = x;
								B1.add(ne);
								// now all edges leading to cycles
								DEdge xe = ne.to.edge;
								while (xe != null) {
									DEdge tmp = xe.next;
									if (!xe.inSPT && xe.to.inspt) {
										DNode.deleteEdge(xe);
										DNode.deleteEdge(xe.pair);
										B2.add(xe);
									}
									xe = tmp;
								}
							}
							ne = ne.next;
						}
					}
					// now finished growing X
					if (B2.size() == 0) {
						// no deleting
						l = trySolve(bsink);
					} else {
						l = decomp(bsink);
					}

					// restore the graph
					for (DEdge te : B1) {
						// remove B1 from the tree
						te.inSPT = false;
						te.pair.inSPT = false;
						te.to.inspt = false;
						te.to.parent = null;
					}
					for (DEdge te : B2) {
						// insert te back
						DNode.insertEdge(te);
						DNode.insertEdge(te.pair);
					}
					bsink.inspt = bin;
					bsink.parent = bp;
				}
				// delete edges in b
				for (DEdge e : b) {
					DNode.deleteEdge(e);
					DNode.deleteEdge(e.pair);
				}
			}
			if (life < 0 || l < life) {
				life = l;
			}

		}

		// now restore the graph
		for (LinkedList<DEdge> es : blocks) {
			for (DEdge e : es) {
				DNode.insertEdge(e);
				DNode.insertEdge(e.pair);
			}
		}
		// restore number of descendants of the sinks
		s_ite = sinks.iterator();
		Iterator<Integer> d_ite = extra.iterator();
		while (s_ite.hasNext()) {
			s_ite.next().des -= d_ite.next();
		}
		return life;
	}

	private double trySolve(DNode s) {
		// TODO Auto-generated method stub
		// find an edge
		DEdge e = null;
		LinkedList<DNode> queue = new LinkedList<DNode>();
		queue.addFirst(s);
		while (!queue.isEmpty() && e == null) {
			DNode v = queue.removeLast();
			e = v.edge;
			while (e != null) {
				if (e.to != v.parent && e.inSPT) {
					queue.add(e.to);
				} else if (!e.inSPT) {
					break;
				}
				e = e.next;
			}
		}
		queue.clear();

		double life = -1;
		// try including e
		e.inSPT = true;
		e.pair.inSPT = true;
		e.to.inspt = true;
		e.to.parent = e.from;
		LinkedList<DEdge> B = new LinkedList<DEdge>();// one B is enough

		// delete redundant edges;B1
		DEdge te = e.to.edge;
		while (te != null) {
			DEdge tmp = te.next;
			if (!te.inSPT && te.to.inspt) {
				DNode.deleteEdge(te);
				DNode.deleteEdge(te.pair);
				B.add(te);
			}
			te = tmp;
		}

		// find edges adjacent to both e.from and e.to; B2
		te = e.from.edge;
		while (te != null) {
			if (!te.inSPT) {
				te.to.mark = true;
			}
			te = te.next;
		}

		te = e.to.edge;
		while (te != null) {
			DEdge tmp = te.next;
			if (!te.inSPT && te.to.mark) {
				DNode.deleteEdge(te);
				DNode.deleteEdge(te.pair);
				B.add(te);
				te.to.mark = false;
			}
			te = tmp;
		}

		te = e.from.edge;
		while (te != null) {
			if (te.to.mark)
				te.to.mark = false;
			te = te.next;
		}

		// find closest poor ancestor; it is the one among ancestors with the
		// least energy
		DNode v = e.from;
		DNode cpa = e.to;
		double min = cpa.E;
		while (v != null) {
			if (v.E <= min) {
				cpa = v;
				min = v.E;
			}
			v = v.parent;
		}
		v = cpa;
		if (v != e.to) {
			// this means e.to is a rich node, and v is the closest poor
			// ancestor
			// mark all descendants that are already in the tree
			queue.add(v);
			while (!queue.isEmpty()) {
				DNode tv = queue.removeLast();
				DEdge tte = tv.edge;
				while (tte != null) {
					if (tte.to != tv.parent && tte.inSPT) {
						queue.addFirst(tte.to);
						tte.to.mark = true;
					}
					tte = tte.next;
				}
			}

			// now delete all edges between nodes connecting to marked nodes if
			// it is already connected to e.to
			DEdge eeto = e.to.edge;
			while (eeto != null) {
				if (!eeto.inSPT) {
					DNode etox = eeto.to;
					DEdge xx = etox.edge;
					while (xx != null) {
						DEdge ttmp = xx.next;
						if (xx.to.mark && xx.to != e.to) {
							// delete xx
							DNode.deleteEdge(xx);
							DNode.deleteEdge(xx.pair);
							B.add(xx);
						}
						xx = ttmp;
					}
				}
				eeto = eeto.next;
			}

			// now reset the marks
			queue.add(v);
			while (!queue.isEmpty()) {
				DNode tv = queue.removeLast();
				DEdge tte = tv.edge;
				while (tte != null) {
					if (tte.to != tv.parent && tte.inSPT) {
						queue.addFirst(tte.to);
						tte.to.mark = false;
					}
					tte = tte.next;
				}
			}
		}

		if (B.size() > 0) {
			// some edges are deleted
			life = decomp(s);
		} else {
			life = trySolve(s);
		}
		// now restore the graph
		e.inSPT = false;
		e.pair.inSPT = false;
		e.to.inspt = false;
		e.to.parent = null;
		for (DEdge ee : B) {
			DNode.insertEdge(ee.pair);
			DNode.insertEdge(ee);
		}

		// the second case
		DNode.deleteEdge(e);
		DNode.deleteEdge(e.pair);
		double l = decomp(s);
		// restore
		DNode.insertEdge(e);
		DNode.insertEdge(e.pair);
		if (l > life)
			life = l;
		return life;
	}

	private void decompose(DNode s, LinkedList<LinkedList<DEdge>> blocks,
			LinkedList<DNode> sinks, LinkedList<Integer> extra) {
		// TODO Auto-generated method stub
		LinkedList<DNode> stack = new LinkedList<DNode>();
		// initialize time of each node
		stack.add(s);
		while (!stack.isEmpty()) {
			DNode v = stack.removeLast();
			v.tID = -1;
			DEdge ve = v.edge;
			while (ve != null) {
				if (ve.to.tID != -1) {
					stack.addFirst(ve.to);
				}
				ve = ve.next;
			}
		}

		LinkedList<DEdge> edgeStack = new LinkedList<DEdge>();

		// now begin the process
		// initialize s
		int time = 0;
		s.tID = time;
		s.low = s.tID;
		stack.addLast(s);
		time = time + 1 + s.des;
		while (!stack.isEmpty()) {
			if (stack.getLast().edge != null) {
				DEdge e = stack.getLast().edge;
				DNode.deleteEdge(e);
				DNode.deleteEdge(e.pair);
				edgeStack.addLast(e);
				if (e.to.tID >= 0) {
					// already visited, update lowest reachable ancestor
					if (e.to.tID < stack.getLast().low) {
						stack.getLast().low = e.to.tID;
					}
				} else { // a new node
					e.to.low = e.to.tID = time;
					time = time + 1 + e.to.des;
					stack.addLast(e.to);
				}
			} else {
				// the top node has no edge connected
				DNode v = stack.removeLast();
				if (stack.isEmpty())
					break;

				if (v.low >= stack.getLast().tID) {
					// a new block is found
					LinkedList<DEdge> nblock = new LinkedList<DEdge>();

					stack.getLast().des += time - v.tID;// new descendants due to this block ÐÂµÄ×ÓËï

					while (!edgeStack.isEmpty()) {
						if ((edgeStack.getLast().to.tID >= v.tID || edgeStack
								.getLast().to == stack.getLast())
								&& (edgeStack.getLast().from.tID >= v.tID || edgeStack
										.getLast().from == stack.getLast())) {
							nblock.add(edgeStack.removeLast());
						} else {
							break;
						}
					}
					sinks.add(stack.getLast());
					blocks.add(nblock);
					extra.add(time - v.tID);
				} else {
					if (stack.getLast().low > v.low)
						stack.getLast().low = v.low;
				}
			}
		}

	}
	public String toString() {
		return "Enum";
	}
}
