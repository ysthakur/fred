
char* fn$foo(int bar) {
  int g = 3;
  char* ifres$0;
  if (g) {
    int y = 5;
    ifres$0 = "foo";
  } else {
    char* y = "foo";
    ifres$0 = y;
  }
  char* fnres$1 = ifres$0;
  return fnres$1;
}