package frontend;

import java.util.ArrayList;

public class Parser {
    private ArrayList<Word> words;
    private int pos = 0;
    private int sz;
    private String parserAns = "";
    private ASTNode ast;
    private ArrayList<String> lines = new ArrayList<>();
    private boolean debug = false;
    private ArrayList<ExcNode> excNodes = new ArrayList<>();

    public Parser(ArrayList<Word> words) {
        this.words = words;
        this.sz = words.size();
        this.ast = getCompUnit();
    }

    ASTNode getCompUnit() {
        ASTNode ast = addNoEnd("CompUnit");
        if (words.get(pos).getTypeCode().equals("INTTK")) {
            if (words.get(pos + 1).getTypeCode().equals("IDENFR") && words.get(pos + 2).getTypeCode().equals("LPARENT")) {
                ast.addChildNodes(getFuncs());
            } else {
                ast.addChildNodes(getDecls());
                ast.addChildNodes(getFuncs());
            }
        } else if (words.get(pos).getTypeCode().equals("VOIDTK")) {
            ast.addChildNodes(getFuncs());
        } else {
            ast.addChildNodes(getDecls());
            ast.addChildNodes(getFuncs());
        }
        ast.addChildNode(getMainFuncDef());
        addOut("CompUnit");
        setParserAns();
        return ast;
    }

    ArrayList<ASTNode> getFuncs() {
        ArrayList<ASTNode> funcs = new ArrayList<>();
        while (pos + 2 < sz && !(words.get(pos).getTypeCode().equals("INTTK") && words.get(pos + 1).getTypeCode().equals("MAINTK") && words.get(pos + 2).getTypeCode().equals("LPARENT"))) {
            funcs.add(getFuncDef());
        }
        return funcs;
    }

    ArrayList<ASTNode> getDecls() {
        ArrayList<ASTNode> decls = new ArrayList<>();
        while (pos + 2 < sz && !((words.get(pos + 1).getTypeCode().equals("MAINTK")
                || words.get(pos + 1).getTypeCode().equals("IDENFR"))
                && words.get(pos + 2).getTypeCode().equals("LPARENT"))) {
            decls.add(getDecl());
        }
        return decls;
    }

    ASTNode getDecl() {
        ASTNode decl = addNoEnd("Decl");
        if (words.get(pos).getTypeCode().equals("INTTK")) {
            decl.addChildNode(getVarDecl());
        } else {
            decl.addChildNode(getConstDecl());
        }
        return decl;
    }

    ASTNode getConstDecl() {
        ASTNode constdecl = addNoEnd("ConstDecl");
        constdecl.addChildNode(addEnd());
        nextSym();
        ASTNode btype = addNoEnd("BType");
        btype.addChildNode(addEnd());
        nextSym();
        constdecl.addChildNode(btype);
        constdecl.addChildNode(getConstDef());
        while (pos < sz && words.get(pos).getTypeCode().equals("COMMA")) {
            constdecl.addChildNodes(addEnds(1));
            constdecl.addChildNode(getConstDef());
        }
        if (!cursymequal("SEMICN")) constdecl.addChildNode(addExcEnd("SEMICN ConstDecl"));
        else constdecl.addChildNodes(addEnds(1));
        addOut("ConstDecl");
        return constdecl;
    }


    ASTNode getVarDecl() {
        ASTNode VarDecl = addNoEnd("VarDecl");
        ASTNode btype = addNoEnd("BType");
        btype.addChildNode(addEnd());
        nextSym();
        VarDecl.addChildNode(btype);
        VarDecl.addChildNode(getVarDef());
        while (pos < sz && words.get(pos).getTypeCode().equals("COMMA")) {
            VarDecl.addChildNodes(addEnds(1));
            VarDecl.addChildNode(getVarDef());
        }
        if (!cursymequal("SEMICN")) VarDecl.addChildNode(addExcEnd("SEMICN VarDecl"));
        else VarDecl.addChildNodes(addEnds(1));
        addOut("VarDecl");
        return VarDecl;
    }

    ASTNode getConstDef() {
        ASTNode constdef = addNoEnd("ConstDef");
        constdef.addChildNode(addNoEnd("Ident"));
        nextSym();
        while (pos < sz && !cursymequal("ASSIGN")) {
            constdef.addChildNodes(addEnds(1));
            constdef.addChildNode(getConstExp());
            if (!cursymequal("RBRACK")) constdef.addChildNode(addExcEnd("RBRACK ConstDef"));
            else constdef.addChildNodes(addEnds(1));
        }
        constdef.addChildNode(addEnd());
        nextSym();
        constdef.addChildNode(getConstInitVal());
        addOut("ConstDef");
        return constdef;
    }

    ASTNode getVarDef() {
        ASTNode VarDef = addNoEnd("VarDef");
        VarDef.addChildNode(addNoEnd("Ident"));
        nextSym();
        while (pos < sz && !cursymequal("ASSIGN") && !cursymequal("SEMICN") && !cursymequal("COMMA")) {
            VarDef.addChildNodes(addEnds(1));
            VarDef.addChildNode(getConstExp());
            if (!cursymequal("RBRACK")) VarDef.addChildNode(addExcEnd("RBRACK VarDef"));
            else VarDef.addChildNodes(addEnds(1));
        }
        if (!cursymequal("ASSIGN")) {
            addOut("VarDef");
            return VarDef;
        }
        VarDef.addChildNode(addEnd());
        nextSym();
        VarDef.addChildNode(getInitVal());
        //nextSym();
        addOut("VarDef");
        return VarDef;
    }

    ASTNode getInitVal() {
        ASTNode InitVal = addNoEnd("InitVal");
        if (cursymequal("LBRACE")) {
            InitVal.addChildNodes(addEnds(1));
            if (!cursymequal("RBRACE")) {
                InitVal.addChildNode(getInitVal());
                while (cursymequal("COMMA")) {
                    InitVal.addChildNodes(addEnds(1));
                    InitVal.addChildNode(getInitVal());
                }
            }
            InitVal.addChildNodes(addEnds(1));
        } else {
            InitVal.addChildNode(getExp());
        }
        addOut("InitVal");
        return InitVal;
    }


    ASTNode getConstExp() {
        ASTNode constexp = addNoEnd("ConstExp");
        constexp.addChildNode(getAddExp());
        addOut("ConstExp");
        return constexp;
    }

    ASTNode getAddExp() {
        ASTNode addexp = addNoEnd("AddExp");
        addexp.addChildNode(getMulExp());
        while (pos < sz && words.get(pos).getTypeCode().equals("PLUS") || words.get(pos).getTypeCode().equals("MINU")) {
            addOut("AddExp");
            addexp.addChildNode(addEnd());
            nextSym();
            addexp.addChildNode(getMulExp());
        }
        addOut("AddExp");
        return addexp;
    }

    ASTNode getMulExp() {
        ASTNode mulexp = addNoEnd("MulExp");
        mulexp.addChildNode(getUnaryExp());
        while (pos < sz && words.get(pos).getTypeCode().equals("MULT") || words.get(pos).getTypeCode().equals("DIV") || words.get(pos).getTypeCode().equals("MOD")) {
            addOut("MulExp");
            mulexp.addChildNode(addEnd());
            nextSym();
            mulexp.addChildNode(getUnaryExp());
        }
        addOut("MulExp");
        return mulexp;
    }

    ASTNode getUnaryExp() {
        ASTNode unaryexp = addNoEnd("UnaryExp");
        if (cursymequal("IDENFR") && words.get(pos + 1).getTypeCode().equals("LPARENT")) {
            unaryexp.addChildNode(addNoEnd("Ident"));
            nextSym();
            unaryexp.addChildNode(addEnd());
            nextSym();
            if (!cursymequal("RPARENT")) unaryexp.addChildNode(getFuncRParams());
            if (!cursymequal("RPARENT")) unaryexp.addChildNode(addExcEnd("RPARENT UnaryExp"));
            else unaryexp.addChildNodes(addEnds(1));
        } else if (cursymequal("PLUS") || cursymequal("MINU") || cursymequal("NOT")) {
            ASTNode UnaryOp = addNoEnd("UnaryOp");
            UnaryOp.addChildNodes(addEnds(1));
            addOut("UnaryOp");
            unaryexp.addChildNode(UnaryOp);
            unaryexp.addChildNode(getUnaryExp());
        } else {
            unaryexp.addChildNode(getPrimaryExp());
        }
        addOut("UnaryExp");
        return unaryexp;
    }

    ASTNode getPrimaryExp() {
        ASTNode primaryexp = addNoEnd("PrimaryExp");
        if (cursymequal("LPARENT")) {
            primaryexp.addChildNode(addEnd());
            nextSym();
            primaryexp.addChildNode(getExp());
            primaryexp.addChildNode(addEnd());
            nextSym();
        } else if (cursymequal("IDENFR")) {
            primaryexp.addChildNode(getLVal());
        } else {
            primaryexp.addChildNode(getNumber());
        }
        addOut("PrimaryExp");
        return primaryexp;
    }

    ASTNode getExp() {
        ASTNode exp = addNoEnd("Exp");
        exp.addChildNode(getAddExp());
        addOut("Exp");
        return exp;
    }

    ASTNode getLVal() {
        ASTNode lval = addNoEnd("LVal");
        lval.addChildNode(addNoEnd("Ident"));
        nextSym();
        while (pos < sz && cursymequal("LBRACK")) {
            lval.addChildNode(addEnd());
            nextSym();
            lval.addChildNode(getExp());
            if (!cursymequal("RBRACK")) lval.addChildNode(addExcEnd("RBRACK LVal"));
            else lval.addChildNodes(addEnds(1));
        }
        addOut("LVal");
        return lval;
    }

    ASTNode getNumber() {
        ASTNode Number = addNoEnd("Number");
        ASTNode IntConst = addNoEnd("IntConst");
        IntConst.addChildNode(addEnd());
        Number.addChildNode(IntConst);
        nextSym();
        addOut("Number");
        return Number;
    }

    ASTNode getFuncRParams() {
        ASTNode FuncRParams = addNoEnd("FuncRParams");
        FuncRParams.addChildNode(getExp());
        while (pos < sz && cursymequal("COMMA")) {
            FuncRParams.addChildNode(addEnd());
            nextSym();
            FuncRParams.addChildNode(getExp());
        }
        addOut("FuncRParams");
        return FuncRParams;
    }


    ASTNode getConstInitVal() {
        ASTNode ConstInitVal = addNoEnd("ConstInitVal");
        if (pos < sz && cursymequal("LBRACE")) {
            ConstInitVal.addChildNode(addEnd());
            nextSym();
            if (!cursymequal("RBRACE")) {
                ConstInitVal.addChildNode(getConstInitVal());
                while (cursymequal("COMMA")) {
                    ConstInitVal.addChildNodes(addEnds(1));
                    ConstInitVal.addChildNode(getConstInitVal());
                }
            }
            ConstInitVal.addChildNode(addEnd());
            nextSym();
        } else {
            ConstInitVal.addChildNode(getConstExp());
        }
        addOut("ConstInitVal");
        return ConstInitVal;
    }

    ASTNode getFuncDef() {
        ASTNode FuncDef = addNoEnd("FuncDef");
        ASTNode FuncType = addNoEnd("FuncType");
        FuncType.addChildNode(addEnd());
        nextSym();
        addOut("FuncType");
        FuncDef.addChildNode(FuncType);
        FuncDef.addChildNode(addNoEnd("Ident"));
        nextSym();
        FuncDef.addChildNode(addEnd());
        nextSym();
        if (!cursymequal("RPARENT")) FuncDef.addChildNode(getFuncFParams());
        if (!cursymequal("RPARENT")) FuncDef.addChildNode(addExcEnd("RPARENT FuncDef"));
        else FuncDef.addChildNodes(addEnds(1));
        FuncDef.addChildNode(getBlock());
        addOut("FuncDef");
        return FuncDef;
    }

    ASTNode getFuncFParams() {
        ASTNode FuncFParams = addNoEnd("FuncFParams");
        FuncFParams.addChildNode(getFuncFParam());
        while (pos < sz && cursymequal("COMMA")) {
            FuncFParams.addChildNode(addEnd());
            nextSym();
            FuncFParams.addChildNode(getFuncFParam());
        }
        addOut("FuncFParams");
        return FuncFParams;
    }

    ASTNode getFuncFParam() {
        ASTNode FuncFParam = addNoEnd("FuncFParam");
        ASTNode BType = addNoEnd("BType");
        BType.addChildNode(addEnd());
        FuncFParam.addChildNode(BType);
        nextSym();
        FuncFParam.addChildNode(addNoEnd("Ident"));
        nextSym();
        if (cursymequal("LBRACK")) {
            FuncFParam.addChildNode(addEnd());
            nextSym();
            if (!cursymequal("RBRACK")) FuncFParam.addChildNode(addExcEnd("RBRACK FuncFParam"));
            else FuncFParam.addChildNodes(addEnds(1));
            if (cursymequal("LBRACK")) {
                FuncFParam.addChildNode(addEnd());
                nextSym();
                FuncFParam.addChildNode(getConstExp());
                if (!cursymequal("RBRACK")) FuncFParam.addChildNode(addExcEnd("RBRACK FuncFParam"));
                else FuncFParam.addChildNodes(addEnds(1));
            }
        }
        addOut("FuncFParam");
        return FuncFParam;
    }

    ASTNode getBlock() {
        ASTNode Block = addNoEnd("Block");
        Block.addChildNode(addEnd());
        nextSym();
        while (pos < sz && !cursymequal("RBRACE")) {
            Block.addChildNode(getBlockItem());
        }
        Block.addChildNode(addEnd());
        nextSym();
        addOut("Block");
        return Block;
    }

    ASTNode getBlockItem() {
        ASTNode BlockItem = addNoEnd("BlockItem");
        if (cursymequal("INTTK") || cursymequal("CONSTTK")) {
            BlockItem.addChildNode(getDecl());
        } else {
            BlockItem.addChildNode(getStmt());
        }
        //addOut("BlockItem");
        return BlockItem;
    }

    ASTNode getStmt() {
        ASTNode Stmt = addNoEnd("Stmt");
        if (cursymequal("LBRACE")) {
            Stmt.addChildNode(getBlock());
        } else if (cursymequal("IFTK")) {
            Stmt.addChildNodes(addEnds(2));
            Stmt.addChildNode(getCond());
            if (!cursymequal("RPARENT")) Stmt.addChildNode(addExcEnd("RPARENT Stmt"));
            else Stmt.addChildNodes(addEnds(1));
            Stmt.addChildNode(getStmt());
            if (cursymequal("ELSETK")) {
                Stmt.addChildNode(addEnd());
                nextSym();
                Stmt.addChildNode(getStmt());
            }
        } else if (cursymequal("WHILETK")) {
            Stmt.addChildNodes(addEnds(2));
            Stmt.addChildNode(getCond());
            if (!cursymequal("RPARENT")) Stmt.addChildNode(addExcEnd("RPARENT Stmt"));
            else Stmt.addChildNodes(addEnds(1));
            Stmt.addChildNode(getStmt());
        } else if (cursymequal("BREAKTK") || cursymequal("CONTINUETK")) {
            Stmt.addChildNodes(addEnds(1)); //break and continue
            if (!cursymequal("SEMICN")) Stmt.addChildNode(addExcEnd("SEMICN Stmt"));
            else Stmt.addChildNodes(addEnds(1)); //;
        } else if (cursymequal("RETURNTK")) {
            Stmt.addChildNodes(addEnds(1));
            if (!cursymequal("SEMICN")) {
                Stmt.addChildNode(getExp());
            }
            if (!cursymequal("SEMICN")) Stmt.addChildNode(addExcEnd("SEMICN Stmt"));
            else Stmt.addChildNodes(addEnds(1)); //;
        } else if (cursymequal("PRINTFTK")) {
            Stmt.addChildNodes(addEnds(2));
            Stmt.addChildNode(getFormatString());
            nextSym();
            while (cursymequal("COMMA")) {
                Stmt.addChildNodes(addEnds(1));
                Stmt.addChildNode(getExp());
            }
            if (!cursymequal("RPARENT")) Stmt.addChildNode(addExcEnd("RPARENT Stmt"));
            else Stmt.addChildNodes(addEnds(1)); //)
            if (!cursymequal("SEMICN")) Stmt.addChildNode(addExcEnd("SEMICN Stmt"));
            else Stmt.addChildNodes(addEnds(1)); //;
        } else if (cursymequal("SEMICN")) {
            Stmt.addChildNode(addEnd());
            nextSym();
        } else {
            int prepos = pos;
            int preoutsz = lines.size();
            ASTNode lval = getLVal();
            if (cursymequal("ASSIGN")) {
                Stmt.addChildNode(lval);
                Stmt.addChildNodes(addEnds(1));
                if (cursymequal("GETINTTK")) {
                    Stmt.addChildNodes(addEnds(2));
                    if (!cursymequal("RPARENT")) Stmt.addChildNode(addExcEnd("RPARENT Stmt"));
                    else Stmt.addChildNodes(addEnds(1));
                    if (!cursymequal("SEMICN")) Stmt.addChildNode(addExcEnd("SEMICN Stmt"));
                    else Stmt.addChildNodes(addEnds(1)); //;
                } else {
                    Stmt.addChildNode(getExp());
                    if (!cursymequal("SEMICN")) Stmt.addChildNode(addExcEnd("SEMICN Stmt"));
                    else Stmt.addChildNodes(addEnds(1)); //;
                }
            } else {
                int outsz = lines.size();
                if (outsz > preoutsz) {
                    lines.subList(preoutsz, outsz).clear();
                }
                pos = prepos;
                Stmt.addChildNode(getExp());
                if (!cursymequal("SEMICN")) Stmt.addChildNode(addExcEnd("SEMICN Stmt"));
                else Stmt.addChildNodes(addEnds(1)); //;
            }
        }
        addOut("Stmt");
        return Stmt;
    }

    ASTNode getCond() {
        ASTNode Cond = addNoEnd("Cond");
        Cond.addChildNode(getLOrExp());
        addOut("Cond");
        return Cond;
    }

    ASTNode getLOrExp() {
        ASTNode LOrExp = addNoEnd("LOrExp");
        LOrExp.addChildNode(getLAndExp());
        while (pos < sz && cursymequal("OR")) {
            addOut("LOrExp");
            LOrExp.addChildNodes(addEnds(1));
            LOrExp.addChildNode(getLAndExp());
        }
        addOut("LOrExp");
        return LOrExp;
    }

    ASTNode getLAndExp() {
        ASTNode LAndExp = addNoEnd("LAndExp");
        LAndExp.addChildNode(getEqExp());
        while (pos < sz && cursymequal("AND")) {
            addOut("LAndExp");
            LAndExp.addChildNodes(addEnds(1));
            LAndExp.addChildNode(getEqExp());
        }
        addOut("LAndExp");
        return LAndExp;
    }

    ASTNode getEqExp() {
        ASTNode EqExp = addNoEnd("EqExp");
        EqExp.addChildNode(getRelExp());
        while (pos < sz && (cursymequal("EQL") || cursymequal("NEQ"))) {
            addOut("EqExp");
            EqExp.addChildNodes(addEnds(1));
            EqExp.addChildNode(getRelExp());
        }
        addOut("EqExp");
        return EqExp;
    }

    ASTNode getRelExp() {
        ASTNode RelExp = addNoEnd("RelExp");
        RelExp.addChildNode(getAddExp());
        while (pos < sz && (cursymequal("LSS") || cursymequal("LEQ") || cursymequal("GRE") || cursymequal("GEQ"))) {
            addOut("RelExp");
            RelExp.addChildNodes(addEnds(1));
            RelExp.addChildNode(getAddExp());
        }
        addOut("RelExp");
        return RelExp;
    }

    ASTNode getFormatString() {
        return addNoEnd("FormatString");
    }

    ASTNode getMainFuncDef() {
        ASTNode MainFuncDef = addNoEnd("MainFuncDef");
        MainFuncDef.addChildNodes(addEnds(4));
        MainFuncDef.addChildNode(getBlock());
        addOut("MainFuncDef");
        return MainFuncDef;
    }

    private void setParserAns() {
        StringBuilder stringBuilder = new StringBuilder(parserAns);
        for (String line : lines) {
            stringBuilder.append(line);
        }
        parserAns = stringBuilder.toString();
    }

    public String getParserAns() {
        return parserAns;
    }

    public void nextSym() {
        if (pos >= sz) return;
        String lineStr = words.get(pos++).getOutPut();
        lines.add(lineStr);
        //parserAns += lineStr;
        if (debug) System.out.print(lineStr);
    }

    public void addOut(String cont) {
        String lineStr = "<" + cont + ">" + "\n";
        lines.add(lineStr);
        if (debug) System.out.print(lineStr);
    }

    public boolean cursymequal(String type) {
        return words.get(pos).getTypeCode().equals(type);
    }

    public ASTNode addEnd() {
        return new ASTNode(words.get(pos).getContent(), words.get(pos).getContent(), true, words.get(pos));
    }

    public ASTNode addNoEnd(String type) {
        if (type.equals("Ident")) {
            return new ASTNode("Ident", words.get(pos).getContent(), true, words.get(pos));
        } else if (type.equals("FormatString")) {
            ASTNode string = new ASTNode("FormatString", words.get(pos).getContent(), true, words.get(pos));
            if (string.isexc()) {
                excNodes.add(string.getExcNode());
            }
            return string;
        }
        return new ASTNode(type, "", false, words.get(pos));
    }

    public ASTNode addExcEnd(String type) {
        ASTNode excnode = new ASTNode("EXC", type, true, words.get(pos - 1));
        excNodes.add(excnode.getExcNode());
        return excnode;
    }

    public ArrayList<ASTNode> addEnds(int x) {
        ArrayList<ASTNode> ans = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            ans.add(addEnd());
            nextSym();
        }
        return ans;
    }

    public void debugOutput() {
        if (debug) {
            System.out.print("cur word:" + words.get(pos).getOutPut());
        }
    }

    public ASTNode getAst() {
        return ast;
    }

    public ArrayList<ExcNode> getExcNodes() {
        return excNodes;
    }
}
