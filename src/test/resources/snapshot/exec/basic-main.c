#include "runtime.h"

enum List_kind { Nil_tag, Cons_tag };
struct List {
  int rc;
  enum Color color;
  int addedPCR;
  enum List_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { int value_Cons; struct List* next_Cons; };
  };
};
void $free_List(struct List* this);
void $decr_List(struct List* this);
void $markGray_List(struct List* this);
void $scan_List(struct List* this);
void $scanBlack_List(struct List* this);
void $collectWhite_List(struct List* this);
void $print_List(struct List* this);
struct List* new$Nil();
struct List* new$Cons(struct List* next, int value);
int fn$sum(struct List* list);
int main();
void $free_List(struct List* this) {
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    break;
  }
  free(this);
}
void $decr_List(struct List* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      $decr_List(this->next_Cons);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_List(struct List* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    this->next_Cons->rc --;
    $markGray_List(this->next_Cons);
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
  case Nil_tag:
    break;
  case Cons_tag:
    $scan_List(this->next_Cons);
    break;
  }
}
void $scanBlack_List(struct List* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      this->next_Cons->rc ++;
      $scanBlack_List(this->next_Cons);
      break;
    }
  }
}
void $collectWhite_List(struct List* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      $collectWhite_List(this->next_Cons);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_List;
  }
}
void $print_List(struct List* this) {
  switch (this->kind) {
  case Nil_tag:
    printf("Nil {");
    printf("}");
    break;
  case Cons_tag:
    printf("Cons {");
    printf("value=");
    printf("%d", this->value_Cons);
    printf(", ");
    printf("next=");
    $print_List(this->next_Cons);
    printf(", ");
    printf("}");
    break;
  }
}
struct List* new$Nil() {
  struct List* $res = malloc(sizeof (struct List));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_List;
  $res->kind = Nil_tag;
  return $res;
}
struct List* new$Cons(struct List* next, int value) {
  struct List* $res = malloc(sizeof (struct List));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_List;
  $res->kind = Cons_tag;
  $res->value_Cons = value;
  $res->next_Cons = next;
  $res->next_Cons->rc ++;
  return $res;
}
int fn$sum(struct List* list) {
  list->rc ++;
  struct List* matchobj$0 = list;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Nil_tag:
    matchres$1 = 0;
    break;
  case Cons_tag:
    int value = matchobj$0->value_Cons;
    struct List* tail = matchobj$0->next_Cons;
    tail->rc ++;
    matchres$1 = value + fn$sum(tail);
    $decr_List(tail);
    break;
  }
  int ret$2 = matchres$1;
  $decr_List(list);
  return ret$2;
}
int main() {
  struct List* list = new$Cons(new$Cons(new$Cons(new$Nil(), 4), 2), 1);
  list->rc ++;
  printf("%d\n", fn$sum(list));
  int ret$3 = 0;
  $decr_List(list);
  processAllPCRs();
  return ret$3;
}
