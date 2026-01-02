package front.parser.syntax.exp;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;

/**
 * 为了方便UnaryExp的解析及翻译，对于原文法定义：
 *  UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
 * 我们把复杂的PrimaryExp分支分出来，加上了一个PrimaryUnaryExp
 *  UnaryExp → PrimaryUnaryExp | FuncCallUnaryExp | UnaryOpUnaryExp
 */
public class PrimaryUnaryExp extends UnaryExp {
    private PrimaryExp expr;

    public PrimaryUnaryExp() {
        super();
        this.expr = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() != SynType.PrimaryExp) {
            System.out.println("PrimaryUnaryExp with a wrong child");
        } else {
            expr = (PrimaryExp) child;
        }
        super.appendChild(child);
    }
    
    /**
     * 获取PrimaryExp节点
     * @return PrimaryExp节点，如果不存在则返回null
     */
    public PrimaryExp getPrimaryExp() {
        return expr;
    }
}
