package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

/**
 * 为了方便UnaryExp的解析及翻译，对于原文法定义：
 *  UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
 * 我们实际使用时加了一层，变成了下面的这种
 *  UnaryExp → PrimaryUnaryExp | FuncCallUnaryExp | UnaryExp
 *  上述右侧的三种其实是三种不同情况下的三类UnaryExp
 */
public class UnaryExp extends BranchNode {
    private UnaryExp expr;
    private UnaryOp operator;
    public boolean isFuncCall = false;

    public UnaryExp() {
        super(SynType.UnaryExp);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.UnaryExp) {
            expr = (UnaryExp) child;
        } else if (child.getNodeType() == SynType.UnaryOp) {
            operator = (UnaryOp) child;
        } else if (child.getNodeType() == SynType.IDENFR) {
            isFuncCall = true;
        }
        super.appendChild(child);
    }

    /**
     * 获取UnaryExp表达式（用于递归结构）
     * @return UnaryExp节点，如果不存在则返回null
     */
    public UnaryExp getExpr() {
        return expr;
    }

    /**
     * 获取一元操作符
     * @return UnaryOp节点，如果不存在则返回null
     */
    public UnaryOp getOperator() {
        return operator;
    }

    /**
     * 判断是否为函数调用
     * @return 如果是函数调用返回true，否则返回false
     */
    public boolean isFuncCall() {
        return isFuncCall;
    }
}
