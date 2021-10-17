package frontend;

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
}
