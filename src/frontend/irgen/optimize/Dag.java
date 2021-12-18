package frontend.irgen.optimize;

import frontend.stdir.ArrayLoadStore;
import frontend.stdir.IRCode;
import frontend.stdir.Sym;

import java.util.ArrayList;

public class Dag {
    ArrayList<ArrayList<IRCode>> basicblocks = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList<>();
    int nodenum = 0;
    ArrayList<ArrayList<IRCode>> dagout = new ArrayList<>();
    ArrayList<Block> blocks = new ArrayList<>();

    public Dag(ArrayList<ArrayList<IRCode>> basicblocks) {
        this.basicblocks = basicblocks;
        for (int i = 0; i < basicblocks.size(); i++) {
            Block block = new Block(basicblocks.get(i));
            blocks.add(block);
        }
    }

    class Block {
        private ArrayList<Graph> graphs;
        private ArrayList<Node> nodes;
        private ArrayList<IRCode> irCodes;

        public Block(ArrayList<IRCode> irCodes) {
            this.irCodes = irCodes;
            graphs = new ArrayList<>();
            nodes = new ArrayList<>();
            generateDag();
        }

        void generateDag() {
            for (IRCode irCode : irCodes) {

            }
        }
    }

    class Graph {
        private int num;
        private boolean isinit;
        private Sym sym;
        private String op;
        private Graph lc;
        private Graph rc;
        private Graph father;
        private ArrayLoadStore arrayLoadStore;

        public Graph(int num, String op) {
            this.num = num;
            this.op = op;
        }

        public Graph(int num, boolean isinit, Sym sym) {
            this.num = num;
            this.isinit = isinit;
            this.sym = sym;
        }

        public void setLc(Graph lc) {
            this.lc = lc;
        }

        public void setRc(Graph rc) {
            this.rc = rc;
        }

        public Graph getLc() {
            return lc;
        }

        public Graph getRc() {
            return rc;
        }

        public void setFather(Graph father) {
            this.father = father;
        }

        public Graph getFather() {
            return father;
        }

        public void setArrayLoadStore(ArrayLoadStore arrayLoadStore) {
            this.arrayLoadStore = arrayLoadStore;
        }

        public ArrayLoadStore getArrayLoadStore() {
            return arrayLoadStore;
        }
    }

    class Node {
        private Sym sym;
        private int value;
        private int num;
        private ArrayLoadStore arrayLoadStore;

        public Node(Sym sym, int value, int num) {
            this.sym = sym;
            this.value = value;
            this.num = num;
        }

        public int getValue() {
            return value;
        }

        public Sym getSym() {
            return sym;
        }

        public int getNum() {
            return num;
        }

        public void setArrayLoadStore(ArrayLoadStore arrayLoadStore) {
            this.arrayLoadStore = arrayLoadStore;
        }

        public ArrayLoadStore getArrayLoadStore() {
            return arrayLoadStore;
        }
    }
}
