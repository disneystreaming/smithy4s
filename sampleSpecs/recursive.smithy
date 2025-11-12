namespace smithy4s.example

structure IntList {
    @required
    head: Integer

    tail: IntList
}

list RecursiveList {
    member: RecursiveListWrapper
}

structure RecursiveListWrapper {
    @required
    items: RecursiveList
}

union Tree {
  tree: TreeNode
  leaf: LeafNode
}

structure TreeNode {
  @required
  left: Tree
  @required
  right: Tree
}

structure LeafNode {
  @required
  value: Integer
}

union ConsList {
  cons: Cons
  nil: Nil
}

structure Cons {
  @required
  head: Integer
  @required
  tail: ConsList
} 

structure Nil {}
