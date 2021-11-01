package frontend;

import com.sun.org.apache.xpath.internal.functions.FuncFalse;
import frontend.llvmir.*;

import java.util.ArrayList;

public class Translater {
    private ASTNode ast;
    private ArrayList<IRCode> irCodes = new ArrayList<>();
    private String IROutput = "";
    private SymbolTable symbolTable = new SymbolTable("global", 0);
    private int regNum = 5;
    private int curLevel;
    private ArrayList<SymbolTable> beginSymStack = new ArrayList<>();
    int curaddr = 0;
    //%0->$0 %1->1 %2->stack
    private Namespace one = new Namespace("1", 2);
    private Namespace zero = new Namespace("0", 2);
    private Namespace oneRegName = new Namespace(1, 1);
    private Namespace stackRegName = new Namespace(2, 1);
    private ArrayList<Integer> codeLabels = new ArrayList<>();
    private ArrayList<Integer> whileLabels = new ArrayList<>();
    private ArrayList<Integer> funcalls = new ArrayList<>();
    private int labelNum = 0;
    private int stackSpace = 4000;

    public Translater(ASTNode ast) {
        this.ast = ast;
        beginSymStack.add(symbolTable);
        visit(ast);
        setIROutput();
    }

    private void visit(ASTNode ast) {
        irCodes.add(new CalcuIR("add", oneRegName, zero, one));
        for (ASTNode astNode : ast.getAstChildNodes()) {
            if (astNode.getType().equals("Decl")) {
                decl2symbol(astNode);
            } else if (astNode.getType().equals("FuncDef")) {
                visitFunc(astNode);
            } else if (astNode.getType().equals("MainFuncDef")) {
                beginSymStack.add(new SymbolTable("main", curLevel));
                curLevel++;
                visitBlock(astNode.getChild(4));
            }
        }
    }

    private void visitBlock(ASTNode block) {
        if (block.getAstChildNodes().size() <= 2) return;
        for (ASTNode node : block.getAstChildNodes()) {
            if (node.getType().equals("{") || node.getType().equals("}")) continue;
            if (node.getChild(0).getType().equals("Decl")) {
                decl2symbol(node.getChild(0));
            } else {
                visitStmt(node.getChild(0));
            }
        }
        beginSymStack.remove(curLevel--);
    }

    private void visitStmt(ASTNode stmt) {
        ASTNode first = stmt.getChild(0);
        if (first.getType().equals("LVal")) {
            if (stmt.getAstChildNodes().size() == 1 && stmt.getChild(1).getType().equals(";")) return;
            if (stmt.getChild(2).getType().equals("Exp")) {
                String name = stmt.getChild(0).getChild(0).getName();
                SymbolTable sym = findVarInAllTable(name);
                if (sym.getBracknum() == 0) {
                    Namespace value = visitAddExp(stmt.getChild(2).getChild(0));
                    Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                    Namespace ans = new Namespace(regNum++, 0);
                    irCodes.add(new LSAddrIR("1", value, addr, new Namespace("0", 2)));
                    regNum--;
                } else if (sym.getBracknum() == 1) {
                    Namespace dim = visitAddExp(stmt.getChild(0).getChild(2).getChild(0));
                    Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                    Namespace tmp = new Namespace(regNum++, 0);
                    irCodes.add(new CalcuIR("shl", tmp, dim, new Namespace("2", 2))); //tmp is address of target
                    Namespace num = visitAddExp(stmt.getChild(2).getChild(0));
                    irCodes.add(new LSAddrIR("1", num, addr, tmp));
                    regNum--;
                } else {
                    Namespace dim1 = visitAddExp(stmt.getChild(0).getChild(2).getChild(0));
                    Namespace dim2 = visitAddExp(stmt.getChild(0).getChild(5).getChild(0));
                    Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                    Namespace tmp = new Namespace(regNum++, 0);
                    irCodes.add(new CalcuIR("shl", tmp, dim2, new Namespace("2", 2))); //tmp is address of target
                    Namespace tmp1 = new Namespace(regNum++, 0);
                    irCodes.add(new CalcuIR("mul", tmp1, new Namespace(String.valueOf(sym.getN2()), 2), new Namespace("2", 2))); //tmp is address of target
                    irCodes.add(new CalcuIR("add", tmp, tmp, tmp1)); //tmp is address of target
                    Namespace num = visitAddExp(stmt.getChild(2).getChild(0));
                    irCodes.add(new LSAddrIR("1", num, addr, tmp));
                    regNum -= 2;
                }
            } else {
                String name = stmt.getChild(0).getChild(0).getName();
                SymbolTable sym = findVarInAllTable(name);
                if (sym.getBracknum() == 0) {
                    Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                    Namespace ans = new Namespace(regNum++, 0);
                    irCodes.add(new IOIR(ans, "0"));
                    irCodes.add(new LSAddrIR("1", ans, addr, new Namespace("0", 2)));
                    regNum--;
                } else if (sym.getBracknum() == 1) {
                    Namespace dim = visitAddExp(stmt.getChild(0).getChild(2).getChild(0));
                    Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                    Namespace tmp = new Namespace(regNum++, 0);
                    irCodes.add(new CalcuIR("shl", tmp, dim, new Namespace("2", 2))); //tmp is address of target
                    Namespace ans = new Namespace(regNum++, 0);
                    irCodes.add(new IOIR(ans, "0"));
                    irCodes.add(new LSAddrIR("1", ans, addr, tmp));
                    regNum -= 2;
                } else {
                    Namespace dim1 = visitAddExp(stmt.getChild(0).getChild(2).getChild(0));
                    Namespace dim2 = visitAddExp(stmt.getChild(0).getChild(5).getChild(0));
                    Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                    Namespace tmp = new Namespace(regNum++, 0);
                    irCodes.add(new CalcuIR("shl", tmp, dim2, new Namespace("2", 2))); //tmp is address of target
                    Namespace tmp1 = new Namespace(regNum++, 0);
                    irCodes.add(new CalcuIR("mul", tmp1, new Namespace(String.valueOf(sym.getN2()), 2), new Namespace("2", 2))); //tmp is address of target
                    irCodes.add(new CalcuIR("add", tmp, tmp, tmp1)); //tmp is address of target
                    Namespace ans = new Namespace(regNum++, 0);
                    irCodes.add(new IOIR(ans, "0"));
                    irCodes.add(new LSAddrIR("1", ans, addr, tmp));
                    regNum -= 3;
                }
            }
        } else if (first.getType().equals("printf")) {
            ArrayList<Namespace> namespaces = new ArrayList<>();
            Namespace namespace = new Namespace(stmt.getChild(2).getName(), 2);
            for (ASTNode astNode : stmt.getAstChildNodes()) {
                if (astNode.getType().equals("Exp")) {
                    namespaces.add(visitAddExp(astNode.getChild(0)));
                }
            }
            irCodes.add(new IOIR(namespace, namespaces, "1"));
        } else if (first.getType().equals("if")) {
            Namespace cond = visitLOrExp(stmt.getChild(2).getChild(0));
            if (cond.getType() == 2) {
                Namespace namespace = new Namespace(regNum++, 0);
                irCodes.add(new CalcuIR("add", namespace, new Namespace(0, 0), cond));
                cond = namespace;
            }
            int labelNumTmp = labelNum;
            codeLabels.add(-1);
            irCodes.add(new BRIR(cond, oneRegName, "bne", new Namespace(labelNum++, 3)));
            visitStmt(stmt.getChild(4));
            codeLabels.set(labelNumTmp, irCodes.size());
            if (stmt.getAstChildNodes().size() > 5) {
                visitStmt(stmt.getChild(6));
            }
            return;
        } else if (first.getType().equals("while")) {
            whileLabels.add(irCodes.size());
            Namespace cond = visitLOrExp(stmt.getChild(2).getChild(0));
            if (cond.getType() == 2) {
                Namespace namespace = new Namespace(regNum++, 0);
                irCodes.add(new CalcuIR("add", namespace, new Namespace(0, 0), cond));
                cond = namespace;
            }
            int labelNumTmp = labelNum;
            irCodes.add(new BRIR(cond, oneRegName, "bne", new Namespace(labelNum++, 3)));
            codeLabels.add(-1);
            visitStmt(stmt.getChild(4));
            codeLabels.set(labelNumTmp, irCodes.size());
            for (int i = codeLabels.size() - 1; i >= 0; i--) {
                if (codeLabels.get(i) == -1) {
                    codeLabels.set(i, irCodes.size());
                    break;
                }
            }
            whileLabels.remove(whileLabels.size() - 1);
            return;
        } else if (first.getType().equals("break")) {
            codeLabels.add(-1);
            irCodes.add(new BRIR("j", new Namespace(labelNum++, 3)));
            return;
        } else if (first.getType().equals("continue")) {
            irCodes.add(new BRIR("j", new Namespace(whileLabels.get(whileLabels.size() - 1), 2)));
            return;
        } else if (first.getType().equals("return")) {
            Namespace ans = null;
            Namespace retaddr = new Namespace(regNum++, 0);
            irCodes.add(new LSAddrIR("load", retaddr, stackRegName, zero));
            if (stmt.getAstChildNodes().size() > 2) {
                ans = visitAddExp(stmt.getChild(1).getChild(0));
                irCodes.add(new CalcuIR("sub", stackRegName, stackRegName, new Namespace("4", 2)));
                irCodes.add(new LSAddrIR("store", ans, stackRegName, zero));
            }
            irCodes.add(new BRIR("ret", retaddr));
            return;
        } else if (first.getType().equals("Block")) {
            beginSymStack.add(new SymbolTable("block", curLevel++));
            visitBlock(first);
        }
    }

    private int getScope() {
        return curLevel == 0 ? 1 : 0;
    }

    private void decl2symbol(ASTNode decl) {
        int line = 0;
        String type = "int"; //int void
        boolean isConst = false;
        String kind = "var";
        String name = "";
        ASTNode constdecl = decl.getAstChildNodes().get(0);
        for (int i = 0; i < constdecl.getAstChildNodes().size(); i++) {
            ASTNode astNode = constdecl.getAstChildNodes().get(i);
            if (astNode.getType().equals("ConstDef") || astNode.getType().equals("VarDef")) {
                int bracknum = 0;
                int k = 0, l = 0;
                int n1 = 0; //int[n1]
                int n2 = 0; //int[n1][n2]
                int scope = curLevel == 0 ? 1 : 0;
                ArrayList<Namespace> namespaces = new ArrayList<>();
                Namespace initvalue = null;
                Namespace tmp = null;
                Namespace valname = null;
                int addr = curaddr;
                boolean assign = false;
                for (int j = 0; j < astNode.getAstChildNodes().size(); j++) {
                    if (astNode.getChild(j).getType().equals("Ident")) {
                        line = astNode.getChild(j).getLine();
                        name = astNode.getChild(j).getName();
                        valname = new Namespace(name, getScope());
                    } else if (astNode.getChild(j).getType().equals("[")) {
                        bracknum++;
                        namespaces.add(visitAddExp(astNode.getChild(j + 1).getChild(0)));
                        j += 2;
                    } else if (astNode.getChild(j).getType().equals("InitVal") || astNode.getChild(j).getType().equals("ConstInitVal")) {
                        assign = true;
                        if (namespaces.size() == 1) {
                            tmp = new Namespace(regNum++, 0);
                            irCodes.add(new CalcuIR("shl", tmp, namespaces.get(0), new Namespace("2", 2)));
                            irCodes.add(new LSAddrIR("2", valname, tmp));
                        } else if (namespaces.size() == 2) {
                            tmp = new Namespace(regNum++, 0);
                            irCodes.add(new CalcuIR("shl", tmp, namespaces.get(1), new Namespace("2", 2)));
                            irCodes.add(new CalcuIR("mul", tmp, namespaces.get(0), tmp));
                            irCodes.add(new LSAddrIR("2", valname, tmp));
                        }
                        if (bracknum == 0) {
                            initvalue = visitAddExp(astNode.getChild(j).getChild(0).getChild(0));
                            irCodes.add(new LSAddrIR("2", valname, new Namespace("4", 2)));
                            irCodes.add(new LSAddrIR("1", initvalue, valname, new Namespace("0", 2)));
                            curaddr += 4;
                        } else if (bracknum == 1) {
                            for (int m = 0; m < astNode.getChild(j).getAstChildNodes().size(); m++) {
                                if (astNode.getChild(j).getChild(m).getAstChildNodes().size() > 0
                                        && (astNode.getChild(j).getChild(m).getChild(0).getType().equals("Exp")
                                        || astNode.getChild(j).getChild(m).getChild(0).getType().equals("ConstExp"))) {
                                    Namespace ret = visitAddExp(astNode.getChild(j).getChild(m).getChild(0).getChild(0));
                                    irCodes.add(new LSAddrIR("1", ret, valname, new Namespace(String.valueOf(4 * k), 2)));
                                    k++;
                                }
                            }
                            curaddr += 4 * k;
                            n1 = k;
                        } else {
                            int lmax = 0;
                            for (int m = 0; m < astNode.getChild(j).getAstChildNodes().size(); m++) {
                                if (astNode.getChild(j).getChild(m).getType().equals("InitVal")
                                        || astNode.getChild(j).getChild(m).getType().equals("ConstInitVal")) {
                                    ASTNode val = astNode.getChild(j).getChild(m);
                                    for (int t = 0; t < val.getAstChildNodes().size(); t++) {
                                        if (val.getChild(t).getType().equals("}")) {
                                            k++;
                                            lmax = l;
                                            l = 0;
                                        }
                                        if (val.getChild(t).getType().equals("InitVal") || val.getChild(t).getType().equals("ConstInitVal")) {
                                            Namespace ret = visitAddExp(val.getChild(t).getChild(0).getChild(0));
                                            irCodes.add(new LSAddrIR("1", ret, valname, new Namespace(String.valueOf(4 * k * lmax + l * 4), 2)));
                                            l++;
                                        }
                                    }
                                }
                            }
                            curaddr += 4 * k * lmax;
                            n1 = k;
                            n2 = lmax;
                        }
                    }
                }
                if (!assign) {
                    if (bracknum == 0) {
                        irCodes.add(new LSAddrIR("2", valname, new Namespace("4", 2)));
                    } else if (bracknum == 1) {
                        tmp = new Namespace(regNum++, 0);
                        irCodes.add(new CalcuIR("shl", tmp, namespaces.get(0), new Namespace("2", 2)));
                        irCodes.add(new LSAddrIR("2", valname, tmp));
                    } else {
                        tmp = new Namespace(regNum++, 0);
                        irCodes.add(new CalcuIR("shl", tmp, namespaces.get(1), new Namespace("2", 2)));
                        irCodes.add(new CalcuIR("mul", tmp, namespaces.get(0), tmp));
                        irCodes.add(new LSAddrIR("2", valname, tmp));
                    }
                }
                switch (bracknum) {
                    case 0:
                        addSymbol(new SymbolTable(line, type, isConst, n1, n2, curLevel, name, bracknum, kind, valname, addr));
                        break;
                    case 1:
                        addSymbol(new SymbolTable(line, type, isConst, n1, n2, curLevel, name, bracknum, kind, namespaces, valname, addr));
                        break;
                    case 2:
                        addSymbol(new SymbolTable(line, type, isConst, n1, n2, curLevel, name, bracknum, namespaces, valname, kind, addr));
                        break;
                }
            }
        }
    }

    private void visitFunc(ASTNode func) {
        for (ASTNode astNode : func.getAstChildNodes()) {
            if (astNode.getType().equals("Block")) {
                beginSymStack.add(new SymbolTable("block", curLevel++));
                visitBlock(astNode);
            }
        }
        //void might goto this way
        Namespace retaddr = new Namespace(regNum++, 0);
        irCodes.add(new LSAddrIR("load", retaddr, stackRegName, zero));
        irCodes.add(new BRIR("ret", retaddr));
    }

    private void funcdef2symbol(ASTNode funcdef) {
        int line = 0;
        String type = ""; //int void
        String kind = "func";
        String name = "";

        ArrayList<SymbolTable> params = new ArrayList<>();
        for (ASTNode astNode : funcdef.getAstChildNodes()) {
            switch (astNode.getType()) {
                case "FuncType":
                    type = astNode.getAstChildNodes().get(0).getName();
                    break;
                case "Ident":
                    name = astNode.getName();
                    line = astNode.getLine();
                    break;
                case "FuncFParams":
                    int line1 = 0;
                    String type1 = "int"; //int void
                    boolean isConst = false;
                    int n1 = 0; //int[n1]
                    int n2 = 0; //int[n1][n2]
                    String kind1 = "var";
                    String name1 = "";
                    int bracknum = 0;
                    boolean flag = false;
                    for (ASTNode astNode1 : astNode.getAstChildNodes()) {
                        if (astNode1.getType().equals("FuncFParam")) {
                            for (ASTNode astNode2 : astNode1.getAstChildNodes()) {
                                switch (astNode2.getType()) {
                                    case "Ident":
                                        line1 = astNode2.getLine();
                                        name1 = astNode2.getName();
                                        flag = true;
                                        break;
                                    case "[":
                                        bracknum++;
                                        break;
                                }
                            }
                        } else {
                            params.add(new SymbolTable(line1, type1, isConst, n1, n2, curLevel, name1, bracknum, kind1));
                            bracknum = 0;
                            flag = false;
                        }
                    }
                    if (flag) {
                        params.add(new SymbolTable(line1, type1, isConst, n1, n2, curLevel, name1, bracknum, kind1));
                    }
                    break;
            }
        }
        addSymbol(new SymbolTable(line, type, curLevel, name, kind, params, irCodes.size()));
    }

    private Namespace visitLOrExp(ASTNode lorexp) {
        if (lorexp.getAstChildNodes().size() > 1) {
            ASTNode rlorexp = new ASTNode("LOrExp", "", false, null);
            rlorexp.addChildNodes(lorexp.getAstChildNodes().subList(2, lorexp.getAstChildNodes().size()));
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            String op = lorexp.getAstChildNodes().get(1).getName();
            String type = op.equals("||") ? "or" : "or";
            CalcuIR calcuIR = new CalcuIR(type, namespace, visitLAndExp(lorexp.getAstChildNodes().get(0)), visitLOrExp(rlorexp));
            irCodes.add(calcuIR);
            regNum = beginReg + 1;
            return namespace;
        } else {
            return visitLAndExp(lorexp.getChild(0));
        }

    }

    private Namespace visitLAndExp(ASTNode landexp) {
        if (landexp.getAstChildNodes().size() > 1) {
            ASTNode rlandexp = new ASTNode("LAndExp", "", false, null);
            rlandexp.addChildNodes(landexp.getAstChildNodes().subList(2, landexp.getAstChildNodes().size()));
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            String op = landexp.getAstChildNodes().get(1).getName();
            String type = op.equals("&&") ? "and" : "and";
            CalcuIR calcuIR = new CalcuIR(type, namespace, visitEqExp(landexp.getAstChildNodes().get(0)), visitLAndExp(rlandexp));
            irCodes.add(calcuIR);
            regNum = beginReg + 1;
            return namespace;
        } else {
            return visitEqExp(landexp.getChild(0));
        }
    }

    //!= --> <||>
    private Namespace visitEqExp(ASTNode eqexp) {
        if (eqexp.getAstChildNodes().size() > 1) {
            ASTNode reqexp = new ASTNode("EqExp", "", false, null);
            reqexp.addChildNodes(eqexp.getAstChildNodes().subList(2, eqexp.getAstChildNodes().size()));
            int beginReg = regNum;
            String op = eqexp.getAstChildNodes().get(1).getName();
            Namespace cha = new Namespace(regNum++, 0);
            irCodes.add(new CalcuIR("sub", cha, visitRelExp(eqexp.getAstChildNodes().get(0)), visitEqExp(eqexp)));
            irCodes.add(new CalcuIR("sltu", cha, cha, new Namespace("1", 2))); //cha = l==r?1:0;
            if (op.equals("!=")) {
                irCodes.add(new CalcuIR("sub", cha, new Namespace("1", 2), cha)); //cha = 1-cha; <=> cha=!cha
            }
            regNum = beginReg + 1;
            return cha;
        } else {
            return visitRelExp(eqexp.getChild(0));
        }
    }

    // > <=> !(<=)
    // < <=> !(>=)
    private Namespace visitRelExp(ASTNode relexp) {
        if (relexp.getAstChildNodes().size() > 1) {
            ASTNode rrelexp = new ASTNode("RelExp", "", false, null);
            rrelexp.addChildNodes(relexp.getAstChildNodes().subList(2, relexp.getAstChildNodes().size()));
            int beginReg = regNum;
            String op = relexp.getAstChildNodes().get(1).getName();
            Namespace cmp = new Namespace(regNum++, 0);
            Namespace lname = visitCondAddExp(relexp.getAstChildNodes().get(0));
            Namespace rname = visitRelExp(rrelexp);
            if (op.equals(">") || op.equals("<=")) {
                irCodes.add(new CalcuIR("slt", cmp, rname, lname)); //cha = l==r?1:0;
                if (op.equals("<=")) {
                    irCodes.add(new CalcuIR("sub", cmp, new Namespace("1", 2), cmp)); //cha = l==r?1:0;
                }
            } else if (op.equals("<") || op.equals(">=")) {
                irCodes.add(new CalcuIR("slt", cmp, lname, rname));
                if (op.equals(">=")) {
                    irCodes.add(new CalcuIR("sub", cmp, new Namespace("1", 2), cmp)); //cha = l==r?1:0;
                }
            }
            regNum = beginReg + 1;
            return cmp;
        } else {
            return visitCondAddExp(relexp.getChild(0));
        }
    }

    private Namespace visitAddExp(ASTNode addexp) {
        if (addexp.getAstChildNodes().size() > 1) {
            ASTNode raddexp = new ASTNode("AddExp", "", false, null);
            raddexp.addChildNodes(addexp.getAstChildNodes().subList(2, addexp.getAstChildNodes().size()));
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            String op = addexp.getAstChildNodes().get(1).getName();
            String type = op.equals("+") ? "add" : "sub";
            CalcuIR calcuIR = new CalcuIR(type, namespace, visitMulExp(addexp.getAstChildNodes().get(0)), visitAddExp(raddexp));
            irCodes.add(calcuIR);
            regNum = beginReg + 1;
            return namespace;
        } else {
            return visitMulExp(addexp.getAstChildNodes().get(0));
        }
    }

    private Namespace visitCondAddExp(ASTNode addexp) {
        if (addexp.getAstChildNodes().size() > 1) {
            ASTNode raddexp = new ASTNode("AddExp", "", false, null);
            raddexp.addChildNodes(addexp.getAstChildNodes().subList(2, addexp.getAstChildNodes().size()));
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            String op = addexp.getAstChildNodes().get(1).getName();
            String type = op.equals("+") ? "add" : "sub";
            CalcuIR calcuIR = new CalcuIR(type, namespace, visitMulExp(addexp.getAstChildNodes().get(0)), visitAddExp(raddexp));
            irCodes.add(calcuIR);
            irCodes.add(new CalcuIR("sltu", namespace, new Namespace(0, 0), namespace));
            regNum = beginReg + 1;
            return namespace;
        } else {
            return visitMulExp(addexp.getAstChildNodes().get(0));
        }
    }

    private Namespace visitMulExp(ASTNode mulexp) {
        if (mulexp.getAstChildNodes().size() > 1) {
            ASTNode rmulexp = new ASTNode("MulExp", "", false, null);
            rmulexp.addChildNodes(mulexp.getAstChildNodes().subList(2, mulexp.getAstChildNodes().size()));
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            String op = mulexp.getAstChildNodes().get(1).getName();
            String type = op.equals("*") ? "mul" : op.equals("/") ? "sdiv" : "srem";
            CalcuIR calcuIR = new CalcuIR(type, namespace, visitUnaryExp(mulexp.getAstChildNodes().get(0)), visitMulExp(rmulexp));
            irCodes.add(calcuIR);
            regNum = beginReg + 1;
            return namespace;
        } else {
            return visitUnaryExp(mulexp.getAstChildNodes().get(0));
        }
    }

    private Namespace visitUnaryExp(ASTNode unaryexp) {
        ASTNode op = unaryexp.getAstChildNodes().get(0);
        boolean neg = false;
        boolean not = false;
        int i = 0;
        while (unaryexp.getAstChildNodes().get(i).getType().equals("UnaryOp")) {
            if (unaryexp.getAstChildNodes().get(i).getAstChildNodes().get(0).getName().equals("-")) neg = !neg;
            else if (unaryexp.getAstChildNodes().get(i).getAstChildNodes().get(0).getName().equals("!")) not = !not;
            i++;
        }
        if (i > 0) {
            if (unaryexp.getAstChildNodes().get(0).getType().equals("Ident")) {
                //todo func call
                String name = unaryexp.getAstChildNodes().get(0).getName();
                SymbolTable func = findFuncInAllTable(name);
                irCodes.add(new BRIR("j", new Namespace(func.getAddr(), 2)));
                irCodes.add(new CalcuIR("sub", stackRegName, stackRegName, new Namespace("4", 2)));
                irCodes.add(new LSAddrIR("store", new Namespace(String.valueOf(irCodes.size()), 2), stackRegName, zero));
                Namespace ans = new Namespace(regNum++, 0);
                if (func.getType().equals("int")) {
                    irCodes.add(new LSAddrIR("load", ans, stackRegName, zero));
                    irCodes.add(new CalcuIR("add", stackRegName, stackRegName, new Namespace("4", 2)));
                }
                irCodes.add(new CalcuIR("add", stackRegName, stackRegName, new Namespace("4", 2)));
                return ans;
            }
            ASTNode node = new ASTNode("UnaryExp", "", false, null);
            node.addChildNodes(unaryexp.getAstChildNodes().subList(i, unaryexp.getAstChildNodes().size()));
            CalcuIR calcuIR = null;
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            if (neg) {
                irCodes.add(new CalcuIR("sub", namespace, new Namespace(0, 0), visitUnaryExp(node)));
            } else if (not) {
                irCodes.add(new CalcuIR("sltu", namespace, visitUnaryExp(node), oneRegName));
                irCodes.add(new CalcuIR("sub", namespace, new Namespace("1", 2), namespace));
            }
            regNum = beginReg + 1;
            return namespace;
        } else {
            return visitPrimaryExp(unaryexp.getAstChildNodes().get(0));
        }
    }

    private Namespace visitPrimaryExp(ASTNode primaryexp) {
        if (primaryexp.getAstChildNodes().get(0).getType().equals("(")) {
            return visitAddExp(primaryexp.getAstChildNodes().get(1).getAstChildNodes().get(0));
        } else if (primaryexp.getAstChildNodes().get(0).getType().equals("LVal")) {
            String name = primaryexp.getChild(0).getChild(0).getName();
            SymbolTable sym = findVarInAllTable(name);
            if (sym.getBracknum() == 0) {
                Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                Namespace ans = new Namespace(regNum++, 0);
                irCodes.add(new LSAddrIR("0", ans, addr, new Namespace("0", 2)));
                return ans;
            } else if (sym.getBracknum() == 1) {
                Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                Namespace num = visitAddExp(primaryexp.getChild(0).getChild(2).getChild(0));
                Namespace ans = new Namespace(regNum++, 0);
                irCodes.add(new CalcuIR("shl", ans, num, new Namespace("2", 2)));
                irCodes.add(new LSAddrIR("0", ans, addr, ans));
                return ans;
            } else {
                Namespace addr = new Namespace(String.valueOf(sym.getAddr()), 2);
                Namespace num1 = visitAddExp(primaryexp.getChild(0).getChild(2).getChild(0));
                Namespace num2 = visitAddExp(primaryexp.getChild(0).getChild(5).getChild(0));
                Namespace ans = new Namespace(regNum++, 0);
                Namespace tmp = new Namespace(regNum++, 0);
                int n1 = sym.getN1();
                int n2 = sym.getN2();
                irCodes.add(new CalcuIR("mul", ans, num1, new Namespace(String.valueOf(4 * n2), 2)));
                irCodes.add(new CalcuIR("shl", tmp, num2, new Namespace(String.valueOf(2), 2)));
                irCodes.add(new CalcuIR("add", ans, ans, tmp));
                irCodes.add(new LSAddrIR("0", ans, addr, ans));
                return ans;
            }
        } else {
            String name = primaryexp.getChild(0).getChild(0).getChild(0).getName();
            return new Namespace(name, 2);
        }
    }

    public void setIROutput() {
        for (IRCode irCode : irCodes) {
            if (irCode instanceof BRIR) {
                BRIR brir = ((BRIR) irCode);
                if (brir.getLabel().getType() == 3) {
                    int num = brir.getLabel().getRegNum();
                    String label = String.valueOf(codeLabels.get(num));
                    brir.getLabel().setName(label);
                    brir.getLabel().setType(2);
                }
            }
            IROutput += irCode.toString() + "\n";
        }
    }

    public String getIROutput() {
        return IROutput;
    }

    private void addSymbol(SymbolTable symbolTable) {
        ExcNode ret = this.beginSymStack.get(curLevel).addSymbol(symbolTable);
    }

    private SymbolTable findFuncInAllTable(String name) {
        for (int i = curLevel; i >= 0; i--) {
            SymbolTable ret = beginSymStack.get(i).findFuncInAllTable(name);
            if (ret != null) return ret;
        }
        return null;
    }

    private SymbolTable findVarInAllTable(String name) {
        for (int i = curLevel; i >= 0; i--) {
            SymbolTable ret = beginSymStack.get(i).findVarInAllTable(name);
            if (ret != null) return ret;
        }
        return null;
    }
}
