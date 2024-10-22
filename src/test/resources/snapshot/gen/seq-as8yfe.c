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
int fn$foo(int bar);
int fn$foo(int bar) {
  bar;
  int ret$0 = 3;
  return ret$0;
}
