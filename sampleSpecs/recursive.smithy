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
  left: Tree
  right: Tree
}

structure LeafNode {
  @required
  value: Integer
}
