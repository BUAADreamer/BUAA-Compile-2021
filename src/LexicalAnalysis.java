import java.io.*;
import java.lang.Character;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LexicalAnalysis {
    private InputStream in = new FileInputStream("testfile.txt");
    private OutputStream out = new FileOutputStream("output.txt");
    private String sourceCode;
    private int pos = 0; //read pointer
    private int sz; //length of source code
    private String lexicalAnalysisAns = "";
    private String curStr = "";
    private int curNum = 0;
    private int lineNum = 1;
    private ArrayList<Word> words = new ArrayList<>();

    public LexicalAnalysis() throws IOException {
        getInput();
        while (true) {
            String ret = getsym();
            if (ret.equals("INTCON")) {
                words.add(new Word(ret, String.valueOf(curNum), lineNum));
                //System.out.println(ret + " " + curNum);
            } else {
                words.add(new Word(ret, curStr, lineNum));
                //System.out.println(ret + " " + curStr);
            }
            if (ret.equals("EOF")) break;
        }
        //System.out.println(lineNum);
        //System.out.println(words);
        outputLeAns();
    }

    //返回类别码
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
            lexicalAnalysisAns += symAns + " " + curStr + "\n";
        } else if (isDigit()) {
            while (pos < sz && isDigit()) {
                curNum = curNum * 10 + sourceCode.charAt(pos) - '0';
                pos++;
            }
            symAns = "INTCON";
            lexicalAnalysisAns += symAns + " " + curNum + "\n";
        } else {
            symAns = judgeSpecWord();
            if (!symAns.equals("annotation") && !symAns.equals("EOF")) {
                lexicalAnalysisAns += symAns + " " + curStr + "\n";
            }
        }
        return symAns;
    }

    public boolean isWhite() {
        return Character.isWhitespace(sourceCode.charAt(pos));
    }

    public boolean isLetter() {
        return Character.isLetter(sourceCode.charAt(pos)) || sourceCode.charAt(pos) == '_';
    }

    public boolean isDigit() {
        return Character.isDigit(sourceCode.charAt(pos));
    }

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
                while (postmp < sz && sourceCode.charAt(postmp) != '*') {
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
            //System.out.println("begin " + postmp);
            postmp++;
            while (postmp < sz && sourceCode.charAt(postmp) != '"') {
                postmp++;
            }
            //System.out.println("end " + postmp);
            postmp++;
            ans = "STRCON";
        } else {
            return "EOF";
        }
        curStr = sourceCode.substring(pos, postmp);
        pos = postmp;
        return ans;
    }

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

    public void getInput() throws IOException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        while (reader.ready()) {
            sb.append((char) reader.read());
        }
//        System.out.println(sb.toString());
        reader.close();
        in.close();
        sourceCode = sb.toString();
        sz = sourceCode.length();
    }

    public void outputLeAns() throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        lexicalAnalysisAns = lexicalAnalysisAns.trim();
        writer.append(lexicalAnalysisAns);
        writer.close();
    }

}


