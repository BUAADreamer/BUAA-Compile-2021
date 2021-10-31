package frontend;

import java.util.ArrayList;

public class Lexer {
    private String sourceCode;
    private int pos = 0; //read pointer
    private int sz; //length of source code
    private String lexerAns = "";
    private String curStr = "";
    private int curNum = 0;
    private int lineNum = 1;
    private ArrayList<Word> words = new ArrayList<>();
    private boolean debug = false;

    public Lexer(String sourceCode) {
        this.sourceCode = sourceCode;
        sz = sourceCode.length();
        while (true) {
            String ret = getsym();
            if (ret.equals("INTCON")) {
                words.add(new Word(ret, String.valueOf(curNum), lineNum));
            } else if (!ret.equals("annotation") && !ret.equals("EOF")) {
                //忽略注释和结尾
                words.add(new Word(ret, curStr, lineNum));
            }
            if (ret.equals("EOF")) break;
        }
        if (debug) {
            System.out.println(lexerAns);
        }
    }

    //返回类别码同时增加词法分析调试输出
    public String getsym() {
        curStr = "";
        curNum = 0;
        while (pos < sz && isWhite()) {
            if (sourceCode.charAt(pos) == '\n') {
                lineNum++;
            }
            pos++;
        }
        if (pos >= sz) return "EOF";
        String symAns;
        if (isLetter()) {
            while (pos < sz && isLetter() || isDigit()) {
                curStr = curStr.concat(sourceCode.substring(pos, pos + 1));
                pos++;
            }
            symAns = judgeWord(curStr);
            lexerAns += symAns + " " + curStr + "\n";
        } else if (isDigit()) {
            while (pos < sz && isDigit()) {
                curNum = curNum * 10 + sourceCode.charAt(pos) - '0';
                pos++;
            }
            symAns = "INTCON";
            lexerAns += symAns + " " + curNum + "\n";
        } else {
            symAns = judgeSpecWord();
            if (!symAns.equals("annotation") && !symAns.equals("EOF")) {
                lexerAns += symAns + " " + curStr + "\n";
            }
        }
        return symAns;
    }

    //判断当前字符是否为空白字符
    public boolean isWhite() {
        return Character.isWhitespace(sourceCode.charAt(pos));
    }

    //判断当前字符是否为字母
    public boolean isLetter() {
        return Character.isLetter(sourceCode.charAt(pos)) || sourceCode.charAt(pos) == '_';
    }

    //判断当前字符是否为数字
    public boolean isDigit() {
        return Character.isDigit(sourceCode.charAt(pos));
    }

    //判断各个特殊字符
    public String judgeSpecWord() {
        char c = sourceCode.charAt(pos);
        int postmp = pos;
        String ans = "";
        if (c == '!') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '=') {
                postmp += 2;
                pos = postmp;
                curStr = "!=";
                return "NEQ";
            }
            postmp++;
            ans = "NOT";
        } else if (c == '|') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '|') {
                postmp += 2;
                ans = "OR";
            }
        } else if (c == '&') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '&') {
                postmp += 2;
                ans = "AND";
            }
        } else if (c == '+') {
            postmp++;
            ans = "PLUS";
        } else if (c == '-') {
            postmp++;
            ans = "MINU";
        } else if (c == '*') {
            postmp++;
            ans = "MULT";
        } else if (c == '/') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '/') {
                postmp += 2;
                while (postmp < sz && sourceCode.charAt(postmp) != '\n') postmp++;
                postmp++;
                lineNum++;
                pos = postmp;
                ans = "annotation";
                return ans;
            } else if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '*') {
                postmp += 2;
                while (postmp < sz && !(sourceCode.charAt(postmp) == '*' && sourceCode.charAt(postmp + 1) == '/')) {
                    if (sourceCode.charAt(postmp) == '\n') lineNum++;
                    postmp++;
                }
                postmp += 2;
                pos = postmp;
                ans = "annotation";
                return ans;
            }
            postmp++;
            ans = "DIV";
        } else if (c == '%') {
            postmp++;
            ans = "MOD";
        } else if (c == '<') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '=') {
                postmp += 2;
                pos = postmp;
                curStr = "<=";
                return "LEQ";
            }
            postmp++;
            ans = "LSS";
        } else if (c == '>') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '=') {
                postmp += 2;
                pos = postmp;
                curStr = ">=";
                return "GEQ";
            }
            postmp++;
            ans = "GRE";
        } else if (c == '=') {
            if (postmp + 1 < sz && sourceCode.charAt(postmp + 1) == '=') {
                postmp += 2;
                pos = postmp;
                curStr = "==";
                return "EQL";
            }
            postmp++;
            ans = "ASSIGN";
        } else if (c == ';') {
            postmp++;
            ans = "SEMICN";
        } else if (c == ',') {
            postmp++;
            ans = "COMMA";
        } else if (c == '(') {
            postmp++;
            ans = "LPARENT";
        } else if (c == ')') {
            postmp++;
            ans = "RPARENT";
        } else if (c == '[') {
            postmp++;
            ans = "LBRACK";
        } else if (c == ']') {
            postmp++;
            ans = "RBRACK";
        } else if (c == '{') {
            postmp++;
            ans = "LBRACE";
        } else if (c == '}') {
            postmp++;
            ans = "RBRACE";
        } else if (c == '"') {
            postmp++;
            while (postmp < sz && sourceCode.charAt(postmp) != '"') {
                postmp++;
            }
            postmp++;
            ans = "STRCON";
        } else {
            return "EOF";
        }
        curStr = sourceCode.substring(pos, postmp);
        pos = postmp;
        return ans;
    }

    //返回标识符种类
    public String judgeWord(String word) {
        switch (word) {
            case "main":
                return "MAINTK";
            case "const":
                return "CONSTTK";
            case "int":
                return "INTTK";
            case "break":
                return "BREAKTK";
            case "continue":
                return "CONTINUETK";
            case "if":
                return "IFTK";
            case "else":
                return "ELSETK";
            case "while":
                return "WHILETK";
            case "getint":
                return "GETINTTK";
            case "printf":
                return "PRINTFTK";
            case "return":
                return "RETURNTK";
            case "void":
                return "VOIDTK";
            default:
                return "IDENFR";
        }
    }

    public ArrayList<Word> getWords() {
        return words;
    }

    public String getLexerAns() {
        return lexerAns;
    }
}
