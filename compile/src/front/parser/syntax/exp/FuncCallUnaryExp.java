package front.parser.syntax.exp;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

/**
 * 为了方便UnaryExp的解析及翻译，对于原文法定义：
 *  UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
 * 我们把复杂的函数调用分支重新改写文法，变成如下效果
 *  UnaryExp → PrimaryUnaryExp | FuncCallUnaryExp | UnaryOpUnaryExp
 *  FuncCallUnaryExp → Ident '(' [FuncRParams] ')'
 */
public class FuncCallUnaryExp extends UnaryExp {
    private TokenNode indentifier;
    private FuncRParams funcRParams;

    // TODO
    public FuncCallUnaryExp() {
        super();
        this.indentifier = null;
        this.funcRParams = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.IDENFR) {
            this.indentifier = (TokenNode) child;
        } else if (child.getNodeType() == SynType.FuncRParams) {
            this.funcRParams = (FuncRParams) child;
        }
        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public TokenNode getIdentifier() {
        return indentifier;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public String getFunctionName() {
        return indentifier != null ? indentifier.getContent() : null;
    }

    public int getLineNumber() {
        return indentifier != null ? indentifier.getLineNumber() : -1;
    }

    public boolean hasParams() {
        return funcRParams != null;
    }
}
