package frontend.irgen.symtable;

import java.util.ArrayList;

public class Func extends SymTable {
    private String name;
    private ArrayList<Var> params;
    private String type; //void int
    private int addr;

    public Func(String name, ArrayList<Var> params, String type) {
        this.name = name;
        this.params = params;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setAddr(int addr) {
        this.addr = addr;
    }

    public int getAddr() {
        return addr;
    }

    public ArrayList<Var> getParams() {
        return params;
    }
}
