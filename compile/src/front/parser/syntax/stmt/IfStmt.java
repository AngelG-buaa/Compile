package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Cond;
import front.parser.syntax.TokenNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 对应Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
 */
public class IfStmt extends Stmt {
    private Cond condition;
    private boolean alreadyHaveCondStmt;
    private ArrayList<Stmt> stmts;

    public IfStmt() {
        super();
        condition = null;
        alreadyHaveCondStmt = false;
        stmts = new ArrayList<>();
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Cond) {
            condition = (Cond) child;
        } else if (child instanceof Stmt) {
            if (alreadyHaveCondStmt) {
                stmts.add((Stmt) child);
            } else {
                alreadyHaveCondStmt = true;
                stmts.add((Stmt) child);
            }
        }

        super.appendChild(child);
    }
    
    // Getter methods
    public Cond getCondition() {
        return condition;
    }
    
    public ArrayList<Stmt> getStmts() {
        return stmts;
    }
    
    public Stmt getIfStmt() {
        return !stmts.isEmpty() ? stmts.get(0) : null;
    }
    
    public Stmt getElseStmt() {
        return stmts.size() > 1 ? stmts.get(1) : null;
    }
    
    public boolean hasCondition() {
        return condition != null;
    }
    
    public boolean hasElse() {
        return stmts.size() > 1;
    }
    
    public int getLineNumber() {
        // 从第一个子节点获取行号
        if (!getChildren().isEmpty() && getChildren().get(0) instanceof TokenNode) {
            return ((TokenNode) getChildren().get(0)).getLineNumber();
        }
        return 0;
    }

}
