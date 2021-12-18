package frontend.irgen.optimize;

import backend.mips.MipsCode;
import frontend.irgen.symtable.Var;
import frontend.stdir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class BasicBlock {
    ArrayList<IRCode> irCodes;
    ArrayList<Integer> prevs = new ArrayList<>();
    ArrayList<Integer> nextvs = new ArrayList<>();
    HashSet<Sym> duinlist = new HashSet<>();
    HashSet<Sym> duoutlist = new HashSet<>();
    HashSet<Sym> defs = new HashSet<>();
    HashSet<Sym> uses = new HashSet<>();
    HashSet<Sym> usedls = new HashSet<>();
    HashSet<Sym> defedls = new HashSet<>();
    ArrayList<HashSet<Integer>> duchains = new ArrayList<>();
    ArrayList<Sym> arrayParams;
    private boolean hasfunccall = false;
    private ArrayList<HashSet<Sym>> killsyms = new ArrayList<>();
    HashMap<Sym, Integer> latestUse = new HashMap<>();
    ArrayList<HashSet<Sym>> killvardecl = new ArrayList<>();
    ArrayList<MipsCode> mipsCodes = new ArrayList<>();
    ArrayList<Sym> funccallparams = new ArrayList<>();

    public BasicBlock(ArrayList<IRCode> irCodes, ArrayList<Sym> arrayParams) {
        this.irCodes = irCodes;
        this.arrayParams = arrayParams;
    }

    public void clear() {
        duinlist.clear();
        duoutlist.clear();
        defs.clear();
        uses.clear();
        usedls.clear();
        defedls.clear();
        duchains.clear();
    }

    public void calcuDefUse() {
        clear();
        for (int i = 0; i < irCodes.size(); i++) {
            duchains.add(new HashSet<>());
        }
        for (int i = 0; i < irCodes.size(); i++) {
            IRCode irCode = irCodes.get(i);
            if (irCode instanceof ArrayDecl) {
                judgeDef(((ArrayDecl) irCode).getSym1());
                calcuDUChain(((ArrayDecl) irCode).getSym1(), i);
            } else if (irCode instanceof ArrayLoadStore) {
                ArrayLoadStore code = (ArrayLoadStore) irCode;
                if (code.getType() == 0) { //store
                    judgeUse(code.getRsym());
                    judgeUse(code.getIndex1());
                    judgeDef(code.getArraySym1());
                    judgeDef(code.getArraySym2());
                    calcuDUChain(code.getArraySym1(), i);
                } else { //load
                    judgeUse(code.getArraySym1());
                    judgeUse(code.getArraySym2());
                    judgeUse(code.getIndex1());
                    judgeDef(code.getLsym());
                    calcuDUChain(code.getLsym(), i);
                }
            } else if (irCode instanceof CondBranch) {
                judgeUse(((CondBranch) irCode).getCmpa());
                judgeUse(((CondBranch) irCode).getCmpb());
            } else if (irCode instanceof Decl) {
                judgeUse(((Decl) irCode).getRsym());
                judgeDef(((Decl) irCode).getSym());
                calcuDUChain(((Decl) irCode).getSym(), i);
            } else if (irCode instanceof Exp) {
                judgeUse(((Exp) irCode).getRsym1());
                judgeUse(((Exp) irCode).getRsym2());
                judgeDef(((Exp) irCode).getLsym());
                calcuDUChain(((Exp) irCode).getLsym(), i);
            } else if (irCode instanceof FuncCall) {
                ArrayList<Sym> params = ((FuncCall) irCode).getParams();
                for (Sym sym : params) {
                    judgeUse(sym);
                    if (sym.toString().charAt(0) == '&') {
                        judgeUse(sym.getIndex());
                        Var array = sym.getArrayname();
                        int len;
                        if (array.getType() == 1) {
                            len = array.getN1();
                        } else {
                            len = array.getN1() * array.getN2();
                        }
                        for (int b = 0; b < len; b++) {
                            judgeUse(new Sym(array.toString(), String.valueOf(b)));
                        }
                    }
                }
                hasfunccall = true;
            } else if (irCode instanceof FuncRet) {
                judgeUse(((FuncRet) irCode).getSym());
            } else if (irCode instanceof Printf) {
                ArrayList<Sym> params = ((Printf) irCode).getSyms();
                for (Sym sym : params) {
                    judgeUse(sym);
                }
            }
        }
    }

    public void killRegCalcu() {
        killvardecl = new ArrayList<>();
        //System.out.println(latestUse);
        for (int i = 0; i < irCodes.size(); i++) {
            HashSet<Sym> syms = new HashSet<>();
            for (Sym sym : latestUse.keySet()) {
                if (latestUse.get(sym) == i && !duoutlist.contains(sym)) {
                    syms.add(sym);
                }
            }
            killvardecl.add(syms);
        }
        //System.out.println(killvardecl);
    }

    public boolean isHasfunccall() {
        return hasfunccall;
    }

    public void addLatestUse(Sym sym, int pos) {
        if (latestUse.containsKey(sym) && latestUse.get(sym) < pos) {
            latestUse.put(sym, pos);
        }
        if (!latestUse.containsKey(sym)) {
            latestUse.put(sym, pos);
        }
    }

    void calcuDUChain(Sym sym, int cur) {
        for (int j = cur + 1; j < irCodes.size(); j++) {
            IRCode irCode = irCodes.get(j);
            if (irCode instanceof ArrayLoadStore) {
                ArrayLoadStore code = (ArrayLoadStore) irCode;
                if (code.getType() == 0) { //store
                    if (code.getRsym().equals(sym) || code.getIndex1().equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                } else { //load
                    if (code.getArraySym1().equals(sym) || code.getIndex1().equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                }
            } else if (irCode instanceof CondBranch) {
                if (((CondBranch) irCode).getCmpa().equals(sym)) {
                    duchains.get(cur).add(j);
                    addLatestUse(sym, j);
                }
                if (((CondBranch) irCode).getCmpb().equals(sym)) {
                    duchains.get(cur).add(j);
                    addLatestUse(sym, j);
                }
            } else if (irCode instanceof Decl) {
                if (((Decl) irCode).getRsym().equals(sym)) {
                    duchains.get(cur).add(j);
                    addLatestUse(sym, j);
                }
            } else if (irCode instanceof Exp) {
                if (((Exp) irCode).getRsym1() != null) {
                    if (((Exp) irCode).getRsym1().equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                }
                if (((Exp) irCode).getRsym2() != null) {
                    if (((Exp) irCode).getRsym2().equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                }
                if (((Exp) irCode).getLsym().equals(sym) && ((Exp) irCode).getRsym1().toString().equals("getint()")) {
                    duchains.get(cur).add(j);
                    addLatestUse(sym, j);
                }
            } else if (irCode instanceof FuncCall) {
                ArrayList<Sym> params = ((FuncCall) irCode).getParams();
                for (Sym sym1 : params) {
                    if (sym1.equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                    if (Sym.isVar(sym1) != null && Sym.isVar(sym1).equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                    if (sym1.toString().charAt(0) == '&') {
                        if (sym1.getIndex() != null && sym1.getIndex().equals(sym)) {
                            duchains.get(cur).add(j);
                            addLatestUse(sym, j);
                        }
                    }
                }
            } else if (irCode instanceof FuncRet) {
                if (((FuncRet) irCode).getSym() != null && ((FuncRet) irCode).getSym().equals(sym)) {
                    duchains.get(cur).add(j);
                    addLatestUse(sym, j);
                }
            } else if (irCode instanceof Printf) {
                ArrayList<Sym> params = ((Printf) irCode).getSyms();
                for (Sym sym1 : params) {
                    if (sym1.equals(sym)) {
                        duchains.get(cur).add(j);
                        addLatestUse(sym, j);
                    }
                }
            }
        }
    }

    void judgeUse(Sym sym) {
        Sym ans = Sym.isVar(sym);
        if (ans != null) {
            if (!defedls.contains(ans)) {
                uses.add(ans);
                usedls.add(ans);
            }
            usedls.add(ans);
        }
    }

    void judgeDef(Sym sym) {
        Sym ans = Sym.isVar(sym);
        if (ans != null) {
            if (!usedls.contains(ans)) {
                defs.add(ans);
                defedls.add(ans);
            }
            defedls.add(ans);
        }
    }

    void calcuOuts(HashSet<Sym> nextvins) {
        duoutlist.addAll(nextvins);
    }

    //in = use ∪ (out - def)
    void calcuIns() {
        HashSet<Sym> tmpduoutlist = new HashSet<>(duoutlist);
        //System.out.println(tmpduoutlist);
        tmpduoutlist.removeIf(sym -> defs.contains(sym));
        tmpduoutlist.addAll(uses);
        //System.out.println(tmpduoutlist);
        duinlist.addAll(tmpduoutlist);
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }

    public void addprev(int prev) {
        prevs.add(prev);
    }

    public void addnextv(int nextv) {
        nextvs.add(nextv);
    }

    public ArrayList<Integer> getNextvs() {
        return nextvs;
    }

    public ArrayList<Integer> getPrevs() {
        return prevs;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("");
        for (IRCode irCode : irCodes) {
            stringBuilder.append(irCode.toString());
        }
        return stringBuilder.toString();
    }

    public IRCode getLastCode() {
        return irCodes.get(irCodes.size() - 1);
    }

    public IRCode getFirstCode() {
        return irCodes.get(0);
    }

    public HashSet<Sym> getDuinlist() {
        return duinlist;
    }

    public HashSet<Sym> getDuoutlist() {
        return duoutlist;
    }

    boolean definouts(Sym sym) {
        return duoutlist.contains(sym) || arrayParams.contains(sym);
    }

    public void deleteDiedCodes() {
        ArrayList<IRCode> newircodes = new ArrayList<>();
//        for (int i = 0; i < irCodes.size(); i++) {
//
//        }
        boolean flag = true;
        for (int i = 0; i < irCodes.size(); i++) {
            IRCode irCode = irCodes.get(i);
            if (irCode instanceof ArrayLoadStore) {
                ArrayLoadStore code = (ArrayLoadStore) irCode;
                if (code.getType() == 0) { //store
//                    System.out.println(definouts(code.getArraySym2()));
//                    System.out.println(duoutlist);
//                    System.out.println(code.getArraySym2());
                    if (definouts(code.getArraySym1()) || definouts(code.getArraySym2()) || duchains.get(i).size() > 0 || (!code.getArraySym1().toString().isEmpty() && code.getArraySym1().toString().charAt(0) == '@')) {
                        newircodes.add(irCodes.get(i));
                    }
                } else { //load
                    if (definouts(code.getLsym()) || definouts(code.getArraySym2()) || duchains.get(i).size() > 0 || (!code.getLsym().toString().isEmpty() && code.getLsym().toString().charAt(0) == '@')) {
                        newircodes.add(irCodes.get(i));
                    }
                }
            } else if (irCode instanceof Decl) {
                if (true) {
                    newircodes.add(irCodes.get(i));
                    continue;
                }
                if (definouts(((Decl) irCode).getSym()) || duchains.get(i).size() > 0) {
                    newircodes.add(irCodes.get(i));
                }
            } else if (irCode instanceof Exp) {
                if (((Exp) irCode).getRsym1().toString().equals("getint()")) {
                    newircodes.add(irCodes.get(i));
                } else if (definouts(((Exp) irCode).getLsym()) || duchains.get(i).size() > 0) {
                    newircodes.add(irCodes.get(i));
                } else if (!((Exp) irCode).getLsym().toString().isEmpty() && ((Exp) irCode).getLsym().toString().charAt(0) == '@') {
                    newircodes.add(irCodes.get(i));
                }
            } else {
                newircodes.add(irCodes.get(i));
//                if (definouts(((FuncCall) irCode).getLsym())) {
//                    flag = false;
//                    break;
//                }
            }
        }
        irCodes = newircodes;
    }

    public boolean deleteDiedCodesOld() {
        ArrayList<IRCode> newircodes = new ArrayList<>();
        boolean flag = true;
        for (int i = 0; i < irCodes.size(); i++) {
            IRCode irCode = irCodes.get(i);
            if (irCode instanceof ArrayLoadStore) {
                ArrayLoadStore code = (ArrayLoadStore) irCode;
                if (code.getType() == 0) { //store
                    if (definouts(code.getArraySym1())) {
                        flag = false;
                        break;
                    }
                } else { //load
                    if (definouts(code.getLsym())) {
                        flag = false;
                        break;
                    }
                }
            } else if (irCode instanceof Decl) {
                if (definouts(((Decl) irCode).getSym())) {
                    flag = false;
                    break;
                }
            } else if (irCode instanceof Exp) {
                if (((Exp) irCode).getRsym1().toString().equals("getint()")) {
                    flag = false;
                    break;
                }
                if (definouts(((Exp) irCode).getLsym())) {
                    flag = false;
                    break;
                }
            } else if (irCode instanceof FuncCall) {
                flag = false;
                break;
//                if (definouts(((FuncCall) irCode).getLsym())) {
//                    flag = false;
//                    break;
//                }
            } else if (irCode instanceof Printf) {
                flag = false;
                break;
            } else if (irCode instanceof FuncRet) {
                flag = false;
                break;
            } else if (irCode instanceof Label && ((Label) irCode).getName().equals("mainfunc_ret_0")) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public HashSet<Sym> getDefs() {
        return defs;
    }

    public HashSet<Sym> getUses() {
        return uses;
    }

    public ArrayList<HashSet<Integer>> getDuchains() {
        return duchains;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock that = (BasicBlock) o;
        return Objects.equals(irCodes, that.irCodes) && Objects.equals(prevs, that.prevs) && Objects.equals(nextvs, that.nextvs) && Objects.equals(duinlist, that.duinlist) && Objects.equals(duoutlist, that.duoutlist) && Objects.equals(defs, that.defs) && Objects.equals(uses, that.uses) && Objects.equals(usedls, that.usedls) && Objects.equals(defedls, that.defedls) && Objects.equals(duchains, that.duchains);
    }

    @Override
    public int hashCode() {
        return Objects.hash(irCodes, prevs, nextvs, duinlist, duoutlist, defs, uses, usedls, defedls, duchains);
    }

    public HashSet<Sym> getDefedls() {
        return defedls;
    }

    public ArrayList<Sym> getArrayParams() {
        return arrayParams;
    }

    public HashSet<Sym> getUsedls() {
        return usedls;
    }

    public HashSet<Sym> getKillSymls(int i) {
        return killvardecl.get(i);
    }
}
