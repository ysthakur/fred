#include "runtime.h"

enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  int field;
  union {
    struct {  };
  };
};
void $free_Foo(struct Foo* this);
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
void $print_Foo(struct Foo* this);
int fn$bar(struct Foo* f);
void $free_Foo(struct Foo* this) {
  fprintf(stderr, "Freeing Foo\n");
  switch (this->kind) {
  case Foo_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  fprintf(stderr, "Decrementing Foo (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
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
    fprintf(stderr, "Removing Foo\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Foo;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Foo_tag:
    printf("Foo {");
    printf("field=");
    printf("%d", this->field);
    printf(", ");
    printf("}");
    break;
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
