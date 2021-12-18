package backend;

import backend.mips.*;
import frontend.irgen.optimize.BasicBlock;
import frontend.irgen.optimize.CompileUnit;
import frontend.irgen.optimize.FuncBlock;
import frontend.irgen.symtable.Block;
import frontend.irgen.symtable.Func;
import frontend.irgen.symtable.Var;
import frontend.stdir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class IR2Mips {
    private CompileUnit compileUnit;
    private ArrayList<IRCode> globaldeclines;
    private ArrayList<FuncBlock> funcBlocks;
    private ArrayList<MipsCode> datacodes = new ArrayList<>();
    private ArrayList<MipsCode> mipsCodes = new ArrayList<>();
    private ArrayList<Block> blockstack = new ArrayList<>();
    int[] tmpregs = {3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 24, 25, 26, 27, 28};
    int[] globalregs = {16, 17, 18, 19, 20, 21, 22, 23};
    private int gp = 0x10010000;
    private int fp = 0x10040000;
    private int curFunc = 0;
    int curLevel = 0;
    ArrayList<MipsCode> curfunccodes = new ArrayList<>();
    Namespace reg0 = new Namespace(0, 0);
    int strnum = 0;
    private int stackaddr = 0x7fff0000;
    private final int stackbase = 0x7fff0000;
    private ArrayList<Func> funcstack = new ArrayList<>();
    private HashMap<Integer, Boolean> regpool = new HashMap<>(); //3-15 24-28
    private HashMap<Integer, Sym> globalreg2sym = new HashMap<>(); //16-23
    private HashMap<Integer, Sym> tmpreg2sym = new HashMap<>();
    private String mipsout;

    public IR2Mips(CompileUnit compileUnit) {
        this.compileUnit = compileUnit;
        globaldeclines = compileUnit.getGlobaldecls();
        resetGlobalRegpool();
        resetTmpRegpool();
        funcBlocks = compileUnit.getFuncBlocks();
        datacodes.add(new LabelMipsCode(".data\n"));
        transdecls();
        for (; curFunc < funcBlocks.size(); curFunc++) {
            transFunc();
        }
        setMipsout();
    }

    void addVarSym(Var var) {
        blockstack.get(curLevel).addSym(var);
    }

    private void addMipsCode(MipsCode mipsCode) {
        mipsCodes.add(mipsCode);
    }

    private void addData(MipsCode mipsCode) {
        datacodes.add(mipsCode);
    }

    private void addText(MipsCode mipsCode) {
        curfunccodes.add(mipsCode);
    }

    void resetTmpRegpool() {
        for (int reg : tmpregs) {
            regpool.put(reg, false);
        }
        tmpreg2sym = new HashMap<>();
    }

    void resetGlobalRegpool() {
        for (int reg : globalregs) {
            regpool.put(reg, false);
        }
        globalreg2sym = new HashMap<>();
    }

    void transdecls() {
        blockstack.add(new Block("global"));
        for (IRCode irCode : globaldeclines) {
            if (irCode instanceof ArrayDecl) {
                transGlobalArrayDecl((ArrayDecl) irCode);
            } else {
                transGlobalDecl((Decl) irCode);
            }
        }
    }

    private void transGlobalDecl(Decl decl) {
        Var var = (Var) decl.getSym().getSymbol();
        var.setAddr(gp);
        gp += 4;
        addVarSym(var);
        addData(new LabelMipsCode(String.format("%s: .word %d\n",
                decl.getSym().getSymbol().getName(), decl.getRsym().getValue())));
    }

    void transGlobalArrayDecl(ArrayDecl arrayDecl) {
        Var array = (Var) arrayDecl.getSym().getSymbol();
        if (arrayDecl.getHasInitVal()) {
            array.setAddr(gp);
            gp += arrayDecl.getSpace();
            addVarSym(array);
            addData(new LabelMipsCode(String.format("%s:\n",
                    arrayDecl.getSym().getSymbol().getName())));
            for (Sym sym : arrayDecl.getArrayval()) {
                if (sym == null) continue;
                addData(new LabelMipsCode(String.format(".word %d\n",
                        sym.getValue())));
            }
        } else {
            array.setAddr(gp);
            gp += arrayDecl.getSpace();
            addVarSym(array);
            addData(new LabelMipsCode(String.format("%s: .space %d\n",
                    arrayDecl.getSym().getSymbol().getName(), arrayDecl.getSpace())));
        }
    }

    private void scanExp(Exp exp) {
        Sym lsym = exp.getLsym();
        Sym rsym1 = exp.getRsym1();
        Sym rsym2 = exp.getRsym2();
        Namespace rname1 = rsym2ns(rsym1);
        Namespace rname2 = rsym2ns(rsym2);
        Namespace lname = lsym2ns(lsym);
        if (lname != null && lname.getType() == 0) {
            if (exp.getOp().equals("=")) {
                addText(new Assign(lname, rname1));
            } else if (exp.getOp().equals("%") || exp.getOp().equals("*") || exp.getOp().equals("/") || exp.getOp().equals("**")) {
                Namespace reg = getReg();
                if (rname1 != null && rname1.getType() == 1) {
                    addText(new Assign(reg, rname1));
                    rname1 = reg;
                } else {
                    if (reg != null)
                        freeReg(reg.getReg());
                }
                Namespace reg1 = getReg();
                if (rname2 != null && rname2.getType() == 1) {
                    addText(new Assign(reg1, rname2));
                    rname2 = reg1;
                } else {
                    if (reg1 != null)
                        freeReg(reg1.getReg());
                }
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                Namespace reg2 = getReg();
                if (exp.getOp().equals("%") || exp.getOp().equals("**")) {
                    addText(new MTF(reg2, "mfhi"));
                } else if (exp.getOp().equals("*")) {
                    addText(new MTF(reg2, "mflo"));
                } else {
                    addText(new MTF(reg2, "mflo"));
                }
                if (lname.getType() == 1) {
                    addloadstore(reg2, reg0, lname, 3);
                } else if (lname.getType() == 0) {
                    addText(new Assign(lname, reg2));
                }
            } else if (exp.getOp().equals("+") || exp.getOp().equals("-")) {
                if (rname1.getType() == 1) {
                    Namespace reg = getReg();
                    addText(new Assign(reg, rname1));
                    rname1 = reg;
                }
                addText(new Calculate(lname, rname1, rname2, exp.getOp()));
            } else {
                if (rname1.getType() == 1) {
                    Namespace reg = getReg();
                    addText(new Assign(reg, rname1));
                    rname1 = reg;
                }
                addText(new Calculate(lname, rname1, rname2, exp.getOp()));
            }
        } else {
            if (exp.getOp().equals("=")) {
                if (rname1 != null && rname1.getType() == 1) {
                    Namespace reg = getReg();
                    addText(new Assign(reg, rname1));
                    rname1 = reg;
                }
                addloadstore(rname1, reg0, lname, 3);
            } else if (exp.getOp().equals("%") || exp.getOp().equals("*") || exp.getOp().equals("/") || exp.getOp().equals("**")) {
                Namespace reg = getReg();
                if (rname1 != null && rname1.getType() == 1) {
                    rname1 = reg;
                    addText(new Assign(reg, rname1));
                } else {
                    if (reg != null)
                        freeReg(reg.getReg());
                }
                Namespace reg1 = getReg();
                if (rname2 != null && rname2.getType() == 1) {
                    rname2 = reg1;
                    addText(new Assign(reg1, rname2));
                } else {
                    if (reg1 != null)
                        freeReg(reg1.getReg());
                }
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                Namespace reg2 = getReg();
                if (exp.getOp().equals("%") || exp.getOp().equals("**")) {
                    addText(new MTF(reg2, "mfhi"));
                } else if (exp.getOp().equals("*")) {
                    addText(new MTF(reg2, "mflo"));
                } else {
                    addText(new MTF(reg2, "mflo"));
                }
                if (lname.getType() == 1) {
                    addloadstore(reg2, reg0, lname, 3);
                } else if (lname.getType() == 0) {
                    addText(new Assign(lname, reg2));
                }
            } else if (exp.getOp().equals("+") || exp.getOp().equals("-")) {
                Namespace reg = getReg();
                if (rname1.getType() == 1) {
                    Namespace reg1 = getReg();
                    addText(new Assign(reg1, rname1));
                    rname1 = reg1;
                }
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                addloadstore(reg, reg0, lname, 3);
            } else {
                Namespace reg = getReg();
                if (rname1.getType() == 1) {
                    Namespace reg1 = getReg();
                    addText(new Assign(reg1, rname1));
                    rname1 = reg1;
                }
                addText(new Calculate(reg, rname1, rname2, exp.getOp()));
                addloadstore(reg, reg0, lname, 3);
            }
        }
    }

    private void addloadstore(Namespace reg1, Namespace reg2, Namespace addr, int type) {
        if (addr.getType() == 1) {
            if (addr.getValue() >= stackbase) {
                if (reg2 != null && reg2.getReg() > 0) {
                    addText(new Calculate(reg2, reg2, new Namespace(stackbase - addr.getValue(), 1), "+"));
                    addText(new Calculate(reg2, reg2, new Namespace(29, 0), "+"));
                    addText(new LoadStore(reg1, reg2, new Namespace(0, 1), type));
                    return;
                } else {
                    addText(new LoadStore(reg1, reg2, addr, type));
                    return;
                }
            }
            addText(new LoadStore(reg1, reg2, addr, type));
        } else {
            if (reg2.getReg() == 0) {
                addText(new LoadStore(reg1, addr, new Namespace(0, 1), type));
            } else {
                addText(new Calculate(addr, addr, reg2, "+"));
                addText(new LoadStore(reg1, addr, new Namespace(0, 1), type));
            }
        }
    }

    private void freeReg(int reg) {
        regpool.put(reg, false);
    }

    private Namespace getReg() {
        for (int reg : tmpregs) {
            if (!regpool.get(reg) && !tmpreg2sym.containsKey(reg)) {
                regpool.put(reg, true);
                return new Namespace(reg, 0);
            }
        }
        for (int reg : tmpregs) {
            if (!tmpreg2sym.containsKey(reg)) {
                regpool.put(reg, true);
                return new Namespace(reg, 0);
            }
        }
        return new Namespace(tmpregs[0], 0);
    }

    private Namespace lsym2ns(Sym sym) {
        if (sym == null) return null;
        if (sym.getType() == 1) {
            if (sym.toString().charAt(0) == '%' || sym.toString().charAt(0) == '@') {
                Var var = findVarInAllTable(sym.toString().substring(1));
                int addr = var.getAddr();
                if (var.getReg() > 0) {
                    return new Namespace(var.getReg(), 0);
                }
                return new Namespace(addr, 1);
            } else {
                if (sym2reg(sym) != null) {
                    return sym2reg(sym);
                }
                Namespace reg = getReg();
                tmpreg2sym.put(reg.getReg(), sym);
                return reg;
            }
        } else {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var == null) return null;
            int addr = var.getAddr();
            return new Namespace(addr, 1);
        }
    }

    private Namespace rsym2ns(Sym sym) {
        if (sym == null) return null;
        if (sym.getType() == 0) {
            return new Namespace(sym.getValue(), 1);
        } else if (sym.getType() == 1) {
            if ((sym.toString().charAt(0) == '%' || sym.toString().charAt(0) == '@')) {
                Var var = findVarInAllTable(sym.toString().substring(1));
                if (var.getReg() > 0) {
                    return new Namespace(var.getReg(), 0);
                }
                int addr = var.getAddr();
                Namespace reg1 = getReg();
                addloadstore(reg1, reg0, new Namespace(addr, 1), 0);
                return reg1;
            } else if (sym.getName().equals("getint()")) {
                addText(new Syscall(0));
                return new Namespace(2, 0);
            } else if (sym.getName().equals("RET")) {
                return new Namespace(2, 0);
            } else if (sym.toString().charAt(0) == '&') {
                String str = sym.toString();
                int pos = str.indexOf('[');
                if (pos == -1) pos = str.length();
                Var var = findVarInAllTable(sym.toString().substring(2, pos));
                return new Namespace(var.getAddr(), 1);
            } else {
                Namespace reg = sym2reg(sym);
                if (reg == null) {
                    reg = getReg();
                    tmpreg2sym.put(reg.getReg(), sym);
                }
                return reg;
            }
        } else if (sym.getType() == 2) {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var.getReg() > 0) {
                return new Namespace(var.getReg(), 0);
            }
            if (var == null) return null;
            int addr = var.getAddr();
            Namespace reg1 = getReg();
            addloadstore(reg1, reg0, new Namespace(addr, 1), 0);
            return reg1;
        } else if (sym.getType() == 3) {
            //1dim array pass
            String str = sym.toString();
            int pos = str.indexOf('[');
            if (pos == -1) pos = str.length();
            Var var = findVarInAllTable(sym.toString().substring(2, pos));
            return new Namespace(var.getAddr(), 1);
        } else {
            Namespace index = transIndex2reg(sym.getIndex());
            Var var = findVarInAllTable(sym.getArrayname().getName());
            Namespace addr = new Namespace(var.getAddr(), 1);
            addText(new Calculate(index, index, addr, "+"));
            return index;
        }
    }

    private Namespace rsym2ns2(Sym sym, int offset) {
        if (sym == null) return null;
        if (sym.getType() == 0) {
            return new Namespace(sym.getValue(), 1);
        } else if (sym.getType() == 1) {
            if (sym.toString().charAt(0) == '%' || sym.toString().charAt(0) == '@') {
                if (sym.toString().contains("[")) {
                    String str = sym.toString();
                    int pos = str.indexOf('[');
                    if (pos == -1) pos = str.length();
                    Var var = findVarInAllTable(sym.toString().substring(2, pos));
                    return new Namespace(var.getAddr(), 1);
                }
                Var var = findVarInAllTable(sym.toString().substring(1));
                if (var.getReg() > 0) {
                    return new Namespace(var.getReg(), 0);
                }
                int addr = var.getAddr();
                addr -= offset;
                Namespace reg1 = getReg();
                addloadstore(reg1, reg0, new Namespace(addr, 1), 0);
                return reg1;
            } else if (sym.getName().equals("getint()")) {
                addText(new Syscall(0));
                return new Namespace(2, 0);
            } else if (sym.getName().equals("RET")) {
                return new Namespace(2, 0);
            } else if (sym.toString().charAt(0) == '&') {
                if (sym.getType() == 3) {
                    String str = sym.toString();
                    int pos = str.indexOf('[');
                    if (pos == -1) pos = str.length();
                    Var var = findVarInAllTable(sym.toString().substring(2, pos));
                    return new Namespace(var.getAddr(), 1);
                } else if (sym.getType() == 4) {
                    return null;
                }
            } else {
                Namespace reg = sym2reg(sym);
                return reg;
            }
        } else if (sym.getType() == 2) {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var.getReg() > 0) {
                return new Namespace(var.getReg(), 0);
            }
            if (var == null) return null;
            int addr = var.getAddr();
            addr -= offset;
            Namespace reg1 = getReg();
            addloadstore(reg1, reg0, new Namespace(addr, 1), 0);
            return reg1;
        } else if (sym.getType() == 3) {
            //1dim array pass
            String str = sym.toString();
            int pos = str.indexOf('[');
            if (pos == -1) pos = str.length();
            Var var = findVarInAllTable(sym.toString().substring(2, pos));
            int addr = var.getAddr();
            addr -= offset;
            Namespace reg1 = getReg();
            addloadstore(reg1, reg0, new Namespace(addr, 1), 0);
            return reg1;
        } else {
            Namespace index = transIndex2reg(sym.getIndex());
            Var var = findVarInAllTable(sym.getArrayname().getName());
            int addr = var.getAddr();
            addr -= offset;
            Namespace reg1 = getReg();
            addloadstore(reg1, reg0, new Namespace(addr, 1), 0);
            addText(new Calculate(reg1, reg1, index, "+"));
            return reg1;
        }
        return null;
    }

    Namespace sym2reg(Sym sym) {
        for (int reg : tmpreg2sym.keySet()) {
            if (tmpreg2sym.get(reg).toString().equals(sym.toString())) {
                return new Namespace(reg, 0);
            }
        }
        return null;
    }

    ArrayList<Namespace> getUsingRegs() {
        ArrayList<Namespace> regs = new ArrayList<>();
        for (int reg : regpool.keySet()) {
            if (regpool.get(reg)) {
                regs.add(new Namespace(reg, 0));
            }
        }
        return regs;
    }

    private Namespace transIndex2reg(Sym index) {
        if (index.getType() == 0) {
            Namespace reg = getReg();
            addText(new Assign(reg, index.getValue() * 4));
            //addText(new Calculate(reg, reg, new Namespace(2, 1), "sll"));
            return reg;
        } else if (index.getType() == 1) {
            Namespace i = sym2reg(index);
            addText(new Calculate(i, i, new Namespace(2, 1), "sll"));
            return i;
        } else {
            Namespace i = rsym2ns(index);
            if (i.getType() == 1) {
                Namespace reg = getReg();
                addText(new Assign(reg, i.getValue()));
                return reg;
            }
            addText(new Calculate(i, i, new Namespace(2, 1), "sll"));
            return i;
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
                if (tar != null && tar.getType() == 1) {
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
        Sym cmpa = condBranch.getCmpa();
        Sym cmpb = condBranch.getCmpb();
        String type = condBranch.getType();
        Namespace a = rsym2ns(cmpa);
        Namespace b = rsym2ns(cmpb);
        if (a.getType() == 1) {
            Namespace reg = getReg();
            addText(new Assign(reg, a.getValue()));
            a = reg;
        }
        if (b.getType() == 1) {
            Namespace reg = getReg();
            addText(new Assign(reg, b.getValue()));
            b = reg;
        }
        freeReg(a.getReg());
        freeReg(b.getReg());
        Namespace label = new Namespace(condBranch.getLabel().getName());
        addText(new BrJump(a, b, label, type));
    }

    private void scanJump(Jump jump) {
        addText(new BrJump(new Namespace(jump.getLabel().getName()), "j"));
    }

    private void scanFuncCall(FuncCall funcCall) {
        ArrayList<Sym> params = funcCall.getParams();
        ArrayList<Var> fparams = funcCall.getFunc().getParams();
        int regbase = stackaddr;
        //push possible useful reg
        ArrayList<Namespace> usingregs = getUsingRegs();
        for (Namespace reg : usingregs) {
            addText(new LoadStore(reg, null, new Namespace(regbase, 1), 3));
            regbase += 4;
        }
        //push cur ret addr
        addText(new LoadStore(new Namespace(31, 0), null, new Namespace(regbase, 1), 3));
        regbase += 4;
        int curbase = regbase;
        //push rparams
        for (int i = 0; i < params.size(); i++) {
            Namespace ns = null;
            Boolean flag = false;
            if (funcstack.size() > 0) {
                for (Var var : funcstack.get(funcstack.size() - 1).getParams()) {
                    String varname = var.getName();
                    if (params.get(i).getType() == 3) {
                        String paraname = null;
                        if (params.get(i).getArrayname() != null)
                            paraname = params.get(i).getArrayname().getName();
                        if (paraname != null && paraname.equals(varname)) {
                            ns = rsym2ns2(params.get(i), 0);
                            flag = true;
                            break;
                        }
                    } else {
                        String paraname = null;
                        if (params.get(i).getArrayname() != null)
                            paraname = params.get(i).getArrayname().getName();
                        if (paraname != null && paraname.equals(varname)) {
                            ns = rsym2ns2(params.get(i), 0);
                            flag = true;
                            break;
                        }
                    }
                    if (params.get(i).getType() == 3 || params.get(i).getType() == 4) continue;
                    if (params.get(i).getSymbol() == null) break;
                    String name1 = params.get(i).getSymbol().getName();
                    if (varname.equals(name1)) {
                        ns = rsym2ns2(params.get(i), 0);
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag) {
                ns = rsym2ns(params.get(i));
            }
            if (ns.getType() == 1) {
                if (ns.getValue() > stackbase) {
                    Namespace reg = getReg();
                    addText(new Calculate(reg, new Namespace(29, 0), new Namespace(stackbase - ns.getValue(), 1), "+"));
                    ns = reg;
                } else {
                    Namespace reg = getReg();
                    addText(new Assign(reg, ns.getValue()));
                    ns = reg;
                }
            }
            addText(new LoadStore(ns, null, new Namespace(curbase, 1), 3));
            curbase += 4;
        }
        addText(new StackManage(4, curbase - stackbase));
        //jump to func pos
        addText(new BrJump(new Namespace(funcCall.getFunc().getName()), "jal"));
        //reset rparams
        addText(new StackManage(5, curbase - stackbase));
        //reset sp
        regbase -= 4;
        addText(new LoadStore(new Namespace(31, 0), null, new Namespace(regbase, 1), 0));
        regbase -= 4;
        //reset possible useful reg
        for (int i = usingregs.size() - 1; i >= 0; i--) {
            addText(new LoadStore(usingregs.get(i), null, new Namespace(regbase, 1), 0));
            regbase -= 4;
        }
        Namespace lns = lsym2ns(funcCall.getLsym());
        if (lns != null) {
            if (lns.getType() == 1) {
                addloadstore(new Namespace(2, 0), reg0, lns, 0);
            } else {
                addText(new Assign(lns, new Namespace(2, 0)));
            }
        }
    }

    private void scanFuncRet(FuncRet funcRet) {
        if (funcRet != null && funcRet.getSym() != null) {
            Namespace rns = rsym2ns(funcRet.getSym());
            curfunccodes.add(new Assign(new Namespace(2, 0), rns));
        }
        curfunccodes.add(new BrJump(new Namespace(31, 0), "jr"));
    }

    private void scanFuncParam(FuncParam funcParam) {
    }

    private void scanFuncDecl(FuncDecl funcDecl) {
        Func func = funcDecl.getFunc();
        curfunccodes.add(new LabelMipsCode(funcDecl.getFunc().getName() + ":\n"));
        blockstack.add(new Block("func"));
        funcstack.add(func);
        curLevel++;
        ArrayList<Var> params = func.getParams();
        int sz = params.size();
        for (int i = 0; i < sz; i++) {
            params.get(i).setAddr(-((sz - i) * 4));
            params.get(i).setReg(name2reg(params.get(i).toString() + params.get(i).getLevel()));
            if (params.get(i).getReg() > 0) {
                addloadstore(new Namespace(params.get(i).getReg(), 0),
                        null, new Namespace(params.get(i).getAddr(), 1), 0);
            }
            addVarSym(params.get(i));
        }
        addFuncSym(funcDecl.getFunc());
    }

    private int name2reg(String name) {
        for (int reg : globalreg2sym.keySet()) {
            if (globalreg2sym.get(reg).toString().equals(name)) {
                return reg;
            }
        }
        return -1;
    }

    private void scanArrayLoadStore(ArrayLoadStore arrayLoadStore) {
        if (arrayLoadStore.getType() == 0) {
            Sym index1 = arrayLoadStore.getIndex1();
            Var array = arrayLoadStore.getArray();
            Sym rsym = arrayLoadStore.getRsym();
            Namespace addr = lsym2ns(new Sym(array));
            Namespace rreg = rsym2ns(rsym);
            if (rreg != null && rreg.getType() == 1) {
                int value = rreg.getValue();
                rreg = getReg();
                addText(new Assign(rreg, value));
            }
            Namespace indexreg = transIndex2reg(index1);
            if (addr.getValue() < 0) {
                Namespace reg = getReg();
                addloadstore(reg, reg, addr, 0);
                addText(new Calculate(reg, reg, indexreg, "+"));
                addloadstore(rreg, reg, new Namespace(0, 1), 3);
                freeReg(reg.getReg());
                freeReg(rreg.getReg());
                if (indexreg != null)
                    freeReg(indexreg.getReg());
            } else {
                addloadstore(rreg, indexreg, addr, 3);
                freeReg(rreg.getReg());
                if (indexreg != null)
                    freeReg(indexreg.getReg());
            }

        } else if (arrayLoadStore.getType() == 2) {
            Sym index1 = arrayLoadStore.getIndex1();
            Var array = arrayLoadStore.getArray();
            Sym lsym = arrayLoadStore.getLsym();
            Namespace raddr = lsym2ns(new Sym(array));
            if (raddr.getValue() > 0) {
                Namespace laddr = lsym2ns(lsym);
                if (laddr != null && laddr.getType() == 1) {
                    Namespace rreg = getReg();
                    Namespace indexreg = transIndex2reg(index1);
                    addloadstore(rreg, indexreg, raddr, 0);
                    addloadstore(rreg, reg0, laddr, 3);
                    //tmpreg2sym.put(rreg.getReg(), lsym);
                    freeReg(rreg.getReg());
                } else {
                    //Namespace rreg = getReg();
                    Namespace indexreg = transIndex2reg(index1);
                    addloadstore(laddr, indexreg, raddr, 0);
                    //tmpreg2sym.put(rreg.getReg(), lsym);
                }
            } else {
                if (raddr.getType() == 1) {
                    Namespace laddr = lsym2ns(lsym);
                    if (laddr != null && laddr.getType() == 1) {
                        Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        Namespace reg = getReg();
                        addloadstore(reg, null, new Namespace(raddr.getValue(), 1), 0);
                        addText(new Calculate(reg, reg, indexreg, "+"));
                        addloadstore(rreg, reg, new Namespace(0, 1), 0);
                        addloadstore(rreg, reg0, laddr, 3);
                        //tmpreg2sym.put(rreg.getReg(), lsym);
                        freeReg(reg.getReg());
                        freeReg(rreg.getReg());
                    } else {
                        //Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        Namespace reg = getReg();
                        addloadstore(reg, null, new Namespace(raddr.getValue(), 1), 0);
                        addText(new Calculate(reg, reg, indexreg, "+"));
                        addloadstore(laddr, reg, new Namespace(0, 1), 0);
                        //tmpreg2sym.put(rreg.getReg(), lsym);
                        freeReg(reg.getReg());
                    }
                } else {
                    Namespace laddr = lsym2ns(lsym);
                    if (laddr != null && laddr.getType() == 1) {
                        Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        addText(new Calculate(raddr, raddr, indexreg, "+"));
                        addloadstore(rreg, raddr, new Namespace(0, 1), 0);
                        addloadstore(rreg, reg0, laddr, 3);
                        //tmpreg2sym.put(rreg.getReg(), lsym);
                        freeReg(rreg.getReg());
                    } else {
                        //Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        addText(new Calculate(raddr, raddr, indexreg, "+"));
                        addloadstore(laddr, raddr, new Namespace(0, 1), 0);
                        //tmpreg2sym.put(rreg.getReg(), lsym);
                    }
                }
            }
        }
    }

    private void scanArrayDecl(ArrayDecl arrayDecl) {
        Var array = (Var) arrayDecl.getSym().getSymbol();
        if (funcBlocks.get(curFunc).isMain()) {
            if (arrayDecl.getHasInitVal()) {
                array.setAddr(fp);
                addVarSym(array);
                Namespace addr = new Namespace(fp, 1);
                for (Sym sym : arrayDecl.getArrayval()) {
                    if (sym == null) continue;
                    if (sym.getType() == 0) {
                        Namespace reg = num2reg(new Namespace(sym.getValue(), 1));
                        addloadstore(reg, reg0, addr, 3);
                        fp += 4;
                        addr = new Namespace(fp, 1);
                    } else {
                        Namespace reg = rsym2ns(sym);
                        addloadstore(reg, reg0, addr, 3);
                        fp += 4;
                        addr = new Namespace(fp, 1);
                    }
                }
                fp += arrayDecl.getSpace();
            } else {
                array.setAddr(fp);
                addVarSym(array);
                Namespace addr = new Namespace(fp, 1);
                fp += arrayDecl.getSpace();
            }
        } else {
            if (arrayDecl.getHasInitVal()) {
                addVarSym(array);
                int baseaddr = stackaddr + (arrayDecl.getSpace() - 4);
                array.setAddr(baseaddr);
                Namespace addr = new Namespace(baseaddr, 1);
                for (Sym sym : arrayDecl.getArrayval()) {
                    if (sym == null) continue;
                    if (sym.getType() == 0) {
                        Namespace reg = num2reg(new Namespace(sym.getValue(), 1));
                        addloadstore(reg, reg0, addr, 3);
                        baseaddr -= 4;
                        addr = new Namespace(baseaddr, 1);
                    } else {
                        Namespace reg = rsym2ns(sym);
                        addloadstore(reg, reg0, addr, 3);
                        baseaddr -= 4;
                        addr = new Namespace(baseaddr, 1);
                    }
                }
                stackaddr += arrayDecl.getSpace();
            } else {
                addVarSym(array);
                int baseaddr = stackaddr + (arrayDecl.getSpace() - 4);
                array.setAddr(baseaddr);
                stackaddr += arrayDecl.getSpace();
            }
        }
        addVarSym(array);
    }

    private Namespace num2reg(Namespace value) {
        Namespace reg = getReg();
        addText(new Assign(reg, value.getValue()));
        return reg;
    }

    private void scanDecl(Decl decl) {
        Var var = (Var) decl.getSym().getSymbol();
        if (funcBlocks.get(curFunc).isMain()) {
            var.setAddr(fp);
            addVarSym(var);
            Namespace rns = rsym2ns(decl.getRsym());
            if (rns != null && rns.getType() == 1) {
                int value = rns.getValue();
                if (var.getReg() > 0) {
                    addText(new Assign(new Namespace(var.getReg(), 0), value));
                    return;
                }
                rns = getReg();
                addText(new Assign(rns, value));
            }
            if (var.getReg() > 0) {
                addText(new Assign(new Namespace(var.getReg(), 0), rns));
                return;
            }
            addText(new LoadStore(rns, reg0, new Namespace(fp, 1), 3));
            if (rns != null)
                freeReg(rns.getReg());
            fp += 4;
        } else {
            var.setAddr(stackaddr);
            addVarSym(var);
            Namespace rns = rsym2ns(decl.getRsym());
            if (rns != null && rns.getType() == 1) {
                int value = rns.getValue();
                if (var.getReg() > 0) {
                    addText(new Assign(new Namespace(var.getReg(), 0), value));
                    return;
                }
                rns = getReg();
                addText(new Assign(rns, value));
            }
            if (var.getReg() > 0) {
                addText(new Assign(new Namespace(var.getReg(), 0), rns));
                return;
            }
            addloadstore(rns, reg0, new Namespace(stackaddr, 1), 3);
            if (rns != null)
                freeReg(rns.getReg());
            stackaddr += 4;
        }
        addVarSym(var);
    }

    private void scanLabel(Label label) {
        String[] names = label.getName().split("_");
        String ident = names[0];
        if (label.getName().length() > 12 && label.getName().substring(0, 12).equals("mainfunc_ret")) {
            addText(new LabelMipsCode("text_end" + label.getName().substring(12) + ": \n"));
            addText(new Syscall(3));
            return;
        }
        if (names[1].equals("func")) {
            if (names[names.length - 1].equals("end")) {
                if (!(curfunccodes.get(curfunccodes.size() - 1) instanceof BrJump)) {
                    curfunccodes.add(new BrJump(new Namespace(31, 0), "jr"));
                }
                blockstack.remove(curLevel--);
                funcstack.remove(funcstack.size() - 1);
                tmpreg2sym = new HashMap<>();
                curfunccodes.add(new LabelMipsCode(label.toString()));
                stackaddr = stackbase;
            } else {
                curfunccodes.add(new LabelMipsCode(label.toString()));
                if (stackaddr > stackbase) {
                    addText(new StackManage(5, stackaddr - stackbase));
                }
            }
            return;
        }
        if (!names[1].equals("main") && !ident.equals("decline")) {
            addText(new LabelMipsCode(label.toString()));
        }
        if (ident.equals("block")) {
            String type = names[names.length - 1];
            if (type.equals("begin")) {
                blockstack.add(new Block("main"));
                curLevel++;
            } else if (type.equals("end")) {
                blockstack.remove(blockstack.size() - 1);
                curLevel--;
                tmpreg2sym = new HashMap<>();
            }
        }
    }

    private Var findVarInAllTable(String name) {
        for (int i = curLevel; i >= 0; i--) {
            Var ret = (Var) blockstack.get(i).findVar1(name);
            if (ret != null) return ret;
        }
        return null;
    }

    void addFuncSym(Func func) {
        blockstack.get(0).addSym(func);
    }

    private Func findFuncInAllTable(String name) {
        Func ret = (Func) blockstack.get(0).findFunc(name);
        if (ret != null) return ret;
        return null;
    }

    public void setMipsout() {
        for (MipsCode mipsCode : datacodes) {
            addMipsCode(mipsCode);
        }
        ArrayList<MipsCode> funccodes = new ArrayList<>();
        for (FuncBlock funcBlock : funcBlocks) {
            if (funcBlock.isMain()) {
                mipsCodes.add(new LabelMipsCode(".text\n"));
                mipsCodes.addAll(funcBlock.getFuncmipscodes());
            } else {
                funccodes.addAll(funcBlock.getFuncmipscodes());
            }
        }
        for (MipsCode mipsCode : funccodes) {
            addMipsCode(mipsCode);
        }
        StringBuilder res = new StringBuilder("");
        for (MipsCode mipsCode : mipsCodes) {
            res.append(mipsCode.toString());
        }
        mipsout = res.toString();
    }


    public String getMipsout() {
        return mipsout;
    }

    void transFunc() {
        resetTmpRegpool();
        resetGlobalRegpool();
        curfunccodes = new ArrayList<>();
        FuncBlock funcBlock = funcBlocks.get(curFunc);
        ArrayList<BasicBlock> basicBlocks = funcBlock.getBasicBlocks();
        globalreg2sym = funcBlock.getGlobalReg2sym();
        for (int i = 0; i < basicBlocks.size(); i++) {
            transbasicblock(basicBlocks.get(i));
        }
        funcBlocks.get(curFunc).setFuncmipscodes(curfunccodes);
    }

    private void transbasicblock(BasicBlock basicBlock) {
        ArrayList<IRCode> ircodes = basicBlock.getIrCodes();
        for (int i = 0; i < ircodes.size(); i++) {
            //System.out.println(i);
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
            //System.out.println(ircodes.get(i));
            //释放无用的临时寄存器
            HashSet<Sym> killls = basicBlock.getKillSymls(i);
            HashSet<Integer> regs = new HashSet<>(tmpreg2sym.keySet());
            for (int reg : regs) {
                if (killls.contains(tmpreg2sym.get(reg))) {
                    tmpreg2sym.remove(reg);
                    regpool.put(reg, false);
                }
                if (!tmpreg2sym.containsKey(reg)) {
                    regpool.put(reg, false);
                }
            }
            for (int reg : tmpregs) {
                if (!tmpreg2sym.containsKey(reg)) {
                    regpool.put(reg, false);
                }
            }
        }
    }
}
