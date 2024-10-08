#include <stdlib.h>
int fn$fact(int i) {
  int ifres$0;
  if (i == 0) {
    ifres$0 = 1;
  } else {
    ifres$0 = i * fn$fact(i - 1);
  }
  int ret$1 = ifres$0;
  return ret$1;
}
