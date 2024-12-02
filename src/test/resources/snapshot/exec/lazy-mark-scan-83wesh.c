#include "runtime.h"

enum FooList_kind { FooCons_tag, FooNil_tag };
struct FooList {
  int rc;
  enum Color color;
  int addedPCR;
  enum FooList_kind kind;
  void (*print)();
  union {
    struct { struct Foo* foo_FooCons; struct FooList* tail_FooCons; };
    struct {  };
  };
};
enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  struct Bar* bar;
  union {
    struct {  };
  };
};
enum Bar_kind { Bar_tag };
struct Bar {
  int rc;
  enum Color color;
  int addedPCR;
  enum Bar_kind kind;
  void (*print)();
  union {
    struct {  };
  };
};
void $free_FooList(struct FooList* this);
void $free_Foo(struct Foo* this);
void $free_Bar(struct Bar* this);
void $decr_FooList(struct FooList* this);
void $decr_Foo(struct Foo* this);
void $decr_Bar(struct Bar* this);
void $markGray_FooList(struct FooList* this);
void $markGray_Foo(struct Foo* this);
void $markGray_Bar(struct Bar* this);
void $scan_FooList(struct FooList* this);
void $scan_Foo(struct Foo* this);
void $scan_Bar(struct Bar* this);
void $scanBlack_FooList(struct FooList* this);
void $scanBlack_Foo(struct Foo* this);
void $scanBlack_Bar(struct Bar* this);
void $collectWhite_FooList(struct FooList* this);
void $collectWhite_Foo(struct Foo* this);
void $collectWhite_Bar(struct Bar* this);
void $print_FooList(struct FooList* this);
void $print_Foo(struct Foo* this);
void $print_Bar(struct Bar* this);
struct FooList* new$FooCons(struct Foo* foo, struct FooList* tail);
struct FooList* new$FooNil();
struct Foo* new$Foo(struct Bar* bar);
struct Bar* new$Bar();
int main();
void $free_FooList(struct FooList* this) {
  fprintf(stderr, "Freeing FooList\n");
  switch (this->kind) {
  case FooCons_tag:
    break;
  case FooNil_tag:
    break;
  }
  free(this);
}
void $free_Foo(struct Foo* this) {
  fprintf(stderr, "Freeing Foo\n");
  switch (this->kind) {
  case Foo_tag:
    break;
  }
  free(this);
}
void $free_Bar(struct Bar* this) {
  fprintf(stderr, "Freeing Bar\n");
  switch (this->kind) {
  case Bar_tag:
    break;
  }
  free(this);
}
void $decr_FooList(struct FooList* this) {
  fprintf(stderr, "Decrementing FooList (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case FooCons_tag:
      $decr_Foo(this->foo_FooCons);
      $decr_FooList(this->tail_FooCons);
      break;
    case FooNil_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $decr_Foo(struct Foo* this) {
  fprintf(stderr, "Decrementing Foo (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      $decr_Bar(this->bar);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $decr_Bar(struct Bar* this) {
  fprintf(stderr, "Decrementing Bar (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Bar_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_FooList(struct FooList* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case FooCons_tag:
    this->foo_FooCons->rc --;
    $markGray_Foo(this->foo_FooCons);
    this->tail_FooCons->rc --;
    $markGray_FooList(this->tail_FooCons);
    break;
  case FooNil_tag:
    break;
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Foo_tag:
    this->bar->rc --;
    $markGray_Bar(this->bar);
    break;
  }
}
void $markGray_Bar(struct Bar* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Bar_tag:
    break;
  }
}
void $scan_FooList(struct FooList* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_FooList(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case FooCons_tag:
    $scan_Foo(this->foo_FooCons);
    $scan_FooList(this->tail_FooCons);
    break;
  case FooNil_tag:
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
    $scan_Bar(this->bar);
    break;
  }
}
void $scan_Bar(struct Bar* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Bar(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Bar_tag:
    break;
  }
}
void $scanBlack_FooList(struct FooList* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case FooCons_tag:
      this->foo_FooCons->rc ++;
      $scanBlack_Foo(this->foo_FooCons);
      this->tail_FooCons->rc ++;
      $scanBlack_FooList(this->tail_FooCons);
      break;
    case FooNil_tag:
      break;
    }
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      this->bar->rc ++;
      $scanBlack_Bar(this->bar);
      break;
    }
  }
}
void $scanBlack_Bar(struct Bar* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      break;
    }
  }
}
void $collectWhite_FooList(struct FooList* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case FooCons_tag:
      $collectWhite_Foo(this->foo_FooCons);
      $collectWhite_FooList(this->tail_FooCons);
      break;
    case FooNil_tag:
      break;
    }
    fprintf(stderr, "Removing FooList\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_FooList;
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      $collectWhite_Bar(this->bar);
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
void $collectWhite_Bar(struct Bar* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      break;
    }
    fprintf(stderr, "Removing Bar\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Bar;
  }
}
void $print_FooList(struct FooList* this) {
  switch (this->kind) {
  case FooCons_tag:
    printf("FooCons {");
    printf("foo=");
    $print_Foo(this->foo_FooCons);
    printf(", ");
    printf("tail=");
    $print_FooList(this->tail_FooCons);
    printf(", ");
    printf("}");
    break;
  case FooNil_tag:
    printf("FooNil {");
    printf("}");
    break;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Foo_tag:
    printf("Foo {");
    printf("bar=");
    $print_Bar(this->bar);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_Bar(struct Bar* this) {
  switch (this->kind) {
  case Bar_tag:
    printf("Bar {");
    printf("}");
    break;
  }
}
struct FooList* new$FooCons(struct Foo* foo, struct FooList* tail) {
  struct FooList* $res = malloc(sizeof (struct FooList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_FooList;
  $res->kind = FooCons_tag;
  $res->foo_FooCons = foo;
  $res->foo_FooCons->rc ++;
  $res->tail_FooCons = tail;
  $res->tail_FooCons->rc ++;
  return $res;
}
struct FooList* new$FooNil() {
  struct FooList* $res = malloc(sizeof (struct FooList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_FooList;
  $res->kind = FooNil_tag;
  return $res;
}
struct Foo* new$Foo(struct Bar* bar) {
  struct Foo* $res = malloc(sizeof (struct Foo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Foo;
  $res->kind = Foo_tag;
  $res->bar = bar;
  $res->bar->rc ++;
  return $res;
}
struct Bar* new$Bar() {
  struct Bar* $res = malloc(sizeof (struct Bar));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Bar;
  $res->kind = Bar_tag;
  return $res;
}
int main() {
  struct Bar* bar = new$Bar();
  bar->rc ++;
  struct Foo* foo = new$Foo(bar);
  foo->rc ++;
  struct FooList* foos = new$FooCons(foo, new$FooNil());
  foos->rc ++;
  int ret$0 = 0;
  $decr_FooList(foos);
  $decr_Foo(foo);
  $decr_Bar(bar);
  processAllPCRs();
  return ret$0;
}
