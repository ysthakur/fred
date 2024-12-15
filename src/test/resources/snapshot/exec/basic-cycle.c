#include "runtime.h"

enum Option_kind { None_tag, Some_tag };
struct Option {
  int rc;
  enum Color color;
  int addedPCR;
  enum Option_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { struct List* value_Some; };
  };
};
enum List_kind { List_tag };
struct List {
  int rc;
  enum Color color;
  int addedPCR;
  enum List_kind kind;
  void (*print)();
  int value;
  struct Option* next;
  union {
    struct {  };
  };
};
void $free_Option(struct Option* this);
void $free_List(struct List* this);
void $decr_Option(struct Option* this);
void $decr_List(struct List* this);
void $markGray_Option(struct Option* this);
void $markGray_List(struct List* this);
void $scan_Option(struct Option* this);
void $scan_List(struct List* this);
void $scanBlack_Option(struct Option* this);
void $scanBlack_List(struct List* this);
void $collectWhite_Option(struct Option* this);
void $collectWhite_List(struct List* this);
void $print_Option(struct Option* this);
void $print_List(struct List* this);
struct Option* new$None();
struct Option* new$Some(struct List* value);
struct List* new$List(struct Option* next, int value);
int main();
void $free_Option(struct Option* this) {
  switch (this->kind) {
  case None_tag:
    break;
  case Some_tag:
    break;
  }
  free(this);
}
void $free_List(struct List* this) {
  switch (this->kind) {
  case List_tag:
    break;
  }
  free(this);
}
void $decr_Option(struct Option* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      $decr_List(this->value_Some);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_Option,
      (void *) $scan_Option,
      (void *) $collectWhite_Option);
  }
}
void $decr_List(struct List* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case List_tag:
      $decr_Option(this->next);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_List,
      (void *) $scan_List,
      (void *) $collectWhite_List);
  }
}
void $markGray_Option(struct Option* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case None_tag:
    break;
  case Some_tag:
    this->value_Some->rc --;
    $markGray_List(this->value_Some);
    break;
  }
}
void $markGray_List(struct List* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case List_tag:
    this->next->rc --;
    $markGray_Option(this->next);
    break;
  }
}
void $scan_Option(struct Option* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Option(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case None_tag:
    break;
  case Some_tag:
    $scan_List(this->value_Some);
    break;
  }
}
void $scan_List(struct List* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_List(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case List_tag:
    $scan_Option(this->next);
    break;
  }
}
void $scanBlack_Option(struct Option* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      this->value_Some->rc ++;
      $scanBlack_List(this->value_Some);
      break;
    }
  }
}
void $scanBlack_List(struct List* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case List_tag:
      this->next->rc ++;
      $scanBlack_Option(this->next);
      break;
    }
  }
}
void $collectWhite_Option(struct Option* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      $collectWhite_List(this->value_Some);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Option;
  }
}
void $collectWhite_List(struct List* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case List_tag:
      $collectWhite_Option(this->next);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_List;
  }
}
void $print_Option(struct Option* this) {
  switch (this->kind) {
  case None_tag:
    printf("None {");
    printf("}");
    break;
  case Some_tag:
    printf("Some {");
    printf("value=");
    $print_List(this->value_Some);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_List(struct List* this) {
  switch (this->kind) {
  case List_tag:
    printf("List {");
    printf("value=");
    printf("%d", this->value);
    printf(", ");
    printf("next=");
    $print_Option(this->next);
    printf(", ");
    printf("}");
    break;
  }
}
struct Option* new$None() {
  struct Option* $res = malloc(sizeof (struct Option));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Option;
  $res->kind = None_tag;
  return $res;
}
struct Option* new$Some(struct List* value) {
  struct Option* $res = malloc(sizeof (struct Option));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Option;
  $res->kind = Some_tag;
  $res->value_Some = value;
  $res->value_Some->rc ++;
  return $res;
}
struct List* new$List(struct Option* next, int value) {
  struct List* $res = malloc(sizeof (struct List));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_List;
  $res->kind = List_tag;
  $res->value = value;
  $res->next = next;
  $res->next->rc ++;
  return $res;
}
int main() {
  pcrBuckets = calloc(sizeof(void *), 1);
  numSccs = 1;
  struct List* a = new$List(new$None(), 1);
  a->rc ++;
  struct List* b = new$List(new$Some(a), 2);
  b->rc ++;
  struct Option* oldValue$0 = a->next;
  a->next = new$Some(b);
  a->next->rc ++;
  $decr_Option(oldValue$0);
  printf("%d\n", a->value + b->value);
  int ret$1 = 0;
  $decr_List(b);
  $decr_List(a);
  processAllPCRs();
  free(pcrBuckets);
  return ret$1;
}
