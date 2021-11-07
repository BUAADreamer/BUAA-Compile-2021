package backend.mips;

import frontend.irgen.symtable.Block;
import frontend.irgen.symtable.Func;
import frontend.irgen.symtable.Var;
import frontend.stdir.*;

import java.util.ArrayList;
import java.util.HashMap;

public class IRTranslator {
    private ArrayList<IRCode> ircodes;
    private String mipsout = "";
    private ArrayList<Block> blockstack = new ArrayList<>();
    private int curLevel = 0;
    private ArrayList<MipsCode> mipsCodes = new ArrayList<>();
    private ArrayList<MipsCode> texts = new ArrayList<>();
    private ArrayList<MipsCode> datas = new ArrayList<>();
    private int gp = 0x10010000;
    private int fp = 0;
    private int sp = 0x7fffeffc;
    private ArrayList<Boolean> regMap = new ArrayList<>();
    private HashMap<String, Namespace> tmpname2reg = new HashMap<>();
    private Namespace reg0 = new Namespace(0, 0);
    private int strnum = 0;

    public IRTranslator(ArrayList<IRCode> ircodes) {
        this.ircodes = ircodes;
        for (int i = 0; i < 32; i++) regMap.add(true);
        blockstack.add(new Block("Global"));
        datas.add(new LabelMipsCode(".data\n"));
        texts.add(new LabelMipsCode(".text\n"));
        scanircodes();
        setMipsout();
    }

    private void scanircodes() {
        for (int i = 0; i < ircodes.size(); i++) {
            if (ircodes.get(i) instanceof Label) scanLabel((Label) ircodes.get(i));
            else if (ircodes.get(i) instanceof Decl) scanDecl((Decl) ircodes.get(i));
            else if (ircodes.get(i) instanceof ArrayDecl) scanArrayDecl((ArrayDecl) ircodes.get(i));
            else if (ircodes.get(i) instanceof ArrayLoadStore) scanArrayLoadStore((ArrayLoadStore) ircodes.get(i));
            else if (ircodes.get(i) instanceof FuncDecl) scanFuncDecl((FuncDecl) ircodes.get(i));
            else if (ircodes.get(i) instanceof FuncParam) scanFuncParam((FuncParam) ircodes.get(i));
            else if (ircodes.get(i) instanceof FuncRet) scanFuncRet((FuncRet) ircodes.get(i));
            else if (ircodes.get(i) instanceof FuncCall) scanFuncCall((FuncCall) ircodes.get(i));
            else if (ircodes.get(i) instanceof Jump) scanJump((Jump) ircodes.get(i));
            else if (ircodes.get(i) instanceof CondBranch) scanCondBranch((CondBranch) ircodes.get(i));
            else if (ircodes.get(i) instanceof Printf) scanPrintf((Printf) ircodes.get(i));
            else if (ircodes.get(i) instanceof Exp) scanExp((Exp) ircodes.get(i));
        }
    }

    private void scanExp(Exp exp) {
        Sym lsym = exp.getLsym();
        Sym rsym1 = exp.getRsym1();
        Sym rsym2 = exp.getRsym2();
        Namespace rname1 = rsym2ns(rsym1);
        Namespace rname2 = rsym2ns(rsym2);
        Namespace lname = lsym2ns(lsym);
        if (lname.type == 0) {
            if (exp.getOp().equals("=")) {
                addText(new Assign(lname, rname1));
            } else if (exp.getOp().equals("%") || exp.getOp().equals("*") || exp.getOp().equals("/")) {
                Namespace reg = getReg();
                if (rname1.getType() == 1) {
                    rname1 = reg;
                    addText(new Assign(reg, rname1));
                } else {
                    freeReg(reg.getReg());
                }
                Namespace reg1 = getReg();
                if (rname2.getType() == 1) {
                    rname2 = reg1;
                    addText(new Assign(reg1, rname2));
                } else {
                    freeReg(reg1.getReg());
                }
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                Namespace reg2 = getReg();
                if (exp.getOp().equals("%")) {
                    addText(new MTF(lname, "mfhi"));
                } else if (exp.getOp().equals("*")) {
                    addText(new MTF(lname, "mflo"));
                } else {
                    addText(new MTF(lname, "mflo"));
                }
                addText(new LoadStore(reg2, reg0, lname, 3));
            } else if (exp.getOp().equals("+") || exp.getOp().equals("-")) {
                addText(new Calculate(lname, rname1, rname2, exp.getOp()));
            }
        } else {
            if (exp.getOp().equals("=")) {
                addText(new LoadStore(rname1, reg0, lname, 3));
            } else if (exp.getOp().equals("%") || exp.getOp().equals("*") || exp.getOp().equals("/")) {
                Namespace reg = getReg();
                if (rname1.getType() == 1) {
                    rname1 = reg;
                    addText(new Assign(reg, rname1));
                } else {
                    freeReg(reg.getReg());
                }
                Namespace reg1 = getReg();
                if (rname2.getType() == 1) {
                    rname2 = reg1;
                    addText(new Assign(reg1, rname2));
                } else {
                    freeReg(reg1.getReg());
                }
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                Namespace reg2 = getReg();
                if (exp.getOp().equals("%")) {
                    addText(new MTF(reg2, "mfhi"));
                } else if (exp.getOp().equals("*")) {
                    addText(new MTF(reg2, "mflo"));
                } else {
                    addText(new MTF(reg2, "mflo"));
                }
                addText(new LoadStore(reg2, reg0, lname, 3));
            } else if (exp.getOp().equals("+") || exp.getOp().equals("-")) {
                Namespace reg = getReg();
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                addText(new LoadStore(reg, reg0, lname, 3));
            }
        }
    }

    private Namespace lsym2ns(Sym sym) {
        if (sym == null) return null;
        if (sym.getType() == 1) {
            if (sym.toString().charAt(0) == '%' || sym.toString().charAt(0) == '@') {
                Var var = findVarInAllTable(sym.toString().substring(1));
                int addr = var.getAddr();
                return new Namespace(addr, 1);
            } else {
                return getReg();
            }
        } else {
            Var var = findVarInAllTable(sym.toString().substring(1));
            int addr = var.getAddr();
            return new Namespace(addr, 1);
        }
    }

    private Namespace rsym2ns(Sym sym) {
        if (sym == null) return null;
        if (sym.getType() == 0) {
            return new Namespace(sym.getValue(), 1);
        } else if (sym.getType() == 1) {
            if (sym.toString().charAt(0) == '%' || sym.toString().charAt(0) == '@') {
                Var var = findVarInAllTable(sym.toString().substring(1));
                int addr = var.getAddr();
                Namespace reg1 = getReg();
                addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
                return reg1;
            } else if (sym.getName().equals("getint()")) {
                addText(new Syscall(0));
                return new Namespace(2, 0);
            } else {
                return getReg();
            }
        } else {
            Var var = findVarInAllTable(sym.toString().substring(1));
            int addr = var.getAddr();
            Namespace reg1 = getReg();
            addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
            return reg1;
        }
    }

    private void scanPrintf(Printf printf) {
        StringBuilder sb = new StringBuilder("");
        String str = printf.getSym().getName();
        ArrayList<Sym> syms = printf.getSyms();
        int cur = 0;
        for (int i = 1; i < str.length() - 1; i++) {
            if (str.charAt(i) != '%') {
                sb.append(str.substring(i, i + 1));
            } else {
                if (!sb.toString().equals("")) {
                    addData(new LabelMipsCode(
                            String.format("str%d: .asciiz \"%s\"\n", ++strnum, sb.toString())));
                    addText(new LoadStore(new Namespace(4, 0), null,
                            new Namespace(String.format("str%d", strnum)), 6));
                    addText(new Syscall(2));
                    sb = new StringBuilder("");
                }
                Namespace tar = rsym2ns(syms.get(cur));
                if (tar.getType() == 1) {
                    addText(new Assign(new Namespace(4, 0), tar.getValue()));
                } else {
                    addText(new Assign(new Namespace(4, 0), tar));
                }
                addText(new Syscall(1));
                cur++;
                i += 1;
            }
        }
        if (!sb.toString().equals("")) {
            addData(new LabelMipsCode(
                    String.format("str%d: .asciiz \"%s\"\n", ++strnum, sb.toString())));
            addText(new LoadStore(new Namespace(4, 0), null,
                    new Namespace(String.format("str%d", strnum)), 6));
            addText(new Syscall(2));
        }
    }

    private void scanCondBranch(CondBranch condBranch) {

    }

    private void scanJump(Jump jump) {
        addText(new BrJump(new Namespace(jump.getLabel().getName()), "j"));
    }

    private void scanFuncCall(FuncCall funcCall) {
    }

    private void scanFuncRet(FuncRet funcRet) {
        if (funcRet.getSym().getType() == 0) {
            addText(new Assign(new Namespace(3, 1), funcRet.getSym().getValue()));
        }
        addText(new BrJump(new Namespace(31, 1), "jr"));
    }

    private void scanFuncParam(FuncParam funcParam) {
    }

    private void scanArrayLoadStore(ArrayLoadStore arrayLoadStore) {
    }

    private void scanArrayDecl(ArrayDecl arrayDecl) {
        Var array = (Var) arrayDecl.getSym().getSymbol();
        if (curLevel == 0) {
            array.setAddr(gp);
            gp += arrayDecl.getSpace();
            addVarSym(array);
            addData(new LabelMipsCode(String.format("%s:\n",
                    arrayDecl.getSym().getSymbol().getName())));
            for (Sym sym : arrayDecl.getArrayval()) {
                addData(new LabelMipsCode(String.format(".word %d\n",
                        sym.getValue())));
            }
        } else {
            array.setAddr(fp);
            fp += arrayDecl.getSpace();
        }
        addVarSym(array);
    }

    private void scanFuncDecl(FuncDecl funcDecl) {
        addText(new LabelMipsCode(funcDecl.getFunc().getName() + ":\n"));
    }

    private void scanDecl(Decl decl) {
        Var var = (Var) decl.getSym().getSymbol();
        if (curLevel == 0) {
            var.setAddr(gp);
            gp += 4;
            addVarSym(var);
            addData(new LabelMipsCode(String.format("%s: .word %d\n",
                    decl.getSym().getSymbol().getName(), decl.getRsym().getValue())));
        } else {
            var.setAddr(fp);
            fp += 4;
        }
        addVarSym(var);
    }

    private void scanLabel(Label label) {
        String[] names = label.getName().split("_");
        String ident = names[0];
        if (!names[1].equals("main") && !ident.equals("decline")) {
            texts.add(new LabelMipsCode(label.toString()));
        }
        if (ident.equals("block")) {
            String type = names[names.length - 1];
            if (type.equals("begin")) {
                blockstack.add(new Block("main"));
                curLevel++;
            } else if (type.equals("end")) {
                blockstack.remove(blockstack.size() - 1);
                curLevel--;
                tmpname2reg = new HashMap<>();
            }
        }

    }

    private void addMipsCode(MipsCode mipsCode) {
        mipsCodes.add(mipsCode);
    }

    private void addData(MipsCode mipsCode) {
        datas.add(mipsCode);
    }

    private void addText(MipsCode mipsCode) {
        texts.add(mipsCode);
    }

    public void setMipsout() {
        for (MipsCode mipsCode : datas) {
            addMipsCode(mipsCode);
        }
        for (MipsCode mipsCode : texts) {
            addMipsCode(mipsCode);
        }
        StringBuilder res = new StringBuilder("");
        for (MipsCode mipsCode : mipsCodes) {
            res.append(mipsCode.toString());
        }
        mipsout = res.toString();
    }

    void addVarSym(Var var) {
        blockstack.get(curLevel).addSym(var);
    }

    void addFuncSym(Func func) {
        blockstack.get(0).addSym(func);
    }

    private Namespace getReg() {
        for (int i = 0; i < 32; i++) {
            if (i == 0 || (i >= 28 && i <= 31)) continue;
            if (regMap.get(i)) {
                regMap.set(i, false);
                return new Namespace(i, 0);
            }
        }
        return null;
    }

    private void freeReg(int reg) {
        regMap.set(reg, true);
    }

    public String getMipsout() {
        return mipsout;
    }

    private Func findFuncInAllTable(String name) {
        Func ret = (Func) blockstack.get(0).findFunc(name);
        if (ret != null) return ret;
        return null;
    }

    private Var findVarInAllTable(String name) {
        for (int i = curLevel; i >= 0; i--) {
            Var ret = (Var) blockstack.get(i).findVar(name);
            if (ret != null) return ret;
        }
        return null;
    }
}
