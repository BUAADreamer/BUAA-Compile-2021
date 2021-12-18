package frontend.irgen.optimize;

import backend.mips.MipsCode;
import frontend.stdir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FuncBlock {
    ArrayList<IRCode> irCodes;
    boolean main; //true-->main function false-->normal function
    ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
    String funcname;
    boolean better = true;
    ArrayList<Sym> arrayParams = new ArrayList<>();
    ArrayList<MipsCode> funcmipscodes = new ArrayList<>();
    //只考虑函数内部的变量，若有函数调用，则只考虑%开头的，若无函数调用，可以考虑全局变量
    HashMap<Sym, HashSet<Sym>> varconflictmap = new HashMap<>();
    ArrayList<Sym> allocateSyms = new ArrayList<>();

    public FuncBlock(ArrayList<IRCode> inirCodes, boolean ismain) {
        irCodes = inirCodes;
        main = ismain;
        if (!ismain) {
            funcname = ((FuncDecl) irCodes.get(0)).getFunc().getName();
            for (int i = 1; i < irCodes.size() && irCodes.get(i) instanceof FuncParam; i++) {
                FuncParam funcParam = (FuncParam) irCodes.get(i);
                if (funcParam.getType().equals("int[]") || funcParam.getType().equals("int[][]")) {
                    arrayParams.add(funcParam.getSym());
                }
            }
        } else {
            funcname = "main";
        }
        BasicBlockDivide basicBlockDivide = new BasicBlockDivide(irCodes);
        ArrayList<ArrayList<IRCode>> bblockircodes = basicBlockDivide.getBasicblocks();
        for (ArrayList<IRCode> bblcokircode : bblockircodes) {
            basicBlocks.add(new BasicBlock(bblcokircode, arrayParams));
            //System.out.println(basicBlocks.get(basicBlocks.size() - 1).toString());
        }
        generateFlow();
        if (!better) return;
        ArrayList<ArrayList<IRCode>> lastbblockcodes;
        while (true) {
            lastbblockcodes = new ArrayList<>();
            for (int i = 0; i < basicBlocks.size(); i++) {
                lastbblockcodes.add(new ArrayList<>(basicBlocks.get(i).getIrCodes()));
            }
            activeAnalysis();
            deleteDiedCodes();
            boolean allsame = true;
            for (int i = 0; i < basicBlocks.size(); i++) {
                if (!lastbblockcodes.get(i).equals(basicBlocks.get(i).getIrCodes())) {
                    allsame = false;
                    break;
                }
            }
            if (allsame) {
                break;
            }
        }
        deleteDiedWhile();
        calcuConflictMap();
        irCodes = new ArrayList<>();
        for (BasicBlock basicBlock : basicBlocks) {
            irCodes.addAll(basicBlock.getIrCodes());
        }
        //removedUnusedLabels();
        for (int i = 0; i < basicBlocks.size(); i++) {
            basicBlocks.get(i).killRegCalcu();
        }
    }

    private void removedUnusedLabels() {
        ArrayList<Integer> unusedls = new ArrayList<>();
        for (int i = 0; i < irCodes.size(); i++) {
            if (irCodes.get(i) instanceof Label) {
                if (((Label) irCodes.get(i)).getName().startsWith("while_")) {
                    boolean use = false;
                    for (int j = 0; j < irCodes.size(); j++) {
                        if (j == i) continue;
                        if (irCodes.get(j) instanceof CondBranch && ((CondBranch) irCodes.get(j)).getLabel().equals(irCodes.get(i))) {
                            use = true;
                            break;
                        }
                        if (irCodes.get(j) instanceof Jump && ((Jump) irCodes.get(j)).getLabel().equals(irCodes.get(i))) {
                            use = true;
                            break;
                        }
                    }
                    if (!use) {
                        unusedls.add(i);
                    }
                }
            }
        }
        ArrayList<IRCode> newircodes = new ArrayList<>();
        for (int i = 0; i < irCodes.size(); i++) {
            if (unusedls.contains(i)) continue;
            newircodes.add(irCodes.get(i));
        }
        irCodes = newircodes;
    }

    /**
     * 活跃变量的数据流分析 启发式算法
     */
    void activeAnalysis() {
        while (true) {
            boolean allsame = true;
            ArrayList<HashSet<Sym>> lastins = new ArrayList<>();
            for (BasicBlock basicBlock : basicBlocks) {
                lastins.add(new HashSet<>(basicBlock.getDuinlist()));
            }
            for (int i = 0; i < basicBlocks.size(); i++) {
                basicBlocks.get(i).calcuDefUse();
                ArrayList<Integer> prevs = basicBlocks.get(i).getPrevs();
                ArrayList<Integer> nextvs = basicBlocks.get(i).getNextvs();
                HashSet<Sym> syms = new HashSet<>();
                for (int nextv : nextvs) {
                    syms.addAll(basicBlocks.get(nextv).getDuinlist());
                }
                basicBlocks.get(i).calcuOuts(syms);
                basicBlocks.get(i).calcuIns();
                if (!basicBlocks.get(i).getDuinlist().equals(lastins.get(i))) {
                    allsame = false;
                }
            }
            if (allsame) {
                break;
            }
        }
        StringBuilder sb = new StringBuilder("");
        if (false) {
            for (int i = 0; i < basicBlocks.size(); i++) {
                sb.append("***********************").append("BasicBlock").append("-").append(i).append("begin***********************\n");
                sb.append("nextvs:").append(basicBlocks.get(i).getNextvs()).append("\n");
                sb.append("outs").append(basicBlocks.get(i).getDuoutlist()).append("\n");
                sb.append("ins:").append(basicBlocks.get(i).getDuinlist()).append("\n");
                sb.append("defs:").append(basicBlocks.get(i).getDefs()).append("\n");
                sb.append("uses:").append(basicBlocks.get(i).getUses()).append("\n");
                sb.append("duchains:").append(basicBlocks.get(i).getDuchains()).append("\n");
                sb.append("***********************").append("BasicBlock").append("-").append(i).append("end***********************\n");
            }
            System.out.println(sb);
        }
    }

    //删除死代码
    void deleteDiedCodes() {
        ArrayList<BasicBlock> newBasicBlocks = new ArrayList<>();
        for (int i = 0; i < basicBlocks.size(); i++) {
            basicBlocks.get(i).deleteDiedCodes();
        }
        /*
        for (BasicBlock basicBlock : basicBlocks) {
            if (!basicBlock.deleteDiedCodes()) {
                newBasicBlocks.add(basicBlock);
            } else {
                ArrayList<Integer> prevs = basicBlock.getPrevs();
                ArrayList<Integer> nextvs = basicBlock.getNextvs();
                IRCode thislastcode = basicBlock.getLastCode();
                IRCode thisfirstcode = basicBlock.getFirstCode();
                if (thisfirstcode instanceof Label) {
                    for (int prev : prevs) {
                        IRCode lastcode = basicBlocks.get(prev).getLastCode();
                        if ((lastcode instanceof Jump || lastcode instanceof CondBranch)) {
                            if (lastcode instanceof Jump && ((Jump) lastcode).getLabel().equals(thisfirstcode)) {
                                basicBlocks.get(prev).getIrCodes().remove(basicBlocks.get(prev).getIrCodes().size() - 1);
                            } else if (lastcode instanceof CondBranch && ((CondBranch) lastcode).getLabel().equals(thisfirstcode)) {
                                basicBlocks.get(prev).getIrCodes().remove(basicBlocks.get(prev).getIrCodes().size() - 1);
                            }
                        }
                    }
                }
            }
        }
        basicBlocks = newBasicBlocks;
        generateFlow();
         */
    }

    void deleteDiedWhile() {
        ArrayList<ArrayList<Integer>> whilebeginends = new ArrayList<>();
        for (int i = 0; i < basicBlocks.size(); i++) {
            ArrayList<Integer> nextvs = basicBlocks.get(i).getNextvs();
            for (int nextv : nextvs) {
                if (nextv < i) {
                    boolean flag = false;
                    for (int j = 0; j < whilebeginends.size(); j++) {
                        if (whilebeginends.get(j).get(0) == nextv) {
                            whilebeginends.get(j).set(1, i);
                            flag = true;
                            break;
                        }
                    }
                    if (flag) continue;
                    ArrayList<Integer> whilebeginend = new ArrayList<>();
                    whilebeginend.add(nextv);
                    whilebeginend.add(i);
                    whilebeginends.add(whilebeginend);
                }
            }
        }
        ArrayList<Integer> removed = new ArrayList<>();
        for (int i = 0; i < whilebeginends.size(); i++) {
            HashSet<Sym> defedsyms = new HashSet<>();
            int begin = whilebeginends.get(i).get(0);
            int end = whilebeginends.get(i).get(1);
            boolean isdied = true;
            boolean hasnoGlobal = true;
            for (int j = begin; j <= end; j++) {
                defedsyms.addAll(basicBlocks.get(j).getDefedls());
                for (Sym sym : basicBlocks.get(j).getDefedls()) {
                    if (sym.toString().charAt(0) == '@') {
                        hasnoGlobal = false;
                        if (!main) {
                            isdied = false;
                        }
                        break;
                    }
                    if (sym.getType() == 5 && arrayParams.contains(sym)) {
                        isdied = false;
                        break;
                    }
                }
                if (!isdied) break;
                for (IRCode irCode : basicBlocks.get(j).getIrCodes()) {
                    if (irCode instanceof FuncCall || irCode instanceof Printf || irCode instanceof FuncRet) {
                        isdied = false;
                        break;
                    }
                    if (irCode instanceof Exp && ((Exp) irCode).getRsym1().toString().equals("getint()")) {
                        isdied = false;
                        break;
                    }
                }
                if (!isdied) break;
            }
            //System.out.println(isdied);
            if (!isdied) continue;
            for (int j = end + 1; j < basicBlocks.size(); j++) {
                HashSet<Sym> usedsyms = basicBlocks.get(j).getUsedls();
                boolean hasuse = false;
                if (basicBlocks.get(j).isHasfunccall() && !hasnoGlobal) {
                    isdied = false;
                    break;
                }
                for (Sym sym : usedsyms) {
                    if (defedsyms.contains(sym)) {
                        hasuse = true;
                        break;
                    }
                }
                if (hasuse) {
                    isdied = false;
                    break;
                }
            }
            if (isdied) {
                for (int j = begin; j <= end; j++) {
                    removed.add(j);
                }
            }
        }
        //System.out.println(removed);
        //System.out.println(whilebeginends);
        ArrayList<BasicBlock> newbasicblocks = new ArrayList<>();
        for (int i = 0; i < basicBlocks.size(); i++) {
            if (removed.indexOf(i) == 0) {
                ArrayList<IRCode> smallirCodes = new ArrayList<>();
                smallirCodes.add(basicBlocks.get(i).getIrCodes().get(0));
                newbasicblocks.add(new BasicBlock(smallirCodes, arrayParams));
            }
            if (removed.contains(i)) continue;
            newbasicblocks.add(basicBlocks.get(i));
        }
        basicBlocks = newbasicblocks;
    }

    public boolean isMain() {
        return main;
    }

    /**
     * 生成流图，也就是这个里面每个基本块类里的prevs和nextvs
     */
    void generateFlow() {
        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock curblock = basicBlocks.get(i);
            IRCode lastcode = curblock.getLastCode();
            if (lastcode instanceof CondBranch) {
                getNext(((CondBranch) lastcode).getLabel(), i);
                if (i + 1 < basicBlocks.size()) {
                    basicBlocks.get(i).addnextv(i + 1);
                    basicBlocks.get(i + 1).addprev(i);
                }
            } else if (lastcode instanceof Jump) {
                getNext(((Jump) lastcode).getLabel(), i);
            } else {
                if (i + 1 < basicBlocks.size()) {
                    basicBlocks.get(i).addnextv(i + 1);
                    basicBlocks.get(i + 1).addprev(i);
                }
            }
        }
    }

    private boolean hasfunccall = false;
    private HashMap<Sym, Integer> sym2refcnt = new HashMap<>();

    boolean judgevar(Sym sym) {
        return sym.getType() == 2 && (sym.toString().charAt(0) == '%' || (sym.toString().charAt(0) == '@' && !hasfunccall));
    }

    void calcuConflictMap() {
        for (int i = 0; i < basicBlocks.size(); i++) {
            if (basicBlocks.get(i).isHasfunccall()) {
                hasfunccall = true;
                break;
            }
        }
        for (int i = 0; i < basicBlocks.size(); i++) {
            ArrayList<Sym> invars = new ArrayList<>(basicBlocks.get(i).getDuinlist());
            for (int j = 0; j < invars.size(); j++) {
                if (!judgevar(invars.get(j))) continue;
                if (!sym2refcnt.containsKey(invars.get(j))) {
                    sym2refcnt.put(invars.get(j), 1);
                } else {
                    sym2refcnt.put(invars.get(j), sym2refcnt.get(invars.get(j)) + 1);
                }
                if (!varconflictmap.containsKey(invars.get(j))) {
                    varconflictmap.put(invars.get(j), new HashSet<>());
                }
                for (int k = 0; k < invars.size(); k++) {
                    if (k == j || !judgevar(invars.get(k))) continue;
                    varconflictmap.get(invars.get(j)).add(invars.get(k));
                }
            }
        }
        while (varconflictmap.size() > 0) {
            //HashMap<Sym, HashSet<Sym>> newvarconflictmap = new HashMap<>();
            Sym chooseSym = null;
            Sym minsym = null;
            int mx = 0;
            int mi = 1000000;
            for (Sym sym : varconflictmap.keySet()) {
                if (varconflictmap.get(sym).size() < 8) {
                    if (sym2refcnt.get(sym) > mx) {
                        mx = sym2refcnt.get(sym);
                        chooseSym = sym;
                    }
                }
                if (sym2refcnt.get(sym) < mi) {
                    mi = sym2refcnt.get(sym);
                    minsym = sym;
                }
            }
            if (chooseSym != null) {
                allocateSyms.add(chooseSym);
            } else {
                chooseSym = minsym;
            }
            for (Sym sym : varconflictmap.keySet()) {
                varconflictmap.get(sym).remove(chooseSym);
            }
            varconflictmap.remove(chooseSym);
        }
        System.out.println(sym2refcnt);
        System.out.println(varconflictmap);
        System.out.println(allocateSyms);
    }

    public HashMap<Integer, Sym> getGlobalReg2sym() {
        HashMap<Integer, Sym> ans = new HashMap<>();
        for (int i = 0; i < allocateSyms.size(); i++) {
            ans.put(16 + i, allocateSyms.get(i));
        }
        return ans;
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }

    void getNext(Label label, int i) {
        for (int j = 0; j < basicBlocks.size(); j++) {
            //if (i == j) continue;
            IRCode first = basicBlocks.get(j).getFirstCode();
            if (first instanceof Label && first.equals(label)) {
                basicBlocks.get(i).addnextv(j);
                basicBlocks.get(j).addprev(i);
            }
        }
    }

    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public void setFuncmipscodes(ArrayList<MipsCode> funcmipscodes) {
        this.funcmipscodes = funcmipscodes;
    }

    public ArrayList<MipsCode> getFuncmipscodes() {
        return funcmipscodes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("");
        sb.append("=======================").append(funcname).append("-").append("begin=======================\n");
        for (int i = 0; i < basicBlocks.size(); i++) {
            sb.append("***********************").append("BasicBlock").append("-").append(i).append("begin***********************\n");
            sb.append(basicBlocks.get(i).toString());
            sb.append("***********************").append("BasicBlock").append("-").append(i).append("end***********************\n");
        }
        sb.append("=======================").append(funcname).append("-").append("end=======================\n");
        return sb.toString();
    }
}
