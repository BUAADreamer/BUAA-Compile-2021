# 编译器设计文档

## 阶段一

### 总体架构

![](pic/Compiler1.png)

本阶段编译器总体设计如上图所示。顶层为`Compiler`类，由`Frontend`类进行前端各个类之间的处理流程和数据传输。首先先利用`IOtool`类获得输入，之后将输入的代码送入`Lexer`
进行词法分析，由词法分析将代码转换成`token`形式的单词表，用`Word`类存储，将这个`Word`类的列表作为`Parser`语法分析的输入数据，`Parser`进行语法分析并处理`a/i/j/k`
四个简单错误，输出抽象语法树和一个错误节点的列表，作为`Visitor`的输入，`Visitor`遍历语法树建立符号表并处理其他的语义层面和符号相关错误，输出完整的错误列表和符号表。整个过程中由各个类完成输出字符串拼接，由`IOtool`
类输出字符串到文件。

`Frontend`类主函数代码如下

```Java
public Frontend()throws IOException{
        IOtool iOtool=new IOtool();//创建IO工具
        Lexer lexer=new Lexer(iOtool.getInput());//词法分析
        iOtool.outputAns(lexer.getLexerAns());
        Parser parser=new Parser(lexer.getWords());//语法分析
        iOtool.outputAns(parser.getParserAns());
        Visitor visitor=new Visitor(parser.getAst(),parser.getExcNodes());//符号表管理与错误处理
        iOtool.outputError(visitor.getExcOutAns());
        }
```

### 词法分析

使用`Lexer`类完成，主要实现了一个`getsym`方法，作为接口可以单独给语法分析动态提供输入，也可以在类内被构造函数调用输出词法分析的输出。同时在类内使用全局变量`pos`
对当前的字符位置做标记，便于更新和回溯。具体判断上，将字符分成三类，标识符类，数字类，其他字符类。标识符类判断是否为保留字或者自定义标识符，其他字符对每个字符做特定的处理转换成相应的输出。具体类方法和变量如下图所示。

`Lexer`主方法如下

```Java
public Lexer(String sourceCode){
        this.sourceCode=sourceCode;
        sz=sourceCode.length();
        while(true){
        String ret=getsym(); //调用getsym方法更新字符位置和获取当前的符号码
        if(ret.equals("INTCON")){
        words.add(new Word(ret,String.valueOf(curNum),lineNum));
        }else if(!ret.equals("annotation")&&!ret.equals("EOF")){
        //忽略注释和结尾
        words.add(new Word(ret,curStr,lineNum));
        }
        if(ret.equals("EOF"))break;
        }
        if(debug){
        System.out.println(lexerAns);
        }
        }
```

<img src="pic/Lexer_Word.png" style="zoom: 33%;" />

### 语法分析

建立`Parser`类完成这部分内容，使用递归下降的方式对词法分析得到的`Word`类列表进行语法分析，利用一个`pos`表示当前的符号指针。建立了一个`ASTNode`类保存了每个语法树节点的`type`，`name`,`Word`
，`isEnd`，`astChildNodes`属性信息。在分析过程中对每个节点都增加保存他们的子节点信息，从而完成语法树建立。建立过程中保存输出结果作为语法分析的输出。下图是类图。

对于原有文法中有左递归的部分进行了消除处理，比如`AddExp → MulExp | AddExp ('+' | '−') MulExp`改为`AddExp → MulExp {('+' | '−') MulExp} `
，类似的还有`MulExp/RelExp/EqExp/LAndExp/LOrExp`。为了保证输出符合要求，在每次遇到这样的节点时如果有多个子节点会在每个非终结符子节点遍历结束后增加输出当前类别码。

`Parser`主方法如下

```Java
public Parser(ArrayList<Word> words){
        this.words=words;
        this.sz=words.size();
        this.ast=getCompUnit(); //获取顶层模块，不断递归向下拆解分析
        }
```

<img src="pic/Parser_ASTNode.png" style="zoom: 33%;" />

### 错误处理

建立了`Visitor`类，`ExcNode`类，`SymbolTable`类三个类实现错误处理和符号表管理。

对`Parser`类进行了修改，实现了对缺少`; ) ]`以及`FormatString`中异常字符这四种错误进行了处理，具体来说就是读到特定字符时如果没有就生成一个异常节点放入树中，同时将错误信息用`ExcNode`
类保存，从而不影响正常的语法分析和语法树遍历。

`SymbolTable`类主要保存了符号的信息：是否为块也就是块类型，函数类型或是变量类型，函数类型会有`params`，每个`param`也是符号表类，保存相应的参数维数信息。而变量类型则会记录维数。

`Visitor`类对`Parser`生成的语法树进行**后序遍历**
，维护一个符号表栈，每次进入一个节点，先对这个节点可能出现的错误进行处理，包括符号重定义，未定义以及各种语义错误。对于每个标识符，都会先查符号表，从当前层逐层向上查找，将查到的符号和当前符号进行比对，根据情况决定是否加入符号表和错误处理。如果当前节点是`block`
节点，则将这个`block`生成的一个符号放入符号表栈中。之后进行子节点遍历，遍历完成结束再将这个`block`节点出栈。

`Visitor`主方法如下

```java
public Visitor(ASTNode ast,ArrayList<ExcNode> excNodes){
        this.ast=ast; //初始化语法树
        this.excNodes=excNodes; //初始化错误节点列表
        this.beginSymStack.add(new SymbolTable("global",0)); //初始化符号表，建立顶层符号表对象
        visit(ast); //后序遍历语法树，进行错误处理与符号表管理
        setExc(); //将错误节点列表转换为字符串输出
        }
```

以下是三个类的类图

<img src="pic/ExcNode_Visitor_SymbolTable.png" style="zoom: 33%;" />


参考网站:
https://gcc.godbolt.org/
