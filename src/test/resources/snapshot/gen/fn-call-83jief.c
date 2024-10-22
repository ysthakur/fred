#include <stdlib.h>
#include <stdio.h>

enum Color { kBlack, kGray, kWhite };

struct FreeCell {
  int rc;
  enum Color color;
  struct FreeCell *next;
};

struct FreeCell *freeList = NULL;

void collectFreeList() {
  while (freeList != NULL) {
    struct FreeCell *next = freeList->next;
    free(freeList);
    freeList = next;
  }
}
int fn$fact(int i);
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
