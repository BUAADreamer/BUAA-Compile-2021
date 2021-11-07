package frontend.preprocess;

import java.util.ArrayList;
import java.util.Comparator;

public class Visitor {
    private SymbolTable symbolTable = new SymbolTable("global", 0);
    private ArrayList<ExcNode> excNodes;
    private ASTNode ast;
    private int curLevel = 0;
    private ArrayList<SymbolTable> beginSymStack = new ArrayList<>();
    private ArrayList<Integer> stackWhile = new ArrayList<>();
    private String excOutAns = "";
    ArrayList<ArrayList<SymbolTable>> stackFuncParams = new ArrayList<>();

    public Visitor(ASTNode ast, ArrayList<ExcNode> excNodes) {
        this.ast = ast;
        this.excNodes = excNodes;
        this.beginSymStack.add(symbolTable);
        visit(ast);
        setExc();
    }

    public String getExcOutAns() {
        return excOutAns;
    }

    private void setExc() {
        excNodes.sort(Comparator.comparingInt(ExcNode::getLine));
        StringBuilder sb = new StringBuilder(excOutAns);
        for (ExcNode excNode : excNodes) {
            sb.append(String.format("%d %s\n", excNode.getLine(), excNode.getExccode()));
            System.out.printf("%d %s %s%n", excNode.getLine(), excNode.getExccode(), excNode.getInfo());
        }
        excOutAns = sb.toString();
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

    private void visit(ASTNode node) {
        if (node.getType().equals("Decl")) {
            decl2symbol(node);
        } else if (node.getType().equals("FuncDef")) {
            funcdef2symbol(node);
        } else if (node.getType().equals("MainFuncDef")) {
            ASTNode block = node.getAstChildNodes().get(4);
            if (block.getAstChildNodes().size() == 2) {
                excNodes.add(new ExcNode(block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine(),
                        "g", "int main func has no return stmt in line "
                        + block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine()));
            } else {
                ASTNode returnstmt = block.getAstChildNodes().get(block.getAstChildNodes().size() - 2).getAstChildNodes().get(0);
                if (!returnstmt.getAstChildNodes().get(0).getName().equals("return")) {
                    excNodes.add(new ExcNode(block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine(),
                            "g", "int main func has no return stmt in line "
                            + block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine()));
                }
            }
        } else if (node.getType().equals("Block")) {
            String name = "block";
            if (stackWhile.size() > 0) {
                name = "while";
                stackWhile.remove(stackWhile.size() - 1);
            }
            SymbolTable block = new SymbolTable(name, curLevel);
            if (stackFuncParams.size() > 0) {
                for (SymbolTable symbolTable : stackFuncParams.get(stackFuncParams.size() - 1)) {
                    ExcNode ans = block.addSymbol(symbolTable);
                    if (ans != null) excNodes.add(ans);
                }
                stackFuncParams.remove(stackFuncParams.size() - 1);
            }
            beginSymStack.add(block);
            curLevel++;
        } else if (node.getType().equals("LVal")) {
            String name = node.getAstChildNodes().get(0).getName();
            if (findVarInAllTable(name) == null) {
                excNodes.add(new ExcNode(node.getAstChildNodes().get(0).getLine(),
                        "c", "use undefined name in LVal " + name + " in line "
                        + node.getAstChildNodes().get(0).getLine()));
            }
        } else if (node.getType().equals("UnaryExp") && node.getAstChildNodes().get(0).getType().equals("Ident")) {
            if (node.getAstChildNodes().size() > 1 && node.getAstChildNodes().get(1).getType().equals("(")) {
                String name = node.getAstChildNodes().get(0).getName();
                SymbolTable matchSymbol = findFuncInAllTable(name);
                int line = node.getAstChildNodes().get(0).getLine();
                if (matchSymbol == null) {
                    excNodes.add(new ExcNode(line,
                            "c", "use undefined name in UnaryExp " + name + " in line "
                            + line));
                } else if (matchSymbol.getKind().equals("func")) {
                    int rparamnum = 0;
                    ArrayList<ASTNode> expLs = new ArrayList<>();
                    for (ASTNode astNode : node.getAstChildNodes().get(2).getAstChildNodes()) {
                        if (astNode.getType().equals("Exp")) {
                            expLs.add(astNode);
                            rparamnum++;
                        }
                    }
                    if (matchSymbol.getParams().size() != rparamnum) {
                        excNodes.add(new ExcNode(line,
                                "d", "use unmatched param num in func " + name + " in line "
                                + line + ",we expect " + matchSymbol.getParams().size() + ",but we got " + rparamnum));
                    } else {
                        ArrayList<SymbolTable> params = matchSymbol.getParams();
                        ArrayList<Integer> ret = judgeParamMatch(params, expLs, rparamnum);
                        if (!ret.isEmpty()) {
                            if (ret.get(0) >= 0)
                                excNodes.add(new ExcNode(line,
                                        "e", "use unmatched param in func " + name + " in line "
                                        + line + ",we got dimension " + ret.get(0) + ",but we expected dimension " + ret.get(1)));
                            else if (ret.get(0) == -1)
                                excNodes.add(new ExcNode(line,
                                        "e", "use unmatched param in func " + name + " in line "
                                        + line + ",we got const,but we expected not const in var " + ret.get(1)));
                            else
                                excNodes.add(new ExcNode(line,
                                        "e", "use unmatched param in func " + name + " in line "
                                        + line + ",we got void,but we expected int in var"));
                        }
                    }
                }
            } else {
                String name = node.getAstChildNodes().get(0).getName();
                SymbolTable matchSymbol = findVarInAllTable(name);
                int line = node.getAstChildNodes().get(0).getLine();
                if (matchSymbol == null) {
                    excNodes.add(new ExcNode(line,
                            "c", "use undefined name in UnaryExp " + name + " in line "
                            + line));
                }
            }
        } else if (node.getType().equals("Stmt")
                && node.getAstChildNodes().get(0).getType().equals("LVal") &&
                node.getAstChildNodes().get(1).getType().equals("=")) {
            ASTNode lval = node.getAstChildNodes().get(0);
            String name = lval.getAstChildNodes().get(0).getName();
            SymbolTable symbolTable = findVarInAllTable(name);
            if (symbolTable != null) {
                if (symbolTable.isConst()) {
                    excNodes.add(new ExcNode(lval.getLine(), "h", "can't change const value of "
                            + name + " in line " + lval.getLine()));
                }
            }
        } else if (node.getType().equals("Stmt") && node.getAstChildNodes().get(0).getType().equals("printf")) {
            String fmstring = node.getAstChildNodes().get(2).getName();
            int num = 0;
            for (int i = 0; i < fmstring.length(); i++) {
                if (fmstring.charAt(i) == '%') {
                    num++;
                }
            }
            int rnum = 0;
            for (ASTNode astNode : node.getAstChildNodes()) {
                if (astNode.getType().equals("Exp")) {
                    rnum++;
                }
            }
            if (rnum != num) {
                excNodes.add(new ExcNode(node.getAstChildNodes().get(0).getLine(), "l",
                        "printf format num don't match,we expected " + num + ",but we got "
                                + rnum + " in line " + node.getAstChildNodes().get(0).getLine()));
            }
        } else if (node.getType().equals("Stmt") && node.getAstChildNodes().get(0).getType().equals("while")) {
            stackWhile.add(1);
        } else if (node.getName().equals("continue") || node.getName().equals("break")) {
            boolean flag = false;
            for (int i = 0; i <= curLevel; i++) {
                if (beginSymStack.get(i).getName().equals("while")) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                excNodes.add(new ExcNode(node.getLine(), "m",
                        String.format("can only use %s in while block,error in line %d", node.getName(), node.getLine())));
            }
        }
        for (ASTNode astNode : node.getAstChildNodes()) {
            visit(astNode);
        }
        if (node.getType().equals("Block")) {
            beginSymStack.remove(curLevel);
            curLevel--;
        }
    }

    private ArrayList<Integer> judgeParamMatch(ArrayList<SymbolTable> params, ArrayList<ASTNode> expLs, int len) {
        ArrayList<Integer> ret = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            ASTNode exp = expLs.get(i);
            SymbolTable param = params.get(i);
            ASTNode addexp = exp.getAstChildNodes().get(0);
            if (addexp.getAstChildNodes().size() == 1) {
                ASTNode mulexp = addexp.getAstChildNodes().get(0);
                if (mulexp.getAstChildNodes().size() == 1) {
                    ASTNode rangeExp = mulexp.getAstChildNodes().get(0);
                    while (rangeExp.getAstChildNodes().get(0).getType().equals("UnaryOp")) {
                        rangeExp = rangeExp.getAstChildNodes().get(1);
                    }
                    ASTNode unaryexp = rangeExp;
                    if (unaryexp.getAstChildNodes().get(0).getType().equals("PrimaryExp")) {
                        ASTNode primaryexp = unaryexp.getAstChildNodes().get(0);
                        if (primaryexp.getAstChildNodes().get(0).getType().equals("LVal")) {
                            ASTNode lval = primaryexp.getAstChildNodes().get(0);
                            SymbolTable var = findVarInAllTable(lval.getAstChildNodes().get(0).getName());
                            int bracknum = 0;
                            for (ASTNode astNode : lval.getAstChildNodes()) {
                                if (astNode.getType().equals("[")) bracknum++;
                            }
                            if (var != null) {
                                if (var.isConst()) {
                                    ret.add(-1);
                                    ret.add(var.getLine());
                                    return ret;
                                }
                                int paramd = var.getBracknum() - bracknum;
                                if (paramd != param.getBracknum()) {
                                    ret.add(paramd);
                                    ret.add(param.getBracknum());
                                    return ret;
                                }
                            }
                        }
                    } else if (unaryexp.getAstChildNodes().get(0).getType().equals("Ident")) {
                        ASTNode ident = unaryexp.getAstChildNodes().get(0);
                        String name = ident.getName();
                        SymbolTable func = findFuncInAllTable(name);
                        if (func != null && func.getType().equals("void")) {
                            ret.add(-2);
                            ret.add(-2);
                            return ret;
                        }
                    }
                }
            }
        }
        return ret;
    }

    private void addSymbol(SymbolTable symbolTable) {
        ExcNode ret = this.beginSymStack.get(curLevel).addSymbol(symbolTable);
        if (ret != null) this.excNodes.add(ret);
    }

    private void decl2symbol(ASTNode decl) {
        int line = 0;
        String type = "int"; //int void
        boolean isConst = false;
        int n1 = 0; //int[n1]
        int n2 = 0; //int[n1][n2]
        String kind = "var";
        String name = "";
        if (decl.getAstChildNodes().get(0).getType().equals("ConstDecl")) {
            isConst = true;
            ASTNode constdecl = decl.getAstChildNodes().get(0);
            for (int i = 0; i < constdecl.getAstChildNodes().size(); i++) {
                ASTNode astNode = constdecl.getAstChildNodes().get(i);
                if (astNode.getType().equals("ConstDef")) {
                    int bracknum = 0;
                    for (ASTNode astNode1 : astNode.getAstChildNodes()) {
                        if (astNode1.getType().equals("Ident")) {
                            line = astNode1.getLine();
                            name = astNode1.getName();
                        } else if (astNode1.getType().equals("[")) {
                            bracknum++;
                        }
                    }
                    SymbolTable symbol = new SymbolTable(line, type, isConst, n1, n2, curLevel, name, bracknum, kind);
                    addSymbol(symbol);
                }
            }
        } else {
            ASTNode constdecl = decl.getAstChildNodes().get(0);
            for (ASTNode astNode : constdecl.getAstChildNodes()) {
                if (astNode.getType().equals("VarDef")) {
                    int bracknum = 0;
                    for (ASTNode astNode1 : astNode.getAstChildNodes()) {
                        if (astNode1.getType().equals("Ident")) {
                            line = astNode1.getLine();
                            name = astNode1.getName();
                        } else if (astNode1.getType().equals("[")) {
                            bracknum++;
                        }
                    }
                    addSymbol(new SymbolTable(line, type, isConst, n1, n2, curLevel, name, bracknum, kind));
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
        stackFuncParams.add(params);
        ASTNode block = funcdef.getAstChildNodes().get(funcdef.getAstChildNodes().size() - 1);
        if (block.getAstChildNodes().size() == 2) {
            if (type.equals("int")) {
                excNodes.add(new ExcNode(block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine(),
                        "g", "int func has no return stmt in line "
                        + block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine()));
            }
            return;
        }
        ASTNode returnstmt = block.getAstChildNodes().get(block.getAstChildNodes().size() - 2).getAstChildNodes().get(0);
        if (type.equals("void")) {
            for (ASTNode astNode1 : block.getAstChildNodes()) {
                if (astNode1.getAstChildNodes().size() == 0) continue;
                ASTNode astNode = astNode1.getAstChildNodes().get(0);
                if (astNode.getAstChildNodes().get(0).getName().equals("return")) {
                    if (!astNode.getAstChildNodes().get(1).getName().equals(";")) {
                        excNodes.add(new ExcNode(astNode.getAstChildNodes().get(0).getLine(),
                                "f", "void func has unmatched return value in line "
                                + astNode.getAstChildNodes().get(0).getLine()));
                    }
                }
            }
        } else {
            if (!returnstmt.getAstChildNodes().get(0).getName().equals("return")) {
                excNodes.add(new ExcNode(block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine(),
                        "g", "int func has no return stmt in line "
                        + block.getAstChildNodes().get(block.getAstChildNodes().size() - 1).getLine()));
            }
        }
    }
}
