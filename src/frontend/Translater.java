package frontend;

import frontend.llvmir.CalcuIR;
import frontend.llvmir.IRCode;
import frontend.llvmir.LSAddrIR;
import frontend.llvmir.Namespace;

import java.util.ArrayList;

public class Translater {
    private ASTNode ast;
    private ArrayList<IRCode> irCodes = new ArrayList<>();
    private String IROutput = "";
    private SymbolTable symbolTable = new SymbolTable("global", 0);
    private int regNum = 0;
    private int curLevel;
    private ArrayList<SymbolTable> beginSymStack = new ArrayList<>();
    int curaddr = 0;

    public Translater(ASTNode ast) {
        this.ast = ast;
        beginSymStack.add(symbolTable);
        visit(ast);
        setIROutput();
    }

    private void visit(ASTNode ast) {
        for (ASTNode astNode : ast.getAstChildNodes()) {
            if (astNode.getType().equals("Decl")) {
                decl2symbol(astNode);
            } else if (astNode.getType().equals("FuncDef")) {
                funcdef2symbol(astNode);
            } else if (astNode.getType().equals("MainFuncDef")) {
                beginSymStack.add(new SymbolTable("main", curLevel));
                curLevel++;
                visitBlock(astNode.getChild(4));
                beginSymStack.remove(curLevel);
                curLevel--;
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
    }

    private void visitStmt(ASTNode stmt) {

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
                for (int j = 0; j < astNode.getAstChildNodes().size(); j++) {
                    if (astNode.getChild(j).getType().equals("Ident")) {
                        line = astNode.getChild(j).getLine();
                        name = astNode.getChild(j).getName();
                    } else if (astNode.getChild(j).getType().equals("[")) {
                        bracknum++;
                        namespaces.add(visitAddExp(astNode.getChild(j + 1).getChild(0)));
                        j += 2;
                    } else if (astNode.getChild(j).getType().equals("InitVal") || astNode.getChild(j).getType().equals("ConstInitVal")) {
                        valname = new Namespace(name, getScope());
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
        addSymbol(new SymbolTable(line, type, curLevel, name, kind, params));
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
                //TODO func call
                return null;
            }
            ASTNode node = new ASTNode("UnaryExp", "", false, null);
            node.addChildNodes(unaryexp.getAstChildNodes().subList(i, unaryexp.getAstChildNodes().size()));
            CalcuIR calcuIR = null;
            int beginReg = regNum;
            Namespace namespace = new Namespace(regNum++, 0);
            if (neg) {
                irCodes.add(new CalcuIR("sub", namespace, new Namespace("0", 2), visitUnaryExp(node)));
            } else if (not) {
                //TODO ! generate
                return null;
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
