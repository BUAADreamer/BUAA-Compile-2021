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
    private int fp = 0x10040000;
    private int sp = 0x7fffeffc;
    private int textaddr = 0x00400000;
    private ArrayList<Boolean> regMap = new ArrayList<>();
    private HashMap<String, Namespace> tmpname2reg = new HashMap<>();
    private ArrayList<MipsCode> funcodes = new ArrayList<>();
    private Namespace reg0 = new Namespace(0, 0);
    private int strnum = 0;
    private Boolean main = false;
    private ArrayList<Func> funcstack = new ArrayList<>();

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
        if (lname != null && lname.type == 0) {
            if (exp.getOp().equals("=")) {
                addText(new Assign(lname, rname1));
            } else if (exp.getOp().equals("%") || exp.getOp().equals("*") || exp.getOp().equals("/")) {
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
                if (exp.getOp().equals("%")) {
                    addText(new MTF(reg2, "mfhi"));
                } else if (exp.getOp().equals("*")) {
                    addText(new MTF(reg2, "mflo"));
                } else {
                    addText(new MTF(reg2, "mflo"));
                }
                if (lname.getType() == 1) {
                    addText(new LoadStore(reg2, reg0, lname, 3));
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
                addText(new LoadStore(rname1, reg0, lname, 3));
            } else if (exp.getOp().equals("%") || exp.getOp().equals("*") || exp.getOp().equals("/")) {
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
                if (exp.getOp().equals("%")) {
                    addText(new MTF(reg2, "mfhi"));
                } else if (exp.getOp().equals("*")) {
                    addText(new MTF(reg2, "mflo"));
                } else {
                    addText(new MTF(reg2, "mflo"));
                }
                if (lname.getType() == 1) {
                    addText(new LoadStore(reg2, reg0, lname, 3));
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
                addText(new LoadStore(reg, reg0, lname, 3));
            } else {
                Namespace reg = getReg();
                if (rname1.getType() == 1) {
                    Namespace reg1 = getReg();
                    addText(new Assign(reg1, rname1));
                    rname1 = reg1;
                }
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
                Namespace reg = getReg();
                tmpname2reg.put(sym.toString(), reg);
                return reg;
            }
        } else {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var == null) return null;
            int addr = var.getAddr();
            return new Namespace(addr, 1);
        }
    }

    private Namespace lsym2ns1(Sym sym) {
        if (sym == null) return null;
        if (sym.getType() == 1) {
            if (sym.toString().charAt(0) == '%' || sym.toString().charAt(0) == '@') {
                Var var = findVarInAllTable(sym.toString().substring(1));
                int addr = var.getAddr();
                if (addr < 0) {
                    Namespace reg = getReg();
                    addText(new LoadStore(reg, reg, new Namespace(addr, 1), 0));
                    return reg;
                }
                return new Namespace(addr, 1);
            } else {
                Namespace reg = getReg();
                tmpname2reg.put(sym.toString(), reg);
                return reg;
            }
        } else {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var == null) return null;
            int addr = var.getAddr();
            if (addr < 0) {
                Namespace reg = getReg();
                addText(new LoadStore(reg, reg, new Namespace(addr, 1), 0));
                return reg;
            }
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
                int addr = var.getAddr();
                Namespace reg1 = getReg();
                addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
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
                Namespace reg = tmpname2reg.get(sym.toString());
                return reg;
            }
        } else if (sym.getType() == 2) {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var == null) return null;
            int addr = var.getAddr();
            Namespace reg1 = getReg();
            addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
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
                int addr = var.getAddr();
                addr -= offset;
                Namespace reg1 = getReg();
                addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
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
                Namespace reg = tmpname2reg.get(sym.toString());
                return reg;
            }
        } else if (sym.getType() == 2) {
            Var var = findVarInAllTable(sym.toString().substring(1));
            if (var == null) return null;
            int addr = var.getAddr();
            addr -= offset;
            Namespace reg1 = getReg();
            addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
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
            addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
            return reg1;
        } else {
            Namespace index = transIndex2reg(sym.getIndex());
            Var var = findVarInAllTable(sym.getArrayname().getName());
            int addr = var.getAddr();
            addr -= offset;
            Namespace reg1 = getReg();
            addText(new LoadStore(reg1, reg0, new Namespace(addr, 1), 0));
            addText(new Calculate(reg1, reg1, index, "+"));
            return index;
        }
        return null;
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
        Func func = findFuncInAllTable(funcCall.getFunc().getName());
        Namespace addr = new Namespace(func.getAddr(), 1);
        ArrayList<Sym> params = funcCall.getParams();
        ArrayList<Var> fparams = funcCall.getFunc().getParams();
        //push possible useful reg
        ArrayList<String> names = new ArrayList<>(tmpname2reg.keySet());
        for (String name : tmpname2reg.keySet()) {
            addText(new StackManage(tmpname2reg.get(name), 0));
            addText(new StackManage(2));
        }
        int offset = names.size() * 4 + 4;
        //push cur ret addr
        addText(new StackManage(new Namespace(31, 0), 0));
        addText(new StackManage(2));
        //push rparams
        for (int i = 0; i < params.size(); i++) {
            Namespace ns = null;
            Boolean flag = false;
            if (funcstack.size() > 0) {
                for (Var var : funcstack.get(funcstack.size() - 1).getParams()) {
                    String varname = var.getName();
                    String paraname = null;
                    if (params.get(i).getArrayname() != null)
                        paraname = params.get(i).getArrayname().getName();
                    if (paraname != null && paraname.equals(varname)) {
                        ns = rsym2ns2(params.get(i), offset);
                        flag = true;
                        break;
                    }
                    if (params.get(i).getType() == 3) continue;
                    if (params.get(i).getSymbol() == null) break;
                    String name1 = params.get(i).getSymbol().getName();
                    if (varname.equals(name1)) {
                        ns = rsym2ns2(params.get(i), offset);
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag) {
                ns = rsym2ns(params.get(i));
            }
            if (ns.getType() == 1) {
                Namespace reg = getReg();
                addText(new Assign(reg, ns.getValue()));
                ns = reg;
            }
            addText(new StackManage(ns, 0));
            addText(new StackManage(2));
            offset += 4;
        }
        //jump to func pos
        addText(new BrJump(new Namespace(funcCall.getFunc().getName()), "jal"));
        //reset rparams
        for (int i = 0; i < params.size(); i++) {
            addText(new StackManage(3));
        }
        //reset sp
        addText(new StackManage(3));
        addText(new StackManage(new Namespace(31, 0), 1));
        //reset possible useful reg
        for (int i = names.size() - 1; i >= 0; i--) {
            addText(new StackManage(3));
            addText(new StackManage(tmpname2reg.get(names.get(i)), 1));
        }
        Namespace lns = lsym2ns(funcCall.getLsym());
        if (lns != null) {
            if (lns.getType() == 1) {
                addText(new LoadStore(new Namespace(2, 0), reg0, lns, 0));
            } else {
                addText(new Assign(lns, new Namespace(2, 0)));
            }
        }
    }

    private void scanFuncRet(FuncRet funcRet) {
        if (funcRet != null && funcRet.getSym() != null) {
            Namespace rns = rsym2ns(funcRet.getSym());
            funcodes.add(new Assign(new Namespace(2, 0), rns));
        }
        funcodes.add(new BrJump(new Namespace(31, 0), "jr"));
    }

    private void scanFuncParam(FuncParam funcParam) {

    }

    Namespace transIndex2reg(Sym index) {
        if (index.getType() == 0) {
            Namespace reg = getReg();
            addText(new Assign(reg, index.getValue()));
            addText(new Calculate(reg, reg, new Namespace(2, 1), "sll"));
            return reg;
        } else if (index.getType() == 1) {
            Namespace i = tmpname2reg.get(index.toString());
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

    Namespace transIndex2reg2(Sym index) {
        if (index.getType() == 0) {
            Namespace reg = getReg();
            addText(new Assign(reg, index.getValue()));
            return reg;
        } else if (index.getType() == 1) {
            Namespace i = tmpname2reg.get(index.toString());
            return i;
        } else {
            Namespace i = lsym2ns(index);
            if (i.getType() == 1) {
                Namespace reg = getReg();
                addText(new LoadStore(reg, reg0, new Namespace(i.getValue(), 1), 0));
                return reg;
            }
            return i;
        }
    }

    private void addloadstore(Namespace reg1, Namespace reg2, Namespace addr, int type) {
        if (addr.getType() == 1) {
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
                addText(new LoadStore(rreg, reg, new Namespace(0, 1), 3));
                freeReg(reg.getReg());
                freeReg(rreg.getReg());
                freeReg(indexreg.getReg());
            } else {
                addloadstore(rreg, indexreg, addr, 3);
                freeReg(rreg.getReg());
                freeReg(indexreg.getReg());
            }

        } else if (arrayLoadStore.getType() == 1) {
            Sym index1 = arrayLoadStore.getIndex1();
            Sym index2 = arrayLoadStore.getIndex2();
            Var array = arrayLoadStore.getArray();
            Sym rsym = arrayLoadStore.getRsym();
            Sym tmp1 = arrayLoadStore.getTmp1();
            Sym tmp2 = arrayLoadStore.getTmp2();
            Namespace t1 = getReg();
            Namespace t2 = getReg();
            Namespace i1 = transIndex2reg2(index1);
            Namespace i2 = transIndex2reg2(index2);
            Namespace n2 = rsym2ns(new Sym(array.getN2()));
            Namespace addr = lsym2ns(new Sym(array));
            Namespace rreg = rsym2ns(rsym);
            if (n2.getType() == 1) {
                n2 = num2reg(n2);
            }
            if (rreg.getType() == 1) {
                Namespace reg = getReg();
                addText(new Assign(reg, rreg.getValue()));
                rreg = reg;
            }
            addText(new Calculate(t1, i1, n2, "*"));
            addText(new MTF(t1, "mflo"));
            addText(new Calculate(t2, t1, i2, "+"));
            addText(new Calculate(t2, t2, new Namespace(2, 1), "sll"));
            if (addr.getValue() < 0) {
                Namespace reg = getReg();
                addText(new LoadStore(reg, reg, addr, 0));
                addloadstore(reg, reg, addr, 0);
                addText(new Calculate(reg, reg, t2, "+"));
                addText(new LoadStore(rreg, reg, new Namespace(0, 1), 3));
                freeReg(reg.getReg());
                freeReg(rreg.getReg());
            } else {
                addloadstore(rreg, t2, addr, 3);
                if (rreg != null)
                    freeReg(rreg.getReg());
            }
            freeReg(t1.getReg());
            freeReg(t2.getReg());
            if (i1.getType() == 0) freeReg(i1.getReg());
            if (i2.getType() == 0) freeReg(i2.getReg());
            if (n2.getType() == 0) freeReg(n2.getReg());
            if (addr.getType() == 0) freeReg(addr.getReg());
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
                    addText(new LoadStore(rreg, reg0, laddr, 3));
                    freeReg(rreg.getReg());
                } else {
                    Namespace rreg = getReg();
                    Namespace indexreg = transIndex2reg(index1);
                    addloadstore(rreg, indexreg, raddr, 0);
                    tmpname2reg.put(lsym.toString(), rreg);
                }
            } else {
                Namespace reg = getReg();
                if (raddr.getType() == 1) {
                    addText(new LoadStore(reg, null, new Namespace(raddr.getValue(), 1), 0));
                    Namespace laddr = lsym2ns(lsym);
                    if (laddr != null && laddr.getType() == 1) {
                        Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        addText(new Calculate(reg, reg, indexreg, "+"));
                        addText(new LoadStore(rreg, reg, new Namespace(0, 1), 0));
                        addloadstore(rreg, reg0, laddr, 3);
                    } else {
                        Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        addText(new Calculate(reg, reg, indexreg, "+"));
                        addText(new LoadStore(rreg, reg, new Namespace(0, 1), 0));
                        tmpname2reg.put(lsym.toString(), rreg);
                    }
                    freeReg(reg.getReg());
                } else {
                    Namespace laddr = lsym2ns(lsym);
                    if (laddr != null && laddr.getType() == 1) {
                        Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        addText(new Calculate(raddr, raddr, indexreg, "+"));
                        addText(new LoadStore(rreg, raddr, new Namespace(0, 1), 0));
                        addloadstore(rreg, reg0, laddr, 3);
                    } else {
                        Namespace rreg = getReg();
                        Namespace indexreg = transIndex2reg(index1);
                        addText(new Calculate(raddr, raddr, indexreg, "+"));
                        addText(new LoadStore(rreg, raddr, new Namespace(0, 1), 0));
                        tmpname2reg.put(lsym.toString(), rreg);
                    }
                    freeReg(reg.getReg());
                }

            }
        } else {
            Sym index1 = arrayLoadStore.getIndex1();
            Sym index2 = arrayLoadStore.getIndex2();
            Var array = arrayLoadStore.getArray();
            Sym lsym = arrayLoadStore.getLsym();
            Sym tmp1 = arrayLoadStore.getTmp1();
            Sym tmp2 = arrayLoadStore.getTmp2();
            Namespace t1 = getReg();
            Namespace t2 = getReg();
            Namespace i1 = transIndex2reg2(index1);
            Namespace i2 = transIndex2reg2(index2);
            Namespace n2 = rsym2ns(new Sym(array.getN2()));
            Namespace addr = lsym2ns(new Sym(array));
            Namespace lreg = rsym2ns(lsym);
            if (lreg == null) {
                lreg = getReg();
                tmpname2reg.put(lsym.toString(), lreg);
            }
            if (lreg.getType() == 1) {
                Namespace reg = getReg();
                addText(new Assign(reg, lreg.getValue()));
                lreg = reg;
            }
            if (n2.getType() == 1) {
                n2 = num2reg(n2);
            }
            addText(new Calculate(t1, i1, n2, "*"));
            addText(new MTF(t1, "mflo"));
            addText(new Calculate(t2, t1, i2, "+"));
            addText(new Calculate(t2, t2, new Namespace(2, 1), "sll"));
            addloadstore(lreg, t2, addr, 0);
            if (addr.getValue() < 0) {
                addText(new Calculate(lreg, lreg, t2, "+"));
                addText(new LoadStore(lreg, lreg, new Namespace(0, 1), 0));
            }
            freeReg(t1.getReg());
            freeReg(t2.getReg());
            if (i1.getType() == 0) freeReg(i1.getReg());
            if (i2.getType() == 0) freeReg(i2.getReg());
            if (n2.getType() == 0) freeReg(n2.getReg());
            if (addr.getType() == 0) freeReg(addr.getReg());
        }
    }

    private Namespace num2reg(Namespace value) {
        Namespace reg = getReg();
        addText(new Assign(reg, value.getValue()));
        return reg;
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
                if (sym == null) continue;
                addData(new LabelMipsCode(String.format(".word %d\n",
                        sym.getValue())));
            }
        } else {
            array.setAddr(fp);
            addVarSym(array);
            Namespace addr = new Namespace(fp, 1);
            for (Sym sym : arrayDecl.getArrayval()) {
                if (sym == null) continue;
                if (sym.getType() == 0) {
                    Namespace reg = num2reg(new Namespace(sym.getValue(), 1));
                    addText(new LoadStore(reg, reg0, addr, 3));
                    fp += 4;
                    addr = new Namespace(fp, 1);
                } else {
                    Namespace reg = rsym2ns(sym);
                    addText(new LoadStore(reg, reg0, addr, 3));
                    fp += 4;
                    addr = new Namespace(fp, 1);
                }
            }
            fp += arrayDecl.getSpace();
        }
        addVarSym(array);
    }

    private void scanFuncDecl(FuncDecl funcDecl) {
        Func func = funcDecl.getFunc();
        blockstack.add(new Block("func"));
        funcstack.add(func);
        curLevel++;
        ArrayList<Var> params = func.getParams();
        int sz = params.size();
        for (int i = 0; i < sz; i++) {
            params.get(i).setAddr(-((sz - i) * 4));
            addVarSym(params.get(i));
        }
        funcDecl.getFunc().setAddr(textaddr + texts.size() * 4);
        addFuncSym(funcDecl.getFunc());
        funcodes.add(new LabelMipsCode(funcDecl.getFunc().getName() + ":\n"));
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
            addVarSym(var);
            Namespace rns = rsym2ns(decl.getRsym());
            if (rns != null && rns.getType() == 1) {
                int value = rns.getValue();
                rns = getReg();
                addText(new Assign(rns, value));
            }
            addText(new LoadStore(rns, reg0, new Namespace(fp, 1), 3));
            if (rns != null)
                freeReg(rns.getReg());
            fp += 4;
        }
        addVarSym(var);
    }

    private void scanLabel(Label label) {
        String[] names = label.getName().split("_");
        String ident = names[0];
        if (names[1].equals("main")) {
            main = true;
        }
        if (label.getName().length() > 12 && label.getName().substring(0, 12).equals("mainfunc_ret")) {
            addText(new LabelMipsCode("text_end" + label.getName().substring(12) + ": \n"));
            addText(new Syscall(3));
            return;
        }
        if (names[1].equals("func")) {
            if (names[names.length - 1].equals("end")) {
                if (!(funcodes.get(funcodes.size() - 1) instanceof BrJump)) {
                    funcodes.add(new BrJump(new Namespace(31, 0), "jr"));
                }
                blockstack.remove(curLevel--);
                funcstack.remove(funcstack.size() - 1);
                tmpname2reg = new HashMap<>();
            }
            funcodes.add(new LabelMipsCode(label.toString()));
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
        if (main == false) {
            funcodes.add(mipsCode);
            return;
        }
        texts.add(mipsCode);
    }

    public void setMipsout() {
        for (MipsCode mipsCode : datas) {
            addMipsCode(mipsCode);
        }
        for (MipsCode mipsCode : texts) {
            addMipsCode(mipsCode);
        }
        for (MipsCode mipsCode : funcodes) {
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
            if (i <= 4 || (i >= 29 && i <= 31)) continue;
            if (regMap.get(i)) {
                regMap.set(i, false);
                return new Namespace(i, 0);
            }
        }
        for (int i = 5; i <= 28; i++) {
            if (!tmpname2reg.values().contains(new Namespace(i, 0))) freeReg(i);
        }
        for (int i = 0; i < 32; i++) {
            if (i <= 4 || (i >= 29 && i <= 31)) continue;
            if (regMap.get(i)) {
                regMap.set(i, false);
                return new Namespace(i, 0);
            }
        }
        return new Namespace(27, 0);
    }

    private void freeReg(int reg) {
        if (tmpname2reg.values().contains(reg)) return;
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
