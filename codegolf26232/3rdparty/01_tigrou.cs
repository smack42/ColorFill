using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

/*

program by tigrou
score 2,098,382
 
https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai/#26917


I try many things, most of them fail and just didn't work at all, until
recently. I got something interesting enough to post an answer.

There is certainly ways to improve this further more. I think going under the
2M steps might be possible.

It took approx 7 hours to generate results. Here is a txt file with all
solutions, in case someone want to check them : results.zip

More details about how it works :

It use A* Pathfinding algorithm.

What is difficult is to find a good heuristic. If the heuristic it
underestimate the cost, it will perform like almost like Dijkstra algorithm
and because of complexity of a 19x19 board with 6 colors it will run forever.
If it overestimate the cost it will converge quickly to a solution but won't
give good ones at all (something like 26 moves were 19 was possible).
Finding the perfect heuristic that give the exact remaining amount of steps to
reach solution would be the best (it would be fast and would give best possible
solution). It is (unless i'm wrong) impossible. It actually require to solve
the board itself first, while this is what you are actually trying to do
(chicken and egg problem)

I tried many things, here is what worked for the heuristic:

 * I build a graph of current board to evaluate. Each node represent a set of
   contiguous, same colored squares. Using that graph, I can easily calculate
   the exact minimal distance from center to any other node. For example
   distance from center to top left would be 10, because at minimum 10 colors
   separate them.

 * For calculating heuristic : I play the current board until the end. For each
   step, I choose the color which will minimize the sum of distances from root
   to all other nodes.

 * Number of moves needed to reach that end is the heuristic.

 * Estimated cost (used by A*) = moves so far + heuristic

It is not perfect as it slightly overestimate the cost (thus non optimal
solution is found). Anyway it is fast to calculate and give good results.

I was able to get huge speed improvment by using graph to perform all
operations. At begining I had a 2-dimension array. I flood it and recalculate
graph when needed (eg : to calculate the heuristic). Now everything is done
using the graph, which calculated only at the beginning. To simulate steps,
flood fill is no more needed, I merge nodes instead. This is a lot faster.

*/



namespace FloodPaintAI
{
    class Node
    {   
        public byte Value;             //1-6
        public int Index;              //unique identifier, used for easily deepcopying the graph
        public List<Node> Neighbours;  
        public List<Tuple<int, int>> NeighboursPositions; //used by BuildGraph() 

        public int Depth;    //used by GetSumDistances() 
        public bool Checked; // 

        public Node(byte value, int index)
        {
            Value = value;      
            Index = index;          
        }

        public Node(Node node)
        {           
            Value = node.Value; 
            Index = node.Index;                     
        }
    }

    class Board
    {
        private const int SIZE = 19;
        private const int STARTPOSITION = 9;

        public Node Root;         //root of graph. each node is a set of contiguous, same color square
        public List<Node> Nodes;  //all nodes in the graph, used for deep copying


        public int EstimatedCost; //estimated cost, used by A* Pathfinding
        public List<byte> Solution;

        public Board(StreamReader input)
        {                   
            byte[,] board = new byte[SIZE, SIZE];
            for(int j = 0 ; j < SIZE ; j++)
            {
                string line = input.ReadLine();
                for(int i = 0 ; i < SIZE ; i++)         
                {                                       
                    board[j, i] = byte.Parse(line[i].ToString());
                }               
            }
            Solution = new List<byte>();
            BuildGraph(board);  
        }

        public Board(Board boardToCopy)
        {               
            //copy the graph            
            Nodes = new List<Node>(boardToCopy.Nodes.Count);
            foreach(Node nodeToCopy in boardToCopy.Nodes)
            {
                Node node = new Node(nodeToCopy);
                Nodes.Add(node);
            }

            //copy "Neighbours" property
            for(int i = 0 ; i < boardToCopy.Nodes.Count ; i++)
            {
                Node node = Nodes[i];
                Node nodeToCopy = boardToCopy.Nodes[i];

                node.Neighbours = new List<Node>(nodeToCopy.Neighbours.Count);
                foreach(Node neighbour in nodeToCopy.Neighbours)
                {
                    node.Neighbours.Add(Nodes[neighbour.Index]);
                }
            }

            Root = Nodes[boardToCopy.Root.Index];
            EstimatedCost = boardToCopy.EstimatedCost;          
            Solution = new List<byte>(boardToCopy.Solution);            
        }

        private void BuildGraph(byte[,] board)
        {                       
            int[,] nodeIndexes = new int[SIZE, SIZE];
            Nodes = new List<Node>();

            //check how much sets we have (1st pass)
            for(int j = 0 ; j < SIZE ; j++)
            {
                for(int i = 0 ; i < SIZE ; i++)         
                {               
                    if(nodeIndexes[j, i] == 0) //not already visited                    
                    {
                        Node newNode = new Node(board[j, i], Nodes.Count);                      
                        newNode.NeighboursPositions = new List<Tuple<int, int>>();
                        Nodes.Add(newNode);

                        BuildGraphFloodFill(board, nodeIndexes, newNode, i, j, board[j, i]);
                    }
                }       
            }

            //set neighbours and root (2nd pass)
            foreach(Node node in Nodes)
            {
                node.Neighbours = new List<Node>();
                node.Neighbours.AddRange(node.NeighboursPositions.Select(x => nodeIndexes[x.Item2, x.Item1]).Distinct().Select(x => Nodes[x - 1]));
                node.NeighboursPositions = null;                
            }
            Root = Nodes[nodeIndexes[STARTPOSITION, STARTPOSITION] - 1];            
        }

        private void BuildGraphFloodFill(byte[,] board, int[,] nodeIndexes, Node node, int startx, int starty, byte floodvalue)
        {
            Queue<Tuple<int, int>> queue = new Queue<Tuple<int, int>>();
            queue.Enqueue(new Tuple<int, int>(startx, starty));

            while(queue.Count > 0)
            {
                Tuple<int, int> position = queue.Dequeue();
                int x = position.Item1;
                int y = position.Item2;

                if(x >= 0 && x < SIZE && y >= 0 && y < SIZE)
                {
                    if(nodeIndexes[y, x] == 0 && board[y, x] == floodvalue)
                    {
                        nodeIndexes[y, x] = node.Index + 1;

                        queue.Enqueue(new Tuple<int, int>(x + 1, y));
                        queue.Enqueue(new Tuple<int, int>(x - 1, y));
                        queue.Enqueue(new Tuple<int, int>(x, y + 1));
                        queue.Enqueue(new Tuple<int, int>(x, y - 1));                                           
                    }               
                    if(board[y, x] != floodvalue)
                        node.NeighboursPositions.Add(position);                         
                }       
            }
        }

        public int GetEstimatedCost()
        {       
            Board current = this;

            //copy current board and play the best color until the end.
            //number of moves required to go the end is the heuristic
            //estimated cost = current cost + heuristic
            while(!current.IsSolved())
            {
                int minSumDistance = int.MaxValue;
                Board minBoard = null;

                //find color which give the minimum sum of distance from root to each other node
                foreach(byte i in current.Root.Neighbours.Select(x => x.Value).Distinct())
                {
                    Board copy = new Board(current);
                    copy.Play(i);                   

                    int distance = copy.GetSumDistances();                  

                    if(distance < minSumDistance)
                    {
                        minSumDistance = distance;
                        minBoard = copy;
                    }
                }
                current = minBoard;
            }           
            return current.Solution.Count;
        }

        public int GetSumDistances()
        {
            //get sum of distances from root to each other node, using BFS
            int sumDistances = 0;           

            //reset marker
            foreach(Node n in Nodes)
            {
                n.Checked = false;                                  
            }

            Queue<Node> queue = new Queue<Node>();
            Root.Checked = true;
            Root.Depth = 0; 
            queue.Enqueue(Root);

            while(queue.Count > 0)
            {
                Node current = queue.Dequeue();                             
                foreach(Node n in current.Neighbours)
                {
                    if(!n.Checked)          
                    {                                   
                        n.Checked = true;                                               
                        n.Depth = current.Depth + 1;
                        sumDistances += n.Depth;            
                        queue.Enqueue(n);   
                    }               
                }
            }
            return sumDistances;
        }       

        public void Play(byte value)            
        {
            //merge root node with other neighbours nodes, if color is matching
            Root.Value = value;
            List<Node> neighboursToRemove = Root.Neighbours.Where(x => x.Value == value).ToList();
            List<Node> neighboursToAdd = neighboursToRemove.SelectMany(x => x.Neighbours).Except((new Node[] { Root }).Concat(Root.Neighbours)).ToList();

            foreach(Node n in neighboursToRemove)
            {
                foreach(Node m in n.Neighbours)
                {
                    m.Neighbours.Remove(n);
                }
                Nodes.Remove(n);
            }   

            foreach(Node n in neighboursToAdd)
            {
                Root.Neighbours.Add(n);         
                n.Neighbours.Add(Root); 
            }           

            //re-synchronize node index
            for(int i = 0 ; i < Nodes.Count ; i++)
            {
                Nodes[i].Index = i;
            }           
            Solution.Add(value);
        }

        public bool IsSolved()
        {           
            //return Nodes.Count == 1;
            return Root.Neighbours.Count == 0;  
        }           
    }


    class Program
    {       
        public static List<byte> Solve(Board input)
        {
            //A* Pathfinding            
            LinkedList<Board> open = new LinkedList<Board>();       
            input.EstimatedCost = input.GetEstimatedCost();
            open.AddLast(input);            

            while(open.Count > 0)
            {                       
                Board current = open.First.Value;
                open.RemoveFirst();

                if(current.IsSolved())
                {
                    return current.Solution;                
                }
                else
                {
                    //play all neighbours nodes colors
                    foreach(byte i in current.Root.Neighbours.Select(x => x.Value).Distinct())
                    {                       
                        Board newBoard = new Board(current);
                        newBoard.Play(i);           
                        newBoard.EstimatedCost = newBoard.GetEstimatedCost();   

                        //insert board to open list
                        bool inserted = false;
                        for(LinkedListNode<Board> node = open.First ; node != null ; node = node.Next)
                        {                               
                            if(node.Value.EstimatedCost > newBoard.EstimatedCost)
                            {
                                open.AddBefore(node, newBoard);
                                inserted = true;
                                break;
                            }
                        }       
                        if(!inserted)
                            open.AddLast(newBoard);                                                 
                    }   
                }   
            }
            throw new Exception(); //no solution found, impossible
        }   

        public static void Main(string[] args)
        {                   
            using (StreamReader sr = new StreamReader("floodtest"))
            {   
                while(!sr.EndOfStream)
                {                               
                    List<Board> boards = new List<Board>();
                    while(!sr.EndOfStream && boards.Count < 100)
                    {
                        Board board = new Board(sr);                        
                        sr.ReadLine(); //skip empty line
                        boards.Add(board);
                    }                                           
                    List<byte>[] solutions = new List<byte>[boards.Count];                                          
                    Parallel.For(0, boards.Count, i => 
                    {                               
                        solutions[i] = Solve(boards[i]); 
                    });                                         
                    foreach(List<byte> solution in solutions)
                    {
                        Console.WriteLine(string.Join(string.Empty, solution));                                             
                    }       
                }               
            }
        }
    }
}

