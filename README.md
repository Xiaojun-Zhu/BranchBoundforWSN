# BranchBoundforWSN

Data and code for the paper: Xiaojun Zhu, Shaojie Tang. A Branch and Bound Algorithm for Building Optimal Data Gathering Tree in Wireless Sensor Networks. to appear in INFORMS Journal on Computing. 

CPLEX is required to use these codes.

## 1. Problem Instances

File 'networks30to100.txt' contains all problems used in the paper. In the file, networks are encoded as follows:

Each network consists of four lines. 
-  The first line is of the form: ProblemID, NumNode, NumOfDirectedEdges, TransmissionEnergy, ReceivingEnergy. For example, `0 30 92 6.66E-4 3.33E-4` means that Problem 0 has 30 nodes and 92 directed edges, transmitting a message consumes energy 6.66\*10^-4J and receiving a message consumes energy 3.33*10^-4.
- The second line has NumNode real values, indicating the initial energy of all nodes. Note that node 0 (the sink) has infinite energy. 
- The third line lists NumOfDirectedEdges/2 pairs of integers, describing undirected edges. For each undirected edge, we list it only once, from smaller node ID to larger node ID. For example, `0 27 0 26 0 22 0 17 0 13 0 2 1 9 1 8 2 29` means 9 undirected edges, (0,27), (0,26), (0,22), (0,17), (0,13), (0,2), (1,9), (1,8) and (2,29). We will insert 18 directed edges in the network. 
- The fourth line lists NumNode pairs of real values, indicating the positions of all nodes. This data is only used for illustration, and is not used in our algorithm. 

The code that generates these networks is `GenerateReadWriteNetwork.java`

## 2. Computational Results
The computational results are in file `computational_results.xlsx`. 

Each method has four columns, (ProblemID, FoundLifetime, CPUTimeInSeconds, Status). Status 2 means the problem is solved to optimality, and status 1 indicates that the problem is not solved. When the problem is not solved, positive FoundLifetime indicates a suboptimal solution. Note that negative FoundLifetime indicates that no feasible solution is found.  


## 3. Implemented Algorithms

Algorithms are implemented in java classes with prefix 'Alg'. 

| File | Implemented Algorithm|
|---|---|
|AlgBB.java	| BB |
|AlgCombineFSBB.java| combining FS with BB, i.e., 10% time for FS and the rest time for BB|
| AlgILPB.java	| ILP-B|
| AlgILPBD.java	| ILP-BD|
| AlgILPFS.java	| ILP-FS|
|AlgEnum.java| Enum|
|AlgILPFSMFlow.java| a multi-commodity flow formulation of FS|

EvalScript.java contains code we used to run the algorithms on problem instances. 
