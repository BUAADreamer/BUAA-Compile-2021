package frontend.irgen.symtable;

import java.util.ArrayList;

public class Block extends SymTable {
    private ArrayList<SymTable> symTables;
    private String name;

    public Block(String name) {
        this.name = name;
        this.symTables = new ArrayList<>();
    }

    public void addSym(SymTable symTable) {
        symTables.add(symTable);
    }

    public SymTable findFunc(String name) {
        for (SymTable symTable : symTables) {
            if (symTable instanceof Func && symTable.getName().equals(name)) {
                return symTable;
            }
        }
        return null;
    }

    public SymTable findVar(String name) {
        for (SymTable symTable : symTables) {
            if (symTable instanceof Var && symTable.getName().equals(name)) {
                return symTable;
            }
        }
        return null;
    }

    public SymTable findVar1(String name) {
        if (true)
            return findVar(name);
        for (SymTable symTable : symTables) {
            if (symTable instanceof Var && ((symTable.getName() + ((Var) symTable).getLevel()).equals(name) || (symTable.getName().equals(name)))) {
                return symTable;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }
}
