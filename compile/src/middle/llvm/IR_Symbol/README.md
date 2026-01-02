## 符号表中各类型符号的信息记录

符号表 <mcfile name="IrSymbolTable.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\IrSym\IrSymbolTable.java"></mcfile> 使用 `HashMap<String, Value>` 存储符号，其中 `String` 是符号名，`Value` 是对应的 LLVM IR 值对象。

### 1. **变量符号**

#### 1.1 局部变量
- **存储对象**: <mcsymbol name="Alloca" filename="Alloca.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\instr\Alloca.java" startline="9" type="class"></mcsymbol> 指令
- **记录信息**:
    - `name`: 变量名（如 `%v1`, `%v2`）
    - `type`: `PointerIrTy`（指向变量实际类型的指针）
    - `init`: 初始值（`Constant` 类型，可能为 null）
- **用途**: 通过 `alloca` 在栈上分配内存，后续通过 `load`/`store` 访问

#### 1.2 全局变量
- **存储对象**: <mcsymbol name="GlobalVar" filename="GlobalVar.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\notInstr\GlobalVar.java" startline="17" type="class"></mcsymbol>
- **记录信息**:
    - `name`: 全局变量名（如 `@g_a`, `@g_arr`）
    - `type`: `PointerIrTy`（指向变量实际类型的指针）
    - `init`: 初始值（`Constant` 类型）
    - `isConst`: 是否为常量
- **用途**: 存储在全局数据段，可直接访问

### 2. **常量符号**
- **存储对象**: <mcsymbol name="ConstData" filename="ConstData.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\constant\ConstData.java" startline="8" type="class"></mcsymbol>
- **记录信息**:
    - `name`: 常量的字符串表示（如 `"42"`, `"'a'"`）
    - `type`: `DataIrTy`（`I32` 或 `I8`）
    - `value`: 常量的整数值
    - `justPlaceholder`: 是否仅为占位符
- **用途**: 编译时常量，可直接使用值

### 3. **函数符号**
- **存储对象**: <mcsymbol name="Function" filename="Function.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\notInstr\Function.java" startline="16" type="class"></mcsymbol>
- **记录信息**:
    - `name`: 函数名（如 `@main`, `@func`）
    - `type`: `FuncIrTy`（包含返回类型和参数类型列表）
    - `bbs`: 基本块列表（`LinkedList<BasicBlock>`）
    - `fParams`: 形参列表（`ArrayList<FParam>`）
    - `value2reg`: 值到寄存器的映射（用于后端代码生成）
- **用途**: 函数定义和调用

### 4. **形参符号**
- **存储对象**: <mcsymbol name="Alloca" filename="Alloca.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\instr\Alloca.java" startline="9" type="class"></mcsymbol> 指令（注意：形参本身是 <mcsymbol name="FParam" filename="FParam.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\notInstr\FParam.java" startline="7" type="class"></mcsymbol>，但符号表中存储的是其对应的 `alloca`）
- **记录信息**:
    - `name`: 形参名
    - `type`: `PointerIrTy`（指向形参类型的指针）
    - 对应的 `FParam` 对象存储在 `Function` 的 `fParams` 中
- **处理过程**:
    1. 创建 `alloca` 指令为形参分配栈空间
    2. 将 `FParam` 的值 `store` 到 `alloca` 中
    3. 将 `alloca` 加入符号表供函数体使用

### 5. **数组符号**

#### 5.1 局部数组
- **存储对象**: <mcsymbol name="Alloca" filename="Alloca.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\instr\Alloca.java" startline="9" type="class"></mcsymbol> 指令
- **记录信息**:
    - `name`: 数组名
    - `type`: `PointerIrTy`（指向 `ArrayIrTy` 的指针）
    - `init`: 数组初始值（`ConstArray` 类型，可能为 null）

#### 5.2 全局数组
- **存储对象**: <mcsymbol name="GlobalVar" filename="GlobalVar.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\value\notInstr\GlobalVar.java" startline="17" type="class"></mcsymbol>
- **记录信息**:
    - `name`: 全局数组名
    - `type`: `PointerIrTy`（指向 `ArrayIrTy` 的指针）
    - `init`: 数组初始值（`ConstArray` 类型）
    - `isConst`: 是否为常量数组

### 6. **符号表的层次结构**
- **作用域管理**: 通过 `father` 字段实现作用域链
- **符号查找**: <mcsymbol name="find" filename="IrSymbolTable.java" path="d:\CODE\compile\BUAA-2024-compiler\src\llvm\IrSym\IrSymbolTable.java" startline="23" type="function"></mcsymbol> 方法沿着 `father` 链向上查找
- **作用域操作**:
    - `enter()`: 创建新的子作用域
    - `leave()`: 返回父作用域

### 总结

符号表中的每个符号都映射到一个 `Value` 对象，这些对象包含了：
1. **类型信息** (`IrTy`): 描述数据的 LLVM IR 类型
2. **名称信息** (`String`): 在 IR 中的标识符
3. **值信息**: 根据符号类型不同而不同（常量值、内存地址、函数定义等）
4. **使用关系** (`ArrayList<Use>`): 记录该值被哪些指令使用

这种设计使得编译器能够在 IR 生成过程中正确地进行名称解析、类型检查和代码生成。
        