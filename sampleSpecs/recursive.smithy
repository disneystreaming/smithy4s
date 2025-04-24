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
