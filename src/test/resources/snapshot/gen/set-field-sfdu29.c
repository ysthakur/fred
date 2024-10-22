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
enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Color color;
  enum Foo_kind kind;
  int field;
  union {
    struct {  };
  };
};
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
int fn$bar(struct Foo* f);
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    free(this);
  } else {
    $markGray_Foo(this);
    $scan_Foo(this);
    freeList = NULL;
    $collectWhite_Foo(this);
    collectFreeList();
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Foo_tag:
    break;
  }
}
void $scan_Foo(struct Foo* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Foo(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Foo_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
  }
}
int fn$bar(struct Foo* f) {
  f->rc ++;
  f->field = 5;
  f->field = 6;
  f->field;
  int ret$0 = f->field;
  $decr_Foo(f);
  return ret$0;
}
