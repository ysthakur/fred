#include "runtime.h"

enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  struct OptFoo* self;
  union {
    struct {  };
  };
};
enum OptFoo_kind { SomeFoo_tag, NoneFoo_tag };
struct OptFoo {
  int rc;
  enum Color color;
  int addedPCR;
  enum OptFoo_kind kind;
  void (*print)();
  union {
    struct { struct Foo* value_SomeFoo; };
    struct {  };
  };
};
void $free_Foo(struct Foo* this);
void $free_OptFoo(struct OptFoo* this);
void $decr_Foo(struct Foo* this);
void $decr_OptFoo(struct OptFoo* this);
void $markGray_Foo(struct Foo* this);
void $markGray_OptFoo(struct OptFoo* this);
void $scan_Foo(struct Foo* this);
void $scan_OptFoo(struct OptFoo* this);
void $scanBlack_Foo(struct Foo* this);
void $scanBlack_OptFoo(struct OptFoo* this);
void $collectWhite_Foo(struct Foo* this);
void $collectWhite_OptFoo(struct OptFoo* this);
void $print_Foo(struct Foo* this);
void $print_OptFoo(struct OptFoo* this);
struct Foo* new$Foo(struct OptFoo* self);
struct OptFoo* new$SomeFoo(struct Foo* value);
struct OptFoo* new$NoneFoo();
int main();
void $free_Foo(struct Foo* this) {
  switch (this->kind) {
  case Foo_tag:
    break;
  }
  free(this);
}
void $free_OptFoo(struct OptFoo* this) {
  switch (this->kind) {
  case SomeFoo_tag:
    break;
  case NoneFoo_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      $decr_OptFoo(this->self);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_Foo,
      (void *) $scan_Foo,
      (void *) $collectWhite_Foo);
  }
}
void $decr_OptFoo(struct OptFoo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case SomeFoo_tag:
      $decr_Foo(this->value_SomeFoo);
      break;
    case NoneFoo_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_OptFoo,
      (void *) $scan_OptFoo,
      (void *) $collectWhite_OptFoo);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Foo_tag:
    this->self->rc --;
    $markGray_OptFoo(this->self);
    break;
  }
}
void $markGray_OptFoo(struct OptFoo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case SomeFoo_tag:
    this->value_SomeFoo->rc --;
    $markGray_Foo(this->value_SomeFoo);
    break;
  case NoneFoo_tag:
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
    $scan_OptFoo(this->self);
    break;
  }
}
void $scan_OptFoo(struct OptFoo* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_OptFoo(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case SomeFoo_tag:
    $scan_Foo(this->value_SomeFoo);
    break;
  case NoneFoo_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      this->self->rc ++;
      $scanBlack_OptFoo(this->self);
      break;
    }
  }
}
void $scanBlack_OptFoo(struct OptFoo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case SomeFoo_tag:
      this->value_SomeFoo->rc ++;
      $scanBlack_Foo(this->value_SomeFoo);
      break;
    case NoneFoo_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      $collectWhite_OptFoo(this->self);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Foo;
  }
}
void $collectWhite_OptFoo(struct OptFoo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case SomeFoo_tag:
      $collectWhite_Foo(this->value_SomeFoo);
      break;
    case NoneFoo_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_OptFoo;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Foo_tag:
    printf("Foo {");
    printf("self=");
    $print_OptFoo(this->self);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_OptFoo(struct OptFoo* this) {
  switch (this->kind) {
  case SomeFoo_tag:
    printf("SomeFoo {");
    printf("value=");
    $print_Foo(this->value_SomeFoo);
    printf(", ");
    printf("}");
    break;
  case NoneFoo_tag:
    printf("NoneFoo {");
    printf("}");
    break;
  }
}
struct Foo* new$Foo(struct OptFoo* self) {
  struct Foo* $res = malloc(sizeof (struct Foo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Foo;
  $res->kind = Foo_tag;
  $res->self = self;
  $res->self->rc ++;
  return $res;
}
struct OptFoo* new$SomeFoo(struct Foo* value) {
  struct OptFoo* $res = malloc(sizeof (struct OptFoo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_OptFoo;
  $res->kind = SomeFoo_tag;
  $res->value_SomeFoo = value;
  $res->value_SomeFoo->rc ++;
  return $res;
}
struct OptFoo* new$NoneFoo() {
  struct OptFoo* $res = malloc(sizeof (struct OptFoo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_OptFoo;
  $res->kind = NoneFoo_tag;
  return $res;
}
int main() {
  struct Foo* v0 = new$Foo(new$NoneFoo());
  v0->rc ++;
  struct Foo* v1 = new$Foo(new$NoneFoo());
  v1->rc ++;
  struct OptFoo* oldValue$0 = v0->self;
  v0->self = new$SomeFoo(v1);
  v0->self->rc ++;
  $decr_OptFoo(oldValue$0);
  struct OptFoo* oldValue$1 = v0->self;
  v0->self = new$SomeFoo(v0);
  v0->self->rc ++;
  $decr_OptFoo(oldValue$1);
  drop((void *) v0->self, (void *) $decr_OptFoo);
  processAllPCRs();
  0;
  struct OptFoo* oldValue$2 = v0->self;
  v0->self = new$SomeFoo(v1);
  v0->self->rc ++;
  $decr_OptFoo(oldValue$2);
  drop((void *) v0->self, (void *) $decr_OptFoo);
  int ret$3 = 0;
  $decr_Foo(v1);
  $decr_Foo(v0);
  processAllPCRs();
  return ret$3;
}
