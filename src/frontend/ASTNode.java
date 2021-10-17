package frontend;

import java.util.ArrayList;
import java.util.Objects;

public class ASTNode {
    private String type;
    private String name;
    private boolean isEnd;
    private ArrayList<ASTNode> astChildNodes;
    private ASTNode parentNode;
    private Word word;
    private String excCode = "r";
    private String excInfo;
    private String parseAns;
    private StringBuilder parseAnsBuilder = new StringBuilder();

    public ASTNode(String type, String name, boolean isEnd, Word word) {
        this.type = type;
        this.name = name;
        this.isEnd = isEnd;
        this.word = word;
        this.astChildNodes = new ArrayList<>();
        judgeSelfExc();
    }

    public void addChildNode(ASTNode node) {
        this.astChildNodes.add(node);
    }

    public void addChildNodes(ArrayList<ASTNode> nodes) {
        this.astChildNodes.addAll(nodes);
    }

    public void setParentNode(ASTNode parentNode) {
        this.parentNode = parentNode;
    }

    public ASTNode getParentNode() {
        return parentNode;
    }

    private void judgeSelfExc() {
        if (type.equals("FormatString")) {
            int len = name.length();
            //System.out.println(name);
            for (int i = 0; i < len; i++) {
                if (i == 0 || i == len - 1) continue;
                if (name.charAt(i) == '%') {
                    if (i + 1 >= len || name.charAt(i + 1) != 'd') {
                        this.excCode = "a";
                        this.excInfo = "FormatString Wrong:Invalid char:%";
                        return;
                    }
                } else if (name.charAt(i) == '\\') {
                    if (i + 1 >= len || name.charAt(i + 1) != 'n') {
                        this.excCode = "a";
                        this.excInfo = "FormatString Wrong:Invalid char:\\";
                        return;
                    }
                } else if (!(name.charAt(i) >= 32 && name.charAt(i) <= 33) && !(name.charAt(i) >= 40 && name.charAt(i) <= 126)) {
                    this.excCode = "a";
                    this.excInfo = "FormatString Wrong:Invalid char:" + name.charAt(i);
                    return;
                }
            }
        } else if (type.equals("EXC")) {
            String[] strings = name.split(" ");
            switch (strings[0]) {
                case "SEMICN":
                    this.type = ";";
                    this.name = ";";
                    this.excCode = "i";
                    this.excInfo = "Lack ; in " + strings[1];
                    break;
                case "RPARENT":
                    this.type = ")";
                    this.name = ")";
                    this.excCode = "j";
                    this.excInfo = "Lack ) in " + strings[1];
                    break;
                case "RBRACK":
                    this.type = "]";
                    this.name = "]";
                    this.excCode = "k";
                    this.excInfo = "Lack ] in " + strings[1];
                    break;
            }
        }
    }

    @Override
    public String toString() {
        if (type.equals("BlockItem") || type.equals("Decl") || type.equals("BType")) {
            return "";
        }
        if (isEnd) {
            return type + " " + name;
        } else {
            return type;
        }
    }

    public int getLine() {
        return word.getLine();
    }

    public boolean getIsEnd() {
        return isEnd;
    }

    public String getType() {
        return type;
    }

    public Word getWord() {
        return word;
    }

    public boolean isexc() {
        return !Objects.equals(excCode, "r");
    }

    public ExcNode getExcNode() {
        return new ExcNode(getLine(), excCode, excInfo);
    }

    public ArrayList<ASTNode> getAstChildNodes() {
        return astChildNodes;
    }

    public String getName() {
        return name;
    }

    public void postOrder(ASTNode node) {
        if (node == null) {
            return;
        }
        String type = node.getType();
        for (int i = 0; i < node.getAstChildNodes().size(); i++) {
            postOrder(node.getAstChildNodes().get(i));
            if ((type.equals("MulExp") || type.equals("AddExp") ||
                    type.equals("RelExp") || type.equals("EqExp") ||
                    type.equals("LAndExp") || type.equals("LOrExp")) && node.getAstChildNodes().size() > 1 && i != node.getAstChildNodes().size() - 1) {
                parseAnsBuilder.append(String.format("<%s>\n", node.getType()));
            }
        }
        if (!node.toString().equals("")) {
            parseAnsBuilder.append(node).append("\n");
        }
    }

    public String getParseAns() {
        return parseAnsBuilder.toString();
    }
}
