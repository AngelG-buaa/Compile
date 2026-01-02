package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Exp;
import front.parser.syntax.exp.LVal;

/**
 * 对应文法：Stmt → LVal '=' Exp ';'（左值赋值表达式语句）
 * 说明：左值（变量或数组元素）被赋值为一个表达式的结果，以分号结束。
 * 示例：
 * x = y + 3 * z;  // LVal为x，Exp为y+3*z
 * arr[i] = (a > b) ? a : b;  // LVal为数组元素arr[i]，Exp为条件表达式
 */
public class AssignStmt extends Stmt {
    private LVal lval;
    private Exp exp;

    public AssignStmt() {
        super();
        lval = null;
        exp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.LVal) {
            lval = (LVal) child;
        } else if (child.getNodeType() == SynType.Exp) {
            exp = (Exp) child;
        }

        super.appendChild(child);
    }
    
    // Getter methods
    public LVal getLVal() {
        return lval;
    }
    
    public Exp getExp() {
        return exp;
    }
    
    public boolean hasLVal() {
        return lval != null;
    }
    
    public boolean hasExp() {
        return exp != null;
    }

}
