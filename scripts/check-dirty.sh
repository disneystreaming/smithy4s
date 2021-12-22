diff=$(git diff --shortstat 2> /dev/null | tail -n1)
if [ -n "$diff" ] ;
then (echo "dirty"; exit 1)
fi
