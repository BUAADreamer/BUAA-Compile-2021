package frontend.stdir;

import frontend.irgen.symtable.SymTable;
import frontend.irgen.symtable.Var;

import java.util.ArrayList;

public class Sym {
    private int value;
    private int type;
    private String name;
    private SymTable symbol;
    private ArrayList<Sym> params;

    /**
     * constructor of number
     *
     * @param value
     */
    public Sym(int value) {
        this.type = 0;
        this.value = value;
    }

    /**
     * constructor of temp var
     *
     * @param name
     */
    public Sym(String name) {
        this.type = 1;
        this.name = name;
    }

    /**
     * constructor of temp var declined
     *
     * @param symbol
     */
    public Sym(SymTable symbol) {
        this.type = 2;
        this.symbol = symbol;
    }

    public Sym(ArrayList<Sym> params) {
        this.params = params;
        this.type = 3;
    }

    @Override
    public String toString() {
        if (type == 0) return String.valueOf(value);
        else if (type == 1) return name;
        else return symbol.toString();
    }

    public String getTypeOut() {
        return ((Var) symbol).getTypeOut();
    }

    public Boolean isconst() {
        return ((Var) symbol).isIsconst();
    }

    public SymTable getSymbol() {
        return symbol;
    }

    public int getType() {
        return type;
    }

    public ArrayList<Sym> getParams() {
        return params;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
