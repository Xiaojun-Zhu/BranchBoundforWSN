
public class DEdge {
DNode from,to;
DEdge previous,next;

DEdge pair;//the directed edge in the reverse direction

boolean inSPT=false;//whether in the partial spanning tree

int num=-1;//for Integer Linear Programming 对于整数线性规划

boolean visited=false;
}
