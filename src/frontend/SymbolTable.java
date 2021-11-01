package frontend;

import frontend.llvmir.Namespace;

import java.util.ArrayList;
import java.util.Objects;

public class SymbolTable {
    private int line;
    private String type; //int void
    private int bracknum; //0,1,2
    private boolean isConst;
    private int n1; //int[n1]
    private int n2; //int[n1][n2]
    private int level;
    private String name;
    private String kind; //var func block
    private ArrayList<SymbolTable> symbolTables; //block link to another symbolTable
    private ArrayList<SymbolTable> params; //funcparams
    private boolean isBlock;
    private int value;
    //save array reg
    //dim info
    private ArrayList<Namespace> namespaces;
    //2dim array initval
    private ArrayList<ArrayList<Namespace>> arrays;
    //1dim array initval
    private ArrayList<Namespace> array;
    //const int initval
    private Namespace initvalue;
    private int addr;

    //error process part
    //normal symbol decline
    public SymbolTable(int line, String type, boolean isConst, int n1, int n2, int level, String name, int bracknum, String kind) {
        this.line = line;
        this.type = type;
        this.isConst = isConst;
        this.n1 = n1;
        this.n2 = n2;
        this.level = level;
        this.name = name;
        this.bracknum = bracknum;
        this.kind = kind;
        this.isBlock = false;
    }

    //function
    public SymbolTable(int line, String type, int level, String name, String kind, ArrayList<SymbolTable> params) {
        this.line = line;
        this.type = type;
        this.level = level;
        this.name = name;
        this.kind = kind;
        this.params = params;
        this.isBlock = false;
    }

    //块
    public SymbolTable(String name, int level) {
        this.name = name;
        this.level = level;
        this.symbolTables = new ArrayList<>();
        this.type = "block";
        this.isBlock = true;
    }

    //IR code generate
    //normal symbol decline IR GEN
    public SymbolTable(int line, String type, boolean isConst, int n1, int n2, int level, String name, int bracknum, ArrayList<Namespace> namespaces, Namespace initvalue, String kind, int addr) {
        this.line = line;
        this.type = type;
        this.isConst = isConst;
        this.n1 = n1;
        this.n2 = n2;
        this.level = level;
        this.name = name;
        this.bracknum = bracknum;
        this.kind = kind;
        this.isBlock = false;
        this.namespaces = namespaces;
        this.arrays = arrays;
        this.addr = addr;
    }

    public SymbolTable(int line, String type, boolean isConst, int n1, int n2, int level, String name, int bracknum, String kind, ArrayList<Namespace> namespaces, Namespace initvalue, int addr) {
        this.line = line;
        this.type = type;
        this.isConst = isConst;
        this.n1 = n1;
        this.n2 = n2;
        this.level = level;
        this.name = name;
        this.bracknum = bracknum;
        this.kind = kind;
        this.isBlock = false;
        this.namespaces = namespaces;
        this.array = array;
        this.addr = addr;
    }

    public SymbolTable(int line, String type, boolean isConst, int n1, int n2, int level, String name, int bracknum, String kind, Namespace initvalue, int addr) {
        this.line = line;
        this.type = type;
        this.isConst = isConst;
        this.n1 = n1;
        this.n2 = n2;
        this.level = level;
        this.name = name;
        this.bracknum = bracknum;
        this.kind = kind;
        this.isBlock = false;
        this.initvalue = initvalue;
        this.addr = addr;
    }

    //IR code func
    public SymbolTable(int line, String type, int level, String name, String kind, ArrayList<SymbolTable> params, int addr) {
        this.line = line;
        this.type = type;
        this.level = level;
        this.name = name;
        this.kind = kind;
        this.params = params;
        this.isBlock = false;
        this.addr = addr;
    }

    public int getAddr() {
        return addr;
    }

    public ArrayList<SymbolTable> getSymbolTables() {
        return symbolTables;
    }

    public ExcNode addSymbol(SymbolTable symbolTable) {
        for (SymbolTable symbolTable1 : symbolTables) {
            if (Objects.equals(name, "global")) {
                if (symbolTable.getName().equals(symbolTable1.getName())) {
                    return new ExcNode(symbolTable.getLine(), "b", "redecline of " +
                            symbolTable.getName() + ",previous decline in line " + symbolTable1.getLine());
                }
            } else {
                if (symbolTable.equals(symbolTable1)) {
                    return new ExcNode(symbolTable.getLine(), "b", "redecline of " +
                            symbolTable.getName() + ",previous decline in line " + symbolTable1.getLine());
                }
            }
        }
        this.symbolTables.add(symbolTable);
        return null;
    }


    public int getLine() {
        return line;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public String getType() {
        return type;
    }

    public ArrayList<SymbolTable> getParams() {
        return params;
    }

    public int getBracknum() {
        return bracknum;
    }

    public boolean isConst() {
        return isConst;
    }

    public SymbolTable findFuncInAllTable(String name) {
        for (SymbolTable symbolTable : this.symbolTables) {
            if (symbolTable.getKind().equals("func") && symbolTable.getName().equals(name)) {
                return symbolTable;
            }
        }
        return null;
    }

    public SymbolTable findVarInAllTable(String name) {
        for (SymbolTable symbolTable : this.symbolTables) {
            if (symbolTable.getKind().equals("var") && symbolTable.getName().equals(name)) {
                return symbolTable;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolTable that = (SymbolTable) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind);
    }

    @Override
    public String toString() {
        return "SymbolTable{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                '}';
    }

    public ArrayList<Namespace> getNamespaces() {
        return namespaces;
    }

    public void setArrays(ArrayList<ArrayList<Namespace>> arrays) {
        this.arrays = arrays;
    }

    public ArrayList<ArrayList<Namespace>> getArrays() {
        return arrays;
    }

    public Namespace getInitvalue() {
        return initvalue;
    }

    public int getN1() {
        return n1;
    }

    public int getN2() {
        return n2;
    }

}
