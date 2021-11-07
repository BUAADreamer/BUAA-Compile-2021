package frontend.irgen;

import frontend.irgen.symtable.Block;
import frontend.irgen.symtable.Func;
import frontend.irgen.symtable.Var;
import frontend.preprocess.ASTNode;
import frontend.stdir.*;

import javax.swing.*;
import java.util.ArrayList;

public class IRGenerater {
    private ASTNode ast;
    private ArrayList<IRCode> ircodes = new ArrayList<>();
    private String iroutput = "";
    private ArrayList<Block> blockstack = new ArrayList<>();
    private int curLevel = 0;
    private int regNum = 0;
    private Sym zero = new Sym(0);
    private Sym one = new Sym(1);
    private int iftag = 0;
    private int whiletag = 0;
    private int blocktag = 0;
    private ArrayList<Label> whilebegintags = new ArrayList<>();
    private ArrayList<Label> whileendtags = new ArrayList<>();

    public IRGenerater(ASTNode ast) {
        this.ast = ast;
        blockstack.add(new Block("global"));
        visit(ast);
        setIroutput();
    }

    private void visit(ASTNode ast) {
        addircode(new Label("decline_init_begin"));
        for (ASTNode astNode : ast.getAstChildNodes()) {
            if (astNode.getType().equals("Decl")) {
                visitDecl(astNode);
            } else if (astNode.getType().equals("FuncDef")) {
                visitFunc(astNode);
            } else if (astNode.getType().equals("MainFuncDef")) {
                addircode(new Label("decline_init_end"));
                blockstack.add(new Block("main"));
                addircode(new Label("block_main_begin"));
                curLevel++;
                visitBlock(astNode.getChild(4));
                addircode(new Label("block_main_end"));
            }
        }
    }

    private void visitBlock(ASTNode block) {
        if (block.getAstChildNodes().size() <= 2) return;
        for (ASTNode node : block.getAstChildNodes()) {
            if (node.getType().equals("{") || node.getType().equals("}")) continue;
            if (node.getChild(0).getType().equals("Decl")) {
                visitDecl(node.getChild(0));
            } else {
                visitStmt(node.getChild(0));
            }
        }
        blockstack.remove(curLevel--);
    }

    private void visitStmt(ASTNode stmt) {
        ASTNode first = stmt.getChild(0);
        if (first.getType().equals("LVal")) {
            if (stmt.getAstChildNodes().size() == 1) return;
            if (stmt.getChild(1).getType().equals(";")) {
                visitExp(stmt.getChild(0));
                return;
            }
            if (stmt.getChild(2).getType().equals("Exp")) {
                String name = stmt.getChild(0).getChild(0).getName();
                Var sym = findVarInAllTable(name);
                Sym value = visitExp(stmt.getChild(2));
                if (sym.getType() == 0) {
                    addircode(new Exp("=", new Sym(sym), value, null));
                } else if (sym.getType() == 1) {
                    Sym num = visitExp(stmt.getChild(0).getChild(2));
                    addircode(new ArrayLoadStore(value, num, sym, 0));
                } else {
                    Sym num1 = visitExp(stmt.getChild(0).getChild(2));
                    Sym num2 = visitExp(stmt.getChild(0).getChild(5));
                    Sym tmp2 = getTempVar();
                    Sym tmp1 = getTempVar();
                    addircode(new ArrayLoadStore(value, tmp1, tmp2, num1, num2, sym, 1));
                }
            } else {
                String name = stmt.getChild(0).getChild(0).getName();
                Var sym = findVarInAllTable(name);
                Sym value = new Sym("getint()");
                if (sym.getType() == 0) {
                    addircode(new Exp("=", new Sym(sym), value, null));
                } else if (sym.getType() == 1) {
                    Sym num = visitExp(stmt.getChild(0).getChild(2));
                    addircode(new ArrayLoadStore(value, num, sym, 0));
                } else {
                    Sym num1 = visitExp(stmt.getChild(0).getChild(2));
                    Sym num2 = visitExp(stmt.getChild(0).getChild(5));
                    Sym tmp2 = getTempVar();
                    Sym tmp1 = getTempVar();
                    addircode(new ArrayLoadStore(value, tmp1, tmp2, num1, num2, sym, 1));
                }
            }
        } else if (first.getType().equals("printf")) {
            ArrayList<Sym> syms = new ArrayList<>();
            Sym sym = new Sym(stmt.getChild(2).getName());
            for (ASTNode astNode : stmt.getAstChildNodes()) {
                if (astNode.getType().equals("Exp")) {
                    syms.add(visitExp(astNode));
                }
            }
            addircode(new Printf(syms, sym));
        } else if (first.getType().equals("if")) {
            Label beginlabel = new Label(String.format("if_%d_begin", ++iftag));
            Label endlabel = new Label(String.format("if_%d_end", iftag));
            visitCond(stmt.getChild(2), beginlabel, endlabel, iftag, "if");
            addircode(beginlabel);
            visitStmt(stmt.getChild(4));
            addircode(endlabel);
            if (stmt.getAstChildNodes().size() > 5) {
                visitStmt(stmt.getChild(6));
            }
            return;
        } else if (first.getType().equals("while")) {
            Label condbeginlabel = new Label(String.format("while_%d_cond_0", ++whiletag));
            Label beginlabel = new Label(String.format("while_%d_begin", whiletag));
            Label endlabel = new Label(String.format("while_%d_end", whiletag));
            visitCond(stmt.getChild(2), beginlabel, endlabel, whiletag, "while");
            addircode(beginlabel);
            whilebegintags.add(condbeginlabel);
            whileendtags.add(endlabel);
            visitStmt(stmt.getChild(4));
            whilebegintags.remove(whilebegintags.size() - 1);
            whileendtags.remove(whileendtags.size() - 1);
            addircode(new Jump(condbeginlabel));
            addircode(endlabel);
            return;
        } else if (first.getType().equals("break")) {
            addircode(new Jump(whileendtags.get(whileendtags.size() - 1)));
            return;
        } else if (first.getType().equals("continue")) {
            addircode(new Jump(whilebegintags.get(whilebegintags.size() - 1)));
            return;
        } else if (first.getType().equals("return")) {
            if (curLevel == 1) {
                addircode(new Label("ret_0"));
                return;
            }
            if (stmt.getChild(1).getType().equals(";")) {
                addircode(new FuncRet(null));
            }
            addircode(new FuncRet(visitExp(stmt.getChild(1))));
            return;
        } else if (first.getType().equals("Block")) {
            addircode(new Label(String.format("block_%d_begin", ++blocktag)));
            blockstack.add(new Block("block"));
            curLevel++;
            visitBlock(first);
            addircode(new Label(String.format("block_%d_end", blocktag)));
        }
    }

    private void visitCond(ASTNode cond, Label beginlabel, Label endlabel, int tag, String name) {
        visitLOrExp(cond.getChild(0), beginlabel, endlabel, tag, name);
    }

    private void visitLOrExp(ASTNode lorexp, Label beginlabel, Label endlabel, int tag, String name) {
        ArrayList<ASTNode> landexps = new ArrayList<>();
        int condtag = 0;
        for (ASTNode node : lorexp.getAstChildNodes()) {
            if (node.getType().equals("LAndExp")) {
                landexps.add(node);
            }
        }
        for (int i = 0; i < landexps.size(); i++) {
            if (i == landexps.size() - 1) {
                visitLAndExp(landexps.get(i), beginlabel, endlabel, new Label(String.format("cond_%s_%d_%d", name, tag, condtag)), null);
            } else {
                visitLAndExp(landexps.get(i), beginlabel, endlabel, new Label(String.format("cond_%s_%d_%d", name, tag, condtag)),
                        new Label(String.format("cond_if_%d_%d", tag, ++condtag)));
            }
        }
    }

    private void visitLAndExp(ASTNode landexp, Label beginlabel, Label endlabel, Label curlabel, Label nextlabel) {
        ArrayList<ASTNode> eqexps = new ArrayList<>();
        addircode(curlabel);
        int condtag = 0;
        for (ASTNode node : landexp.getAstChildNodes()) {
            if (node.getType().equals("EqExp")) {
                eqexps.add(node);
            }
        }
        for (int i = 0; i < eqexps.size(); i++) {
            if (i == eqexps.size() - 1 && nextlabel != null) {
                visitEqExp(eqexps.get(i), false, beginlabel);
            } else {
                if (nextlabel == null) {
                    visitEqExp(eqexps.get(i), true, endlabel);
                } else {
                    visitEqExp(eqexps.get(i), true, nextlabel);
                }
            }
        }
    }

    private Sym visitEqExp(ASTNode astNode, boolean not, Label nextlabel) {
        if (astNode.getAstChildNodes().size() == 3) {
            String type = astNode.getChild(1).getName().equals("==") ? "beq" : "bne";
            if (not) {
                if (type.equals("beq")) type = "bne";
                else type = "beq";
            }
            Sym lsym = visitRelExp(astNode.getChild(0));
            Sym rsym = visitRelExp(astNode.getChild(2));
            addircode(new CondBranch(lsym, rsym, type, nextlabel));
            return null;
        } else if (astNode.getAstChildNodes().size() == 1) {
            Sym rsym = one;
            if (not) {
                rsym = zero;
            }
            Sym lsym = visitRelExp(astNode.getChild(0));
            addircode(new CondBranch(lsym, rsym, "beq", nextlabel));
            return null;
        }
        return null;
    }

    private Sym visitRelExp(ASTNode relexp) {
        if (relexp.getAstChildNodes().size() > 1) {
            ASTNode rrelexp = new ASTNode("RelExp", "", false, null);
            rrelexp.addChildNodes(relexp.getAstChildNodes().subList(2, relexp.getAstChildNodes().size()));
            String op = relexp.getAstChildNodes().get(1).getName();
            Sym ret = getTempVar();
            Sym lsym = visitAddExp(relexp.getAstChildNodes().get(0));
            Sym rsym = visitRelExp(rrelexp);
            if (op.equals(">") || op.equals("<=")) {
                addircode(new Exp("slti", ret, rsym, lsym)); //cha = l==r?1:0;
                if (op.equals("<=")) {
                    addircode(new Exp("-", ret, one, ret)); //cha = l==r?1:0;
                }
            } else if (op.equals("<") || op.equals(">=")) {
                addircode(new Exp("slti", ret, lsym, rsym));
                if (op.equals(">=")) {
                    addircode(new Exp("-", ret, one, ret)); //cha = l==r?1:0;; //cha = l==r?1:0;
                }
            }
            return ret;
        } else {
            return visitAddExp(relexp.getChild(0));
        }
    }

    private void visitFunc(ASTNode funcdef) {
        String type = "int"; //void
        String name = "";
        ArrayList<Var> params = new ArrayList<>();
        curLevel++;
        blockstack.add(new Block("Func"));
        for (ASTNode astNode : funcdef.getAstChildNodes()) {
            switch (astNode.getType()) {
                case "FuncType":
                    type = astNode.getAstChildNodes().get(0).getName();
                    break;
                case "Ident":
                    name = astNode.getName();
                    break;
                case "FuncFParams":
                    boolean isconst = false;
                    int n1 = 0; //int[n1]
                    int n2 = 0; //int[n1][n2]
                    int type1 = 0;
                    String name1 = "";
                    int level = curLevel;
                    Boolean flag = false;
                    for (ASTNode astNode1 : astNode.getAstChildNodes()) {
                        if (astNode1.getType().equals("FuncFParam")) {
                            for (ASTNode astNode2 : astNode1.getAstChildNodes()) {
                                switch (astNode2.getType()) {
                                    case "Ident":
                                        name1 = astNode2.getName();
                                        break;
                                    case "[":
                                        type1++;
                                        break;
                                }
                            }
                            flag = true;
                        } else {
                            Var var = null;
                            if (type1 == 0) {
                                var = new Var(isconst, name1, level);
                            } else if (type1 == 1) {
                                var = new Var(n1, isconst, name1, level);
                            } else {
                                var = new Var(n1, n2, isconst, name1, level);
                            }
                            params.add(var);
                            addVarSym(var);
                            type1 = 0;
                            flag = false;
                        }
                    }
                    if (flag) {
                        Var var = null;
                        if (type1 == 0) {
                            var = new Var(isconst, name1, level);
                        } else if (type1 == 1) {
                            var = new Var(n1, isconst, name1, level);
                        } else {
                            var = new Var(n1, n2, isconst, name1, level);
                        }
                        params.add(var);
                        addVarSym(var);
                    }
                    break;
                case "Block":
                    Func func = new Func(name, params, type);
                    addFuncSym(func);
                    addircode(new FuncDecl(func, (type == "int") ? 1 : 0));
                    for (Var param : params) {
                        addircode(new FuncParam(new Sym(param)));
                    }
                    addircode(new Label("block_func_" + func.getName() + "_begin"));
                    visitBlock(astNode);
                    addircode(new Label("block_func_" + func.getName() + "_end"));
                    break;
            }
        }
    }

    private void visitDecl(ASTNode decl) {
        int n1 = 0;
        int n2 = 0;
        boolean isconst = false;
        int type = 0;
        String name = "";
        int level = curLevel;
        ASTNode specdecl = decl.getChild(0);
        for (int i = 0; i < specdecl.getAstChildNodes().size(); i++) {
            ASTNode astNode = specdecl.getChild(i);
            if (astNode.getType().equals("ConstDef") || astNode.getType().equals("VarDef")) {
                isconst = astNode.getType().equals("ConstDef");
                n1 = 0;
                n2 = 0;
                type = 0;
                ArrayList<Sym> initvalues = new ArrayList<>();
                ArrayList<Sym> syms = new ArrayList<>();
                Sym initvalue = null;
                for (int j = 0; j < astNode.getAstChildNodes().size(); j++) {
                    if (astNode.getChild(j).getType().equals("Ident")) {
                        name = astNode.getChild(j).getName();
                    } else if (astNode.getChild(j).getType().equals("[")) {
                        type++;
                        syms.add(visitExp(astNode.getChild(j + 1)));
                        j += 2;
                    } else if (astNode.getChild(j).getType().equals("InitVal") || astNode.getChild(j).getType().equals("ConstInitVal")) {
                        if (type == 0) {
                            initvalue = visitExp(astNode.getChild(j).getChild(0));
                        } else if (type == 1) {
                            for (int m = 0; m < astNode.getChild(j).getAstChildNodes().size(); m++) {
                                if (astNode.getChild(j).getChild(m).getAstChildNodes().size() > 0
                                        && (astNode.getChild(j).getChild(m).getChild(0).getType().equals("Exp")
                                        || astNode.getChild(j).getChild(m).getChild(0).getType().equals("ConstExp"))) {
                                    Sym ret = visitExp(astNode.getChild(j).getChild(m).getChild(0));
                                    initvalues.add(ret);
                                    n1++;
                                }
                            }
                        } else {
                            for (int m = 0; m < astNode.getChild(j).getAstChildNodes().size(); m++) {
                                if (astNode.getChild(j).getChild(m).getType().equals("InitVal")
                                        || astNode.getChild(j).getChild(m).getType().equals("ConstInitVal")) {
                                    ASTNode val = astNode.getChild(j).getChild(m);
                                    for (int t = 0; t < val.getAstChildNodes().size(); t++) {
                                        if (val.getChild(t).getType().equals("}")) {
                                            n1++;
                                        }
                                        if (val.getChild(t).getType().equals("InitVal") || val.getChild(t).getType().equals("ConstInitVal")) {
                                            Sym ret = visitExp(val.getChild(t).getChild(0));
                                            initvalues.add(ret);
                                            n2++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (n1 != 0) {
                    n2 /= n1;
                }
                if (type == 0) {
                    Var symTable = new Var(isconst, name, level);
                    addVarSym(symTable);
                    Sym var = new Sym(symTable);
                    if (initvalue == null) initvalue = zero;
                    addircode(new Decl(var, initvalue));
                } else if (type == 1) {
                    Var symTable = new Var(n1, isconst, name, level);
                    addVarSym(symTable);
                    Sym var = new Sym(symTable);
                    addircode(new ArrayDecl(var, initvalues, syms));
                } else {
                    Var symTable = new Var(n1, n2, isconst, name, level);
                    addVarSym(symTable);
                    Sym var = new Sym(symTable);
                    addircode(new ArrayDecl(var, initvalues, syms));
                }
            }
        }
    }

    private Sym visitExp(ASTNode exp) {
        return visitAddExp(exp.getChild(0));
    }

    private Sym visitAddExp(ASTNode addexp) {
        if (addexp.getAstChildNodes().size() > 1) {
            ASTNode raddexp = new ASTNode("AddExp", "", false, null);
            raddexp.addChildNodes(addexp.getAstChildNodes().subList(2, addexp.getAstChildNodes().size()));
            Sym rsym1 = visitMulExp(addexp.getChild(0));
            Sym rsym2 = visitAddExp(raddexp);
            String op = addexp.getAstChildNodes().get(1).getName();
            if (rsym1.getType() == rsym2.getType() && rsym1.getType() == 0) {
                return new Sym(calcu(rsym1.getValue(), rsym2.getValue(), op));
            }
            Sym sym = getTempVar();
            addircode(new Exp(op, sym, rsym1, rsym2));
            return sym;
        } else {
            return visitMulExp(addexp.getChild(0));
        }
    }

    private Sym visitMulExp(ASTNode mulexp) {
        if (mulexp.getAstChildNodes().size() > 1) {
            ASTNode rmulexp = new ASTNode("MulExp", "", false, null);
            rmulexp.addChildNodes(mulexp.getAstChildNodes().subList(2, mulexp.getAstChildNodes().size()));
            Sym rsym1 = visitUnaryExp(mulexp.getChild(0));
            Sym rsym2 = visitMulExp(rmulexp);
            String op = mulexp.getAstChildNodes().get(1).getName();
            if (rsym1.getType() == rsym2.getType() && rsym1.getType() == 0) {
                return new Sym(calcu(rsym1.getValue(), rsym2.getValue(), op));
            }
            Sym sym = getTempVar();
            addircode(new Exp(op, sym, rsym1, rsym2));
            return sym;
        } else {
            return visitUnaryExp(mulexp.getChild(0));
        }
    }

    private int calcu(int value, int value1, String op) {
        if (op.equals("+")) {
            return value + value1;
        } else if (op.equals("-")) {
            return value - value1;
        } else if (op.equals("*")) {
            return value * value1;
        } else if (op.equals("/")) {
            return value / value1;
        } else if (op.equals("%")) {
            return value % value1;
        }
        return 0;
    }

    private Sym visitUnaryExp(ASTNode unaryexp) {
        ASTNode op = unaryexp.getAstChildNodes().get(0);
        boolean neg = false;
        boolean not = false;
        int i = 0;
        while (unaryexp.getChild(0).getType().equals("UnaryOp")) {
            if (unaryexp.getChild(i).getChild(0).getName().equals("-")) neg = !neg;
            else if (unaryexp.getChild(i).getChild(0).getName().equals("!")) not = !not;
            i++;
            unaryexp = unaryexp.getChild(1);
        }
        if (i > 0) {
            if (neg) {
                Sym rsym = visitUnaryExp(unaryexp);
                if (rsym.getType() == 0) {
                    return new Sym(-rsym.getValue());
                }
                Sym sym = new Sym("t" + regNum++);
                addircode(new Exp("-", sym, zero, rsym));
                return sym;
            } else if (not) {
                Sym rsym = visitUnaryExp(unaryexp);
                Sym sym = getTempVar();
                addircode(new Exp("sltiu", sym, rsym, one));
                addircode(new Exp("-", sym, one, rsym));
                return sym;
            }
        } else {
            if (unaryexp.getAstChildNodes().get(0).getType().equals("Ident")) {
                String name = unaryexp.getAstChildNodes().get(0).getName();
                Func func = findFuncInAllTable(name);
                ArrayList<Sym> syms = new ArrayList<>();
                Sym lsym = getTempVar();
                for (ASTNode node : unaryexp.getAstChildNodes()) {
                    if (node.getType().equals("FuncRParams")) {
                        ASTNode funcrparams = node;
                        for (ASTNode exp : node.getAstChildNodes()) {
                            if (exp.getType().equals("Exp")) {
                                Sym sym = visitExp(exp);
                                syms.add(sym);
                            }
                        }
                    }
                }
                addircode(new FuncCall(lsym, func, syms));
                return lsym;
            }
            return visitPrimaryExp(unaryexp.getChild(0));
        }
        return null;
    }

    private Sym visitPrimaryExp(ASTNode primaryexp) {
        if (primaryexp.getAstChildNodes().get(0).getType().equals("(")) {
            return visitExp(primaryexp.getChild(1));
        } else if (primaryexp.getAstChildNodes().get(0).getType().equals("LVal")) {
            String name = primaryexp.getChild(0).getChild(0).getName();
            Var sym = findVarInAllTable(name);
            if (sym.getType() == 0) {
                return new Sym(sym);
            } else if (sym.getType() == 1) {

                if (primaryexp.getChild(0).getAstChildNodes().size() == 1) {
                    return new Sym("&" + sym);
                    /*
                    ArrayList<Sym> rparams = new ArrayList<>();
                    rparams.add(new Sym(sym.getN1()));
                    for (int i = 0; i < sym.getN1(); i++) {
                        Sym num = new Sym(i);
                        Sym tmp = getTempVar();
                        addircode(new ArrayLoadStore(tmp, num, sym, 2));
                        rparams.add(tmp);
                    }
                    return new Sym(rparams);
                     */
                }
                Sym num = visitExp(primaryexp.getChild(0).getChild(2));
                Sym tmp = getTempVar();
                addircode(new ArrayLoadStore(tmp, num, sym, 2));
                return tmp;
            } else {
                if (primaryexp.getChild(0).getAstChildNodes().size() == 1) {
                    /*
                    ArrayList<Sym> rparams = new ArrayList<>();
                    rparams.add(new Sym(sym.getN1()));
                    for (int i = 0; i < sym.getN1(); i++) {
                        for (int j = 0; j < sym.getN2(); j++) {
                            Sym num1 = new Sym(i);
                            Sym num2 = new Sym(j);
                            Sym tmp2 = getTempVar();
                            Sym tmp1 = getTempVar();
                            Sym tmp = getTempVar();
                            addircode(new ArrayLoadStore(tmp, tmp1, tmp2, num1, num2, sym, 3));
                            rparams.add(tmp);
                        }
                    }
                    return new Sym(rparams);
                    */
                    return new Sym("&" + sym);
                } else if (primaryexp.getChild(0).getAstChildNodes().size() == 4) {
                    ArrayList<Sym> rparams = new ArrayList<>();
                    Sym num1 = visitExp(primaryexp.getChild(0).getChild(2));
                    Sym temp = getTempVar();
                    if (num1.getType() == 0) {
                        temp = new Sym(calcu(num1.getValue(), sym.getN2(), "*"));
                    } else {
                        addircode(new Exp("*", temp, num1, new Sym(sym.getN2())));
                    }
                    return new Sym(String.format("&%s[%s]", sym, temp));
                }
                Sym num1 = visitExp(primaryexp.getChild(0).getChild(2));
                Sym num2 = visitExp(primaryexp.getChild(0).getChild(5));
                Sym tmp2 = getTempVar();
                Sym tmp1 = getTempVar();
                Sym tmp = getTempVar();
                addircode(new ArrayLoadStore(tmp, tmp1, tmp2, num1, num2, sym, 3));
                return tmp;
            }
        } else {
            String name = primaryexp.getChild(0).getChild(0).getChild(0).getName();
            return new Sym(Integer.valueOf(name));
        }
    }

    private void addircode(IRCode ircode) {
        ircodes.add(ircode);
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

    void addVarSym(Var var) {
        blockstack.get(curLevel).addSym(var);
    }

    void addFuncSym(Func func) {
        blockstack.get(0).addSym(func);
    }

    public void setIroutput() {
        StringBuilder res = new StringBuilder(iroutput);
        for (IRCode code : ircodes) {
            res.append(code.toString());
        }
        iroutput = res.toString();
    }

    Sym getTempVar() {
        return new Sym("t" + regNum++);
    }

    public String getIroutput() {
        return iroutput;
    }

    public ArrayList<IRCode> getIrcodes() {
        return ircodes;
    }
}
