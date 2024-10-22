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
char* fn$foo(int bar);
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
  char* ret$1 = ifres$0;
  return ret$1;
}
