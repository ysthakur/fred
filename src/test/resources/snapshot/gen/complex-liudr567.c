#include "runtime.h"

enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  union {
    struct { int x_Bar; int y_Bar; };
    struct { char* a_Baz; int b_Baz; };
  };
};
void $free_Foo(struct Foo* this);
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
void $print_Foo(struct Foo* this);
struct Foo* new$Bar(int x, int y);
struct Foo* new$Baz(char* a, int b);
int fn$foo(struct Foo* foo);
int main();
void $free_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    break;
  case Baz_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Bar_tag:
      break;
    case Baz_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Bar_tag:
    break;
  case Baz_tag:
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
  case Bar_tag:
    break;
  case Baz_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      break;
    case Baz_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      break;
    case Baz_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Foo;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    printf("Bar {");
    printf("x=");
    printf("%d", this->x_Bar);
    printf(", ");
    printf("y=");
    printf("%d", this->y_Bar);
    printf(", ");
    printf("}");
    break;
  case Baz_tag:
    printf("Baz {");
    printf("a=");
    printf("%s", this->a_Baz);
    printf(", ");
    printf("b=");
    printf("%d", this->b_Baz);
    printf(", ");
    printf("}");
    break;
  }
}
struct Foo* new$Bar(int x, int y) {
  struct Foo* $res = malloc(sizeof (struct Foo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Foo;
  $res->kind = Bar_tag;
  $res->x_Bar = x;
  $res->y_Bar = y;
  return $res;
}
struct Foo* new$Baz(char* a, int b) {
  struct Foo* $res = malloc(sizeof (struct Foo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Foo;
  $res->kind = Baz_tag;
  $res->a_Baz = a;
  $res->b_Baz = b;
  return $res;
}
int fn$foo(struct Foo* foo) {
  foo->rc ++;
  struct Foo* matchobj$0 = foo;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Bar_tag:
    int x = matchobj$0->x_Bar;
    int yyy = matchobj$0->y_Bar;
    matchres$1 = x + yyy;
    break;
  case Baz_tag:
    char* astr = matchobj$0->a_Baz;
    int b = matchobj$0->b_Baz;
    matchres$1 = b;
    break;
  }
  int ret$2 = matchres$1;
  $decr_Foo(foo);
  return ret$2;
}
int main() {
  pcrBuckets = calloc(sizeof(void *), 1);
  numSccs = 1;
  struct Foo* foo = new$Bar(1, 2);
  foo->rc ++;
  int ret$3 = fn$foo(foo);
  $decr_Foo(foo);
  processAllPCRs();
  free(pcrBuckets);
  return ret$3;
}
